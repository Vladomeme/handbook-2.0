package net.handbook.main.editor;

import net.handbook.main.feature.WaypointManager;
import net.handbook.main.resources.entry.PositionEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

public class LocationWriter {

    final static MinecraftClient client = MinecraftClient.getInstance();
    final static ChatHud chat = client.inGameHud.getChatHud();

    public static CategoryWriter<PositionEntry> writer;

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

        chat.addMessage(Text.of("New location added: " + name + "."));

        writer.add(new PositionEntry(name, "", "", WaypointManager.getShard(),
                new int[]{(int) entity.getX(), (int) entity.getY(), (int) entity.getZ()}));
    }

    public static void delete(PositionEntry entry) {
        if (writer.delete(entry)) chat.addMessage(Text.of("Entry removed: " + entry.title()));
        else chat.addMessage(Text.of("Failed to delete this entry"));
    }
}
