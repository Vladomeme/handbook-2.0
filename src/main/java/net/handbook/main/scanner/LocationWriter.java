package net.handbook.main.scanner;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class LocationWriter {

    String title = "Locations";
    final List<Location> entries = new ArrayList<>();

    public void addLocation(String name) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;
        ClientPlayerEntity entity = MinecraftClient.getInstance().player;
        if (entity == null) return;

        if (name.startsWith("\"")) name = name.replace("\"", "");

        HandbookClient.LOGGER.info("ADDING NEW LOCATION: " + name);
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("New location added: " + name +
                ". Don't forget to dump it before closing the game!"));

        entries.add(new Location(name, world.getRegistryKey().getValue().toString(), entity.getX(), entity.getY(), entity.getZ()));
    }

    public void write() {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("DUMPING " + entries.size() +
                " LOCATIONS INTO \"handbook_locations.json\" in /config."));
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            writer = gson.newJsonWriter(new FileWriter(new File(FabricLoader.getInstance().getConfigDir().toFile(), "handbook_locations.json")));
            writer.setIndent("    ");
            gson.toJson(this, LocationWriter.class, writer);
        } catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't save.");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }
}
