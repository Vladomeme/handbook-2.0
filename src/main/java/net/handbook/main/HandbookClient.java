package net.handbook.main;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.xpple.clientarguments.arguments.CEntityArgument;
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
import net.handbook.main.config.HandbookConfig;
import net.handbook.main.editor.*;
import net.handbook.main.feature.*;
import net.handbook.main.resources.ScreenWithFilters;
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

import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HandbookClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("handbook");
    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static KeyBinding mainScreenKey;
    public static KeyBinding tradeScreenKey;
    @SuppressWarnings("unused")
    public static KeyBinding mapScreenKey;
    public static KeyBinding locationScreenKey;

    static int errorTimer = -1;
    public static int clickTimer;

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {

            @Override
            public Identifier getFabricId() {
                return Identifier.of("handbook", "resources");
            }

            @Override
            public void reload(ResourceManager manager) {
                DataManager.onReload(manager);
            }
        });

        registerKeyBinds();
        registerEvents();
        registerCommands();

        LOGGER.info("Handbook 2.0 is installed!");
    }

    private void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (resetAndReturnKey(mainScreenKey)) openHandbookScreen();
            if (resetAndReturnKey(tradeScreenKey)) openTradeScreen();
            //todo update part 2
            //if (resetAndReturnKey(mapScreenKey)) openMapScreen();
            if (resetAndReturnKey(locationScreenKey)) openLocationScreen();

            if (client.world != null && HandbookConfig.INSTANCE.enableScanner) NPCWriter.tick();
            WaypointManager.tick();
            if (AreaSelector.isActive()) AreaSelector.emitParticles();
            if (client.currentScreen instanceof ScreenWithFilters screen) screen.scheduledFilter();
            if (client.world != null && WaypointManager.shouldRestore()) WaypointManager.sendRestoreMessage();

            if (errorTimer > -1 && --errorTimer == 0) DataManager.writerMissingError();
            clickTimer++;
        });

        WorldRenderEvents.AFTER_ENTITIES.register((ctx) -> {
            if (WaypointManager.isActive() && (WaypointManager.getDistance() > 30)) WaypointManager.renderBeacon(ctx);
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (handler.getConnection().getAddress().toString().contains("monumenta")) {
                if (WaypointManager.waypointsSaved()) WaypointManager.prepareRestoreMessage();
            }
            if (NPCWriter.writer == null) errorTimer = 100;
            NPCWriter.tick = -40;
            DataManager.setTradeEntries();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DataManager.save());
    }

    private void registerKeyBinds() {
        mainScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open handbook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_T, "Handbook 2.0"));
        tradeScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open trades", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "Handbook 2.0"));
        //todo update part 2
        //mapScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open map", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "Handbook 2.0"));
        locationScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Add location", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_L, "Handbook 2.0"));
    }

    private void registerCommands() {
        //INTENDED FOR USER USE
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("handbook")
                        .then(literal("dump_advancements")
                                .then(argument("Root", StringArgumentType.string()).executes(ctx ->
                                        AdvancementWriter.dumpAdvancements(StringArgumentType.getString(ctx, "Root")))))
                        .then(literal("add")
                                .then(literal("location").then(argument("Name", StringArgumentType.string())
                                        .suggests(this::getLocationSuggestions).executes(ctx ->
                                                LocationWriter.add(StringArgumentType.getString(ctx, "Name")))))
                                .then(literal("NPC").then(argument("Target", CEntityArgument.entity()).executes(ctx ->
                                        NPCWriter.add(CEntityArgument.getEntity(ctx, "Target"), true)))))
                        .then(literal("waypoint")
                                .then(argument("x", IntegerArgumentType.integer())
                                        .then(argument("y", IntegerArgumentType.integer())
                                                .then(argument("z", IntegerArgumentType.integer()).executes(ctx ->
                                                        WaypointManager.setWaypoint(new WaypointEntry("Waypoint", null,
                                                                new Waypoint(
                                                                        IntegerArgumentType.getInteger(ctx, "x"),
                                                                        IntegerArgumentType.getInteger(ctx, "y"),
                                                                        IntegerArgumentType.getInteger(ctx, "z"),
                                                                        null), false, null)))
                                                        .then(argument("name", StringArgumentType.string()).executes(ctx ->
                                                                WaypointManager.setWaypoint(new WaypointEntry(StringArgumentType.getString(ctx, "name"), null,
                                                                        new Waypoint(
                                                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                                                IntegerArgumentType.getInteger(ctx, "y"),
                                                                                IntegerArgumentType.getInteger(ctx, "z"),
                                                                                null), false, null))
                                                        ))))
                                .then(literal("info").executes(ctx -> WaypointManager.printInfo()))))
                        .then(literal("area")
                                .then(literal("select").executes(ctx -> AreaSelector.init()))
                                .then(literal("npc_mass_delete").executes(ctx -> NPCWriter.delete(AreaSelector.getSelection())))
                                .then(literal("npc_mass_delete_and_blacklist").executes(ctx -> NPCWriter.delete(AreaSelector.getSelection(), true))))
                        .then(literal("clear_trades").executes(ctx -> NPCWriter.clear()))
                        .then(literal("update_npcs").executes(ctx -> NPCWriter.updateNearby(32, true))
                                .then(argument("radius", IntegerArgumentType.integer()).executes(ctx ->
                                        NPCWriter.updateNearby(IntegerArgumentType.getInteger(ctx, "radius"), true))))
                        .then(literal("set_persistent").executes(ctx -> NPCWriter.setPersistency(2))
                                .then(argument("radius", IntegerArgumentType.integer()).executes(ctx ->
                                        NPCWriter.setPersistency(IntegerArgumentType.getInteger(ctx, "radius")))))
                        .then(literal("remove_dupes").executes(ctx -> NPCWriter.removeDuplicates()))
//                        .then(literal("convert_trades").executes(ctx -> DataManager.convertTradeFiles()))
        ));
        //INTERNAL
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("hb_internal")
                        .then(literal("waypoint")
                                .then(literal("alternate").executes(ctx -> WaypointManager.setAltPath()))
                                .then(literal("restore").executes(ctx -> WaypointManager.restoreWaypoints()))
                                .then(literal("continue").executes(ctx -> WaypointManager.continuePath()))
                                .then(literal("skip").executes(ctx ->
                                        WaypointManager.onWaypointReached(client.player, client.world)))
                                .then(literal("path").executes(ctx -> WaypointManager.addPathToChain())))
                        .then(literal("area")
                                .then(literal("save").executes(ctx -> AreaSelector.startSaving()))
                                .then(literal("confirm").executes(ctx -> AreaSelector.saveArea()))
                                .then(literal("retry").executes(ctx -> AreaSelector.retry()))
                                .then(literal("cancel").executes(ctx -> AreaSelector.exitSaving(Text.empty(), true)))
                                .then(literal("exit").executes(ctx -> AreaSelector.exitSelection()))
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

    public static HandbookScreen openHandbookScreen() {
        if (client.currentScreen instanceof HandbookScreen screen) return screen;
        else if (DataManager.isReady(true)) {
            HandbookScreen screen = new HandbookScreen();
            client.setScreen(screen);
            return screen;
        }
        return null;
    }

    public static TradeScreen openTradeScreen() {
        if (client.currentScreen instanceof TradeScreen screen) return screen;
        else if (DataManager.isReady(true)) {
            TradeScreen screen = new TradeScreen();
            client.setScreen(screen);
            return screen;
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static MapScreen openMapScreen() {
        if (client.currentScreen instanceof MapScreen screen) return screen;
        else if (DataManager.isReady(true)) {
            MapScreen screen = new MapScreen();
            client.setScreen(screen);
            return screen;
        }
        return null;
    }

    public static void openLocationScreen() {
        if (DataManager.isReady(true) && !(client.currentScreen instanceof LocationScreen)) {
            if (HandbookConfig.INSTANCE.editorMode) client.setScreen(new LocationScreen(Text.of("")));
            else client.inGameHud.getChatHud().addMessage(Text.of("Can't open New Location screen: editor mode is disabled."));
        }
    }

    private static boolean resetAndReturnKey(KeyBinding keyBinding) {
        boolean bl = keyBinding.wasPressed();
        keyBinding.reset();
        return bl;
    }

    //Gets all entry names from a standard category "Locations" and filters them to current command input
    @SuppressWarnings("unused")
    private CompletableFuture<Suggestions> getLocationSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        for (CategoryWriter<? extends Entry> writer : DataManager.writers) {
            if (!writer.category.title().equals("Locations")) continue;

            String input = builder.getInput().toLowerCase().replace("/handbook add location ", "");
            for (Entry entry : writer.entries()) {
                if (entry.clearTitle().toLowerCase().contains(input)) builder.suggest(entry.clearTitle());
            }
        }
        return builder.buildFuture();
    }
}
