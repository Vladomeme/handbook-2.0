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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LocationWriter {

    String type = "positioned";
    String title = "Locations";
    final List<Location> entries = new ArrayList<>();
    private transient int newCount = 0;

    public void addLocation(String name) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;
        ClientPlayerEntity entity = MinecraftClient.getInstance().player;
        if (entity == null) return;

        if (name.startsWith("\"")) name = name.replace("\"", "");

        HandbookClient.LOGGER.info("ADDING NEW LOCATION: " + name);
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("New location added: " + name +
                ". Don't forget to save it!"));

        newCount++;
        entries.add(new Location(name, world.getRegistryKey().getValue().toString(), entity.getX(), entity.getY(), entity.getZ()));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void write() {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Saved \"locations.json\" with " + entries.size() +
                " locations total, " + newCount + " new locations."));
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            File file = new File(FabricLoader.getInstance().getConfigDir() + "/handbook", "locations.json");
            file.getParentFile().mkdirs();
            writer = gson.newJsonWriter(new FileWriter(file));
            writer.setIndent("    ");
            gson.toJson(this, LocationWriter.class, writer);
            this.newCount = 0;
        } catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't save locations.json.");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    public static LocationWriter read() {
        Gson gson = new Gson();
        try {
            File file = new File(FabricLoader.getInstance().getConfigDir() + "/handbook", "locations.json");
            return gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), LocationWriter.class);
        }
        catch (Exception e) {
            HandbookClient.LOGGER.error("Could not find locations.json in config/handbook/. A new file will be created when dumping.");
        }
        return new LocationWriter();
    }
}
