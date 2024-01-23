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
import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.category.*;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.WaypointChain;
import net.handbook.main.resources.entry.WaypointEntry;
import net.handbook.main.resources.waypoint.Waypoint;
import net.handbook.main.scanner.AdvancementWriter;
import net.handbook.main.scanner.LocationWriter;
import net.handbook.main.scanner.NPCWriter;
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

    public static KeyBinding openScreen;

    public static final LocationWriter locationWriter = LocationWriter.read();
    public static final NPCWriter npcWriter = NPCWriter.read();

    static boolean firstLoad = false;

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {

            @Override
            public Identifier getFabricId() {
                return new Identifier("handbook", "resources");
            }

            @Override
            public void reload(ResourceManager manager) {
                Gson gson = new Gson();

                HandbookScreen.categories.clear();
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
                                HandbookScreen.categories.add(category);
                            }
                            case "waypoint" -> {
                                WaypointCategory category = gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), WaypointCategory.class);
                                LOGGER.info("Loading waypoint category " + category.getTitle());
                                HandbookScreen.categories.add(mergeWaypointEntries(category));
                            }
                            case "trader" -> {
                                TraderCategory category = gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), TraderCategory.class);
                                LOGGER.info("Loading trader category " + category.getTitle());
                                HandbookScreen.categories.add(category);
                            }
                            default -> {
                                Category category = gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), Category.class);
                                LOGGER.info("Loading normal category " + category.getTitle());
                                HandbookScreen.categories.add(category);
                            }
                        }
                    } catch (IOException | JsonSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
                HandbookScreen.categories.sort(Comparator.comparing(BaseCategory::getTitle));
                for (BaseCategory category : HandbookScreen.categories) {
                    category.getEntries().sort(Comparator.comparing(Entry::getClearTitle));
                }
                LOGGER.info("Loaded " + HandbookScreen.categories.size() + " categories");
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openScreen.wasPressed()) {
                if (!(MinecraftClient.getInstance().currentScreen instanceof HandbookScreen)) openScreen();
                while (openScreen.wasPressed()) {
                    //don't care
                }
            }
            npcWriter.findEntities();
            WaypointManager.tick();
            if (MinecraftClient.getInstance().currentScreen instanceof HandbookScreen) HandbookScreen.filterEntries();
            if (MinecraftClient.getInstance().world != null && WaypointManager.shouldRestore())
                WaypointManager.sendRestoreMessage();
        });

        WorldRenderEvents.AFTER_ENTITIES.register((ctx) -> {
            if (WaypointManager.isActive() && (WaypointManager.getDistance() > 30)) WaypointManager.renderBeacon(ctx);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (WaypointManager.waypointsSaved()) WaypointManager.prepareRestoreMessage();
        });

        openScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open handbook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_T, "Handbook 2.0"));

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
                                        WaypointManager.onWaypointReached(MinecraftClient.getInstance().player, MinecraftClient.getInstance().world)))
                                .then(literal("path").executes(ctx -> WaypointManager.addPathToChain()))
                                .then(literal("info").executes(ctx -> WaypointManager.printInfo())))
        ));

        LOGGER.info("Handbook 2.0 loaded!");
    }

    private static WaypointCategory mergeWaypointEntries(WaypointCategory category) {
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

    public static void openScreen() {
        MinecraftClient.getInstance().setScreen(new HandbookScreen(Text.of("")));
    }

    //returns int because it's used in command
    public static int dumpAll() {
        npcWriter.write();
        locationWriter.write();
        LOGGER.info("Saved all new handbook entries.");
        return 1;
    }

    private CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        for (BaseCategory category : HandbookScreen.categories) {
            if (!category.getTitle().equals("Locations")) continue;

            for (Entry entry : category.getEntries()) {
                if (entry.getClearTitle().toLowerCase().contains(builder.getInput().toLowerCase()
                        .replace("/handbook add location ", ""))) builder.suggest(entry.getClearTitle());
            }
        }
        return builder.buildFuture();
    }
}
