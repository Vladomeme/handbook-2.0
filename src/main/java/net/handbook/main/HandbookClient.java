package net.handbook.main;

import com.google.gson.Gson;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.handbook.main.resources.Category;
import net.handbook.main.resources.Entry;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class HandbookClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("handbook");

    public static KeyBinding openScreen;

    public static final LocationWriter locationWriter = new LocationWriter();
    public static final NPCWriter npcWriter = new NPCWriter();

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {

            @Override
            public Identifier getFabricId() {
                return new Identifier("handbook", "resources");
            }

            @SuppressWarnings("OptionalGetWithoutIsPresent")
            @Override
            public void reload(ResourceManager manager) {
                Gson gson = new Gson();

                HandbookScreen.categories.clear();

                manager.findResources("handbook", id -> id.getPath().endsWith(".json")).keySet().forEach(id -> {
                    try {
                        LOGGER.info("Trying to load category " + id.getPath());
                        HandbookScreen.categories.add(gson.fromJson(new BufferedReader(new InputStreamReader(manager.getResource(id).get().getInputStream())), Category.class));
                    } catch (IOException e) {
                        LOGGER.info("Failed to load category " + id.getPath());
                    }
                });
                HandbookScreen.categories.sort(Comparator.comparing(Category::getTitle));
                for (Category category : HandbookScreen.categories) {
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
}
