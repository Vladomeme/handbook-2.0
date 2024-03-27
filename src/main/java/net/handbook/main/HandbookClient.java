package net.handbook.main;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.xpple.clientarguments.arguments.CEntityArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.editor.*;
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.feature.TradeScreen;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.category.*;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.TraderEntry;
import net.handbook.main.resources.entry.WaypointChain;
import net.handbook.main.resources.entry.WaypointEntry;
import net.handbook.main.resources.waypoint.Waypoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOfferList;
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

    public static final HandbookScreen handbookScreen = HandbookScreen.INSTANCE;
    public static final TradeScreen tradeScreen = TradeScreen.INSTANCE;
    public static final LocationWriter locationWriter = LocationWriter.INSTANCE;
    public static final NPCWriter npcWriter = NPCWriter.INSTANCE;

    static boolean firstLoad = false;

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {

            @Override
            public Identifier getFabricId() {
                return new Identifier("handbook", "resources");
            }

            @Override
            public void reload(ResourceManager manager) {
                onReload();
            }
        });

        registerEvents();
        registerKeyBinds();
        registerCommands();

        LOGGER.info("Handbook 2.0 loaded!");
    }

    private void onReload() {
        Gson gson = new Gson();

        handbookScreen.categories.clear();
        if (firstLoad) dumpAll();
        firstLoad = true;

        try {
            if (!Files.exists(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades")))
                Files.createDirectories(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades"));
            if (!Files.exists(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/textures")))
                Files.createDirectories(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/textures"));
            if (!Files.exists(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/waypoints")))
                Files.createDirectories(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/waypoints"));
        } catch (IOException e) {
            LOGGER.error("Failed to create handbook directories.");
            return;
        }

        File[] files = new File(FabricLoader.getInstance().getConfigDir() + "/handbook").listFiles();
        if (files == null) {
            LOGGER.error("No handbook categories found!");
            return;
        }

        for (File file : files) {
            if (!file.getName().endsWith("json") || file.getName().equals("config.json")) continue;
            try {
                String type = gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), CategoryType.class).getType();
                switch (type) {
                    case "positioned" -> {
                        PositionedCategory category = gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), PositionedCategory.class);
                        LOGGER.info("Loading positioned category " + category.getTitle());
                        handbookScreen.categories.add(category);
                    }
                    case "area" -> {
                        AreaCategory category = gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), AreaCategory.class);
                        LOGGER.info("Loading area category " + category.getTitle());
                        handbookScreen.categories.add(category);
                    }
                    case "waypoint" -> {
                        WaypointCategory category = gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), WaypointCategory.class);
                        LOGGER.info("Loading waypoint category " + category.getTitle());
                        handbookScreen.categories.add(mergeWaypointEntries(category));
                    }
                    case "trader" -> {
                        TraderCategory category = gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), TraderCategory.class);
                        LOGGER.info("Loading trader category " + category.getTitle());
                        if (category.getTitle().equals("EXCLUDE")) {
                            npcWriter.setBlacklist(category);
                            continue;
                        }
                        else handbookScreen.categories.add(category);

                        tradeScreen.clear();
                        for (TraderEntry entry : category.getEntries()) {
                            TradeOfferList offers = entry.getOffers();
                            if (offers != null) tradeScreen.addEntries(offers, entry.getID());
                        }
                    }
                    case "mark" -> LOGGER.info("Loading marked entries data.");
                    default -> {
                        Category category = gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), Category.class);
                        LOGGER.info("Loading normal category " + category.getTitle());
                        handbookScreen.categories.add(category);
                    }
                }
            } catch (IOException | JsonSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        handbookScreen.categories.sort(Comparator.comparing(BaseCategory::getTitle));
        for (BaseCategory category : handbookScreen.categories) {
            category.getEntries().sort(Comparator.comparing(Entry::getClearTitle));
        }
        LOGGER.info("Loaded " + handbookScreen.categories.size() + " categories");
    }

    private void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openScreen.wasPressed()) {
                if (!(client.currentScreen instanceof HandbookScreen)) openHandbookScreen();
                while (openScreen.wasPressed()) {
                    //drain all presses
                }
            }
            if (addLocation.wasPressed()) {
                if (!(client.currentScreen instanceof LocationScreen)) openLocationScreen();
                while (addLocation.wasPressed()) {
                    //drain all presses
                }
            }
            npcWriter.findEntities();
            WaypointManager.tick();
            if (AreaSelector.isActive()) AreaSelector.emitParticles();
            if (client.currentScreen instanceof HandbookScreen) handbookScreen.filterEntries();
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
    }

    private void registerKeyBinds() {
        openScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open handbook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_T, "Handbook 2.0"));
        addLocation = KeyBindingHelper.registerKeyBinding(new KeyBinding("Add location", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_L, "Handbook 2.0"));
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("handbook")
                        .then(literal("dump")
                                .then(literal("NPC").executes(ctx -> npcWriter.write()))
                                .then(literal("locations").executes(ctx -> locationWriter.write()))
                                .then(literal("advancements")
                                        .then(argument("Root", StringArgumentType.string()).executes(ctx ->
                                                AdvancementWriter.dumpAdvancements(StringArgumentType.getString(ctx, "Root")))))
                                .then(literal("all").executes(ctx -> dumpAll())))
                        .then(literal("add")
                                .then(literal("location").then(argument("Name", StringArgumentType.string())
                                        .suggests(this::getSuggestions).executes(ctx ->
                                                locationWriter.addLocation(StringArgumentType.getString(ctx, "Name")))))
                                .then(literal("NPC").then(argument("Target", CEntityArgumentType.entity()).executes(ctx ->
                                        npcWriter.addNPC(CEntityArgumentType.getCEntity(ctx, "Target"), true)))))
                        .then(literal("waypoint")
                                .then(argument("x", IntegerArgumentType.integer())
                                        .then(argument("y", IntegerArgumentType.integer())
                                                .then(argument("z", IntegerArgumentType.integer()).executes(ctx ->
                                                        WaypointManager.setWaypoint(new WaypointEntry("Waypoint", null,
                                                                new Waypoint(
                                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                                        IntegerArgumentType.getInteger(ctx, "y"),
                                                                        IntegerArgumentType.getInteger(ctx, "z")), false, null))))))
                                .then(literal("alternate").executes(ctx -> WaypointManager.setAltPath()))
                                .then(literal("restore").executes(ctx -> WaypointManager.restoreWaypoints()))
                                .then(literal("continue").executes(ctx -> WaypointManager.continuePath()))
                                .then(literal("skip").executes(ctx ->
                                        WaypointManager.onWaypointReached(client.player, client.world)))
                                .then(literal("path").executes(ctx -> WaypointManager.addPathToChain()))
                                .then(literal("info").executes(ctx -> WaypointManager.printInfo())))
                        .then(literal("area")
                                .then(literal("select").executes(ctx -> AreaSelector.init()))
                                .then(literal("finish").executes(ctx -> AreaSelector.finish()))
                                .then(literal("move")
                                        .then(argument("Point", IntegerArgumentType.integer())
                                                .executes(ctx -> AreaSelector.movePointToPlayer(IntegerArgumentType.getInteger(ctx, "Point")))
                                                .then(argument("Dimension", IntegerArgumentType.integer())
                                                        .then(argument("Distance", IntegerArgumentType.integer()).executes(ctx ->
                                                                AreaSelector.movePoint(
                                                                        IntegerArgumentType.getInteger(ctx, "Point"),
                                                                        IntegerArgumentType.getInteger(ctx, "Dimension"),
                                                                        IntegerArgumentType.getInteger(ctx, "Distance"))))))))
        ));
    }

    private WaypointCategory mergeWaypointEntries(WaypointCategory category) {
        category.getEntries().forEach(entry -> {
            Gson gson = new Gson();
            File file = new File(FabricLoader.getInstance().getConfigDir() + "/handbook/waypoints/" + entry.getID() + ".json");
            WaypointEntry[] waypoints = new WaypointEntry[0];
            try {
                waypoints = gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), WaypointChain.class).getWaypoints();
            } catch (IOException e) {
                LOGGER.error("Failed to read waypoint entry file " + entry.getID() + ".json. Trying to open it in-game" +
                        " will likely cause a crash.");
            }
            entry.setChain(new WaypointChain(waypoints));
        });
        return category;
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

    //returns int because it's used in command
    @SuppressWarnings("SameReturnValue")
    public static int dumpAll() {
        npcWriter.write();
        locationWriter.write();
        handbookScreen.markedEntries.write();
        LOGGER.info("Saved all handbook entries.");
        return 1;
    }

    private CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        for (BaseCategory category : handbookScreen.categories) {
            if (!category.getTitle().equals("Locations")) continue;

            for (Entry entry : category.getEntries()) {
                if (entry.getClearTitle().toLowerCase().contains(builder.getInput().toLowerCase()
                        .replace("/handbook add location ", ""))) builder.suggest(entry.getClearTitle());
            }
        }
        return builder.buildFuture();
    }
}
