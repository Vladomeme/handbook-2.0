package net.handbook.main;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.xpple.clientarguments.arguments.CEntityArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.*;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.feature.TradeScreen;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.category.*;
import net.handbook.main.resources.entry.*;
import net.handbook.main.resources.waypoint.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HandbookClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("handbook");
    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static KeyBinding openScreen;
    public static KeyBinding addLocation;

    public static HandbookScreen handbookScreen;
    public static TradeScreen tradeScreen;
    public static final List<CategoryWriter<? extends Entry>> writers = new ArrayList<>();

    static boolean firstLoad = true;

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {

            @Override
            public Identifier getFabricId() {
                return new Identifier("handbook", "resources");
            }

            @Override
            public void reload(ResourceManager manager) {
                onReload(manager);
            }
        });

        registerKeyBinds();
        registerEvents();
        registerCommands();

        LOGGER.info("Handbook 2.0 loaded!");
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private void onReload(ResourceManager manager) {
        if (firstLoad) {
            if (!Files.exists(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/first_load"))) copyAllFiles(manager);

            handbookScreen = HandbookScreen.INSTANCE;
            tradeScreen = TradeScreen.INSTANCE;

            WaypointManager.screen = handbookScreen;
            HandbookConfig.read();

            firstLoad = false;
        }
        else save();

        File[] files = new File(FabricLoader.getInstance().getConfigDir() + "/handbook").listFiles();
        if (files == null) {
            LOGGER.error("No handbook categories found!");
            return;
        }
        tradeScreen.clear();

        for (File file : files) {
            if (!file.getName().endsWith("json") || file.getName().equals("config.json")) continue;
            try {
                String s = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                String type = JsonParser.parseString(s).getAsJsonObject().get("type").getAsString();
                switch (type) {
                    case "normal" -> {
                        CategoryWriter<Entry> writer = new CategoryWriter<>(file.toPath(), new TypeToken<>(){});
                        if (writer.category.getTitle().equals("EXCLUDE")) continue;
                        writers.add(writer);
                    }
                    case "positioned" -> {
                        CategoryWriter<PositionedEntry> writer = new CategoryWriter<>(file.toPath(), new TypeToken<>(){});
                        if (writer.category.getTitle().equals("EXCLUDE")) continue;
                        writers.add(writer);
                        if (writer.category.getTitle().equals("Locations")) LocationWriter.writer = writer;
                    }
                    case "area" -> {
                        CategoryWriter<AreaEntry> writer = new CategoryWriter<>(file.toPath(), new TypeToken<>(){});
                        if (writer.category.getTitle().equals("EXCLUDE")) continue;
                        writers.add(writer);
                    }
                    case "trader" -> {
                        CategoryWriter<TraderEntry> writer = new CategoryWriter<>(file.toPath(), new TypeToken<>(){});
                        writers.add(writer);
                        if (writer.category.getTitle().equals("EXCLUDE")) {
                            NPCWriter.blacklist = writer;
                            LOGGER.info("Loaded trader blacklist");
                            continue;
                        }
                        if (writer.category.getTitle().equals("NPC")) NPCWriter.writer = writer;
                        for (Entry entry : writer.category.getEntries()) {
                            if (entry.hasOffers()) tradeScreen.addEntries(entry.getOffers(), entry.getID());
                        }
                    }
                    case "waypoint" -> {
                        CategoryWriter<WaypointEntry> writer = new CategoryWriter<>(file.toPath(), new TypeToken<>(){});
                        if (writer.category.getTitle().equals("EXCLUDE")) continue;
                        writers.add(writer);
                    }
                    case "mark" -> {
                        LOGGER.info("Loaded marked entries data.");
                        handbookScreen.markedEntries = (new Gson()).fromJson(
                                Files.readString(file.toPath(), StandardCharsets.UTF_8), MarkCategory.class);
                        continue;
                    }
                }
                LOGGER.info("Loaded {} category {}", type, writers.get(writers.size() - 1).category.getTitle());
            }
            catch (IOException | JsonSyntaxException e) {
                LOGGER.info("Failed to read category file {}", file.toPath());
                LOGGER.info(e.getMessage());
            }
        }
        writers.sort(null);
        writers.forEach(writer -> writer.category.getEntries().sort(null));
        LOGGER.info("Loaded {} categories", writers.size());
        WaypointManager.updateBeaconColor(HandbookConfig.INSTANCE.beaconColor);
    }

    private void copyAllFiles(ResourceManager manager) {
        LOGGER.info("Looks like Handbook is loaded for the first time. Copying all files...");

        Path home = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook");
        try {
            Files.createDirectories(Path.of(home + "/textures"));
            Files.createDirectories(Path.of(home + "/trades"));
            Files.createDirectories(Path.of(home + "/waypoints"));
            Files.createFile(Path.of(home + "/first_load"));

        } catch (IOException e) {
            LOGGER.error("Failed to create handbook directories.");
            return;
        }

        manager.findResources("handbook_default", id -> true).forEach((id, resource) -> {
            Path path = Path.of(home + id.getPath().replace("handbook_default", ""));
            try {
                Files.write(path, resource.getInputStream().readAllBytes());
            } catch (IOException e) {
                LOGGER.error("Failed to copy handbook file: {}.", id.getPath());
            }
        });
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openScreen.wasPressed()) {
                if (!(client.currentScreen instanceof HandbookScreen)) openHandbookScreen();
                while (openScreen.wasPressed()) {}
            }
            if (addLocation.wasPressed()) {
                if (!(client.currentScreen instanceof LocationScreen)) openLocationScreen();
                while (addLocation.wasPressed()) {}
            }
            WaypointManager.tick();
            if (AreaSelector.isActive()) AreaSelector.emitParticles();
            if (client.currentScreen instanceof HandbookScreen) handbookScreen.filterEntries(true);
            if (client.currentScreen instanceof TradeScreen) tradeScreen.filterEntries();
            if (client.world != null && WaypointManager.shouldRestore())
                WaypointManager.sendRestoreMessage();
        });

        WorldRenderEvents.AFTER_ENTITIES.register((ctx) -> {
            if (WaypointManager.isActive() && (WaypointManager.getDistance() > 30)) WaypointManager.renderBeacon(ctx);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (WaypointManager.waypointsSaved()) WaypointManager.prepareRestoreMessage();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> save());
    }

    private void registerKeyBinds() {
        openScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open handbook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_T, "Handbook 2.0"));
        addLocation = KeyBindingHelper.registerKeyBinding(new KeyBinding("Add location", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_L, "Handbook 2.0"));
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("handbook")
                        .then(literal("dump")
                                .then(literal("advancements")
                                        .then(argument("Root", StringArgumentType.string()).executes(ctx ->
                                                AdvancementWriter.dumpAdvancements(StringArgumentType.getString(ctx, "Root"))))))
                        .then(literal("add")
                                .then(literal("location").then(argument("Name", StringArgumentType.string())
                                        .suggests(this::getSuggestions).executes(ctx ->
                                                LocationWriter.add(StringArgumentType.getString(ctx, "Name")))))
                                .then(literal("NPC").then(argument("Target", CEntityArgumentType.entity()).executes(ctx ->
                                        NPCWriter.add(CEntityArgumentType.getCEntity(ctx, "Target"), true)))))
                        .then(literal("waypoint")
                                .then(argument("x", IntegerArgumentType.integer())
                                        .then(argument("y", IntegerArgumentType.integer())
                                                .then(argument("z", IntegerArgumentType.integer()).executes(ctx ->
                                                        WaypointManager.setWaypoint(new WaypointEntry("Waypoint", null,
                                                                new Waypoint(
                                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                                        IntegerArgumentType.getInteger(ctx, "y"),
                                                                        IntegerArgumentType.getInteger(ctx, "z"), null), false, null))))))
                                .then(literal("alternate").executes(ctx -> WaypointManager.setAltPath()))
                                .then(literal("restore").executes(ctx -> WaypointManager.restoreWaypoints()))
                                .then(literal("continue").executes(ctx -> WaypointManager.continuePath()))
                                .then(literal("skip").executes(ctx ->
                                        WaypointManager.onWaypointReached(client.player, client.world)))
                                .then(literal("path").executes(ctx -> WaypointManager.addPathToChain()))
                                .then(literal("info").executes(ctx -> WaypointManager.printInfo())))
                        .then(literal("area")
                                .then(literal("select").executes(ctx -> AreaSelector.init()))
                                .then(literal("save").executes(ctx -> AreaSelector.save()))
                                .then(literal("confirm").executes(ctx -> AreaSelector.confirm(true)))
                                .then(literal("cancel").executes(ctx -> AreaSelector.confirm(false)))
                                .then(literal("finish").executes(ctx -> AreaSelector.finish()))
                                .then(literal("move")
                                        .then(argument("Point", IntegerArgumentType.integer())
                                                .executes(ctx -> AreaSelector.movePointToPlayer(IntegerArgumentType.getInteger(ctx, "Point")))
                                                .then(argument("Dimension", IntegerArgumentType.integer())
                                                        .then(argument("Distance", IntegerArgumentType.integer()).executes(ctx ->
                                                                AreaSelector.movePoint(
                                                                        IntegerArgumentType.getInteger(ctx, "Point"),
                                                                        IntegerArgumentType.getInteger(ctx, "Dimension"),
                                                                        IntegerArgumentType.getInteger(ctx, "Distance")))))))
                                .then(literal("npc_mass_delete").executes(ctx -> NPCWriter.delete(AreaSelector.getSelection())))
                                .then(literal("npc_mass_delete_and_blacklist").executes(ctx -> NPCWriter.delete(AreaSelector.getSelection(), true)))
                                .then(literal("mass_clear_blacklist").executes(ctx -> NPCWriter.deleteBlacklist(AreaSelector.getSelection()))))
                        .then(literal("clear_trades").executes(ctx -> NPCWriter.clear()))
        ));
    }

    public static void openHandbookScreen() {
        client.setScreen(handbookScreen);
    }

    public static void openTradeScreen() {
        client.setScreen(tradeScreen);
    }

    public static void openLocationScreen() {
        client.setScreen(new LocationScreen(Text.of("")));
    }

    public static List<Category<? extends Entry>> getCategories() {
        List<Category<? extends Entry>> list = new ArrayList<>();
        writers.forEach(writer -> {
            if (!writer.category.getTitle().equals("EXCLUDE")) list.add(writer.category);
        });
        return list;
    }

    private static void save() {
        NPCWriter.saveTrades();
        handbookScreen.markedEntries.write();
        writers.forEach(CategoryWriter::write);
        writers.clear();
    }

    @SuppressWarnings("unused")
    private CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        for (CategoryWriter<? extends Entry> writer : writers) {
            if (!writer.category.getTitle().equals("Locations")) continue;

            for (Entry entry : writer.category.getEntries()) {
                if (entry.getClearTitle().toLowerCase().contains(builder.getInput().toLowerCase()
                        .replace("/handbook add location ", ""))) builder.suggest(entry.getClearTitle());
            }
        }
        return builder.buildFuture();
    }
}
