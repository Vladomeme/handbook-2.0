package net.handbook.main.editor;

import net.handbook.main.HandbookClient;
import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.entry.PositionedEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

public class LocationWriter {

    final static MinecraftClient client = MinecraftClient.getInstance();
    final static ChatHud chat = client.inGameHud.getChatHud();

    public static CategoryWriter<PositionedEntry> writer;

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

        writer.entries().add(new PositionedEntry(name, "", "", WaypointManager.getShard(),
                new int[]{(int) entity.getX(), (int) entity.getY(), (int) entity.getZ()}));
        writer.setUpdate();
    }

    public static void delete(PositionedEntry entry) {
        if (writer.entries().remove(entry)) {
            writer.setUpdate();
            chat.addMessage(Text.of("Entry removed: " + entry.getTitle()));
        }
        else chat.addMessage(Text.of("Failed to delete this entry"));
    }
}
