package net.handbook.main;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.resources.*;
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

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HandbookClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("handbook");

    public static KeyBinding openScreen;

    public static final LocationWriter locationWriter = LocationWriter.read();
    public static final NPCWriter npcWriter = NPCWriter.read();

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
                dumpAll();

                if (!Files.exists(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades"))) {
                    try {
                        Files.createDirectories(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades"));
                    } catch (IOException e) {
                        LOGGER.error("Failed to create handbook directories.");
                        return;
                    }
                }

                File[] files = new File(FabricLoader.getInstance().getConfigDir() + "/handbook").listFiles();
                if (files == null) {
                    LOGGER.error("No handbook categories found!");
                    return;
                }

                for (File file : files) {
                    if (!file.getName().endsWith("json")) continue;
                    try {
                        String type = gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), CategoryType.class).getType();
                        switch (type) {
                            case "positioned" -> {
                                PositionedCategory category = gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), PositionedCategory.class);
                                LOGGER.info("Loading positioned category " + category.getTitle());
                                HandbookScreen.categories.add(category);
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
            if (MinecraftClient.getInstance().currentScreen instanceof HandbookScreen) HandbookScreen.filterEntries();
        });

        openScreen = KeyBindingHelper.registerKeyBinding(new KeyBinding("Open handbook", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_T, "Handbook 2.0"));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal("handbook")
                        .then(literal("dump")
                                .then(literal("NPC").executes(context -> {
                                    npcWriter.write();
                                    return 1;
                                }))
                                .then(literal("locations").executes(context -> {
                                    locationWriter.write();
                                    return 1;
                                }))
                                .then(literal("all").executes(context -> {
                                    dumpAll();
                                    return 1;
                                })))
                        .then(literal("add")
                                .then(literal("location").then(argument("Name", StringArgumentType.string()).executes(context -> {
                                    locationWriter.addLocation(StringArgumentType.getString(context, "Name"));
                                    return 1;
                                }))))

        ));

        LOGGER.info("Handbook 2.0 loaded!");
    }

    public static void openScreen() {
        MinecraftClient.getInstance().setScreen(new HandbookScreen(Text.of("")));
    }

    public static void dumpAll() {
        npcWriter.write();
        locationWriter.write();
    }
}
