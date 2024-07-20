package net.handbook.main.editor;

import net.handbook.main.HandbookClient;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.PositionedEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

public class LocationWriter {

    final static MinecraftClient client = MinecraftClient.getInstance();
    final static ChatHud chat = client.inGameHud.getChatHud();

    public static CategoryWriter writer;

    @SuppressWarnings("SameReturnValue")
    public static int add(String name) {
        addLocation(name);
        return 1;
    }

    public static void addLocation(String name) {
        ClientWorld world = client.world;
        ClientPlayerEntity entity = client.player;
        if (world == null || entity == null) return;

        if (name.startsWith("\"")) name = name.replace("\"", "");

        HandbookClient.LOGGER.info("ADDING NEW LOCATION: {}", name);
        chat.addMessage(Text.of("New location added: " + name + "."));

        writer.category.getEntries().add(new PositionedEntry(name, "", "", WaypointManager.getShard(),
                new int[]{(int) entity.getX(), (int) entity.getY(), (int) entity.getZ()}));
        writer.shouldUpdate = true;
    }

    public static void delete(Entry entry) {
        if (writer.category.getEntries().remove(entry)) {
            writer.shouldUpdate = true;
            chat.addMessage(Text.of("Entry removed: " + entry.getTitle()));
        }
        else chat.addMessage(Text.of("Failed to delete this entry"));
    }
}
