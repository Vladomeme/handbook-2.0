package net.handbook.main.editor;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.feature.WaypointManager;
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

    //returns int because it's used in command
    @SuppressWarnings("SameReturnValue")
    public int addLocation(String name) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return 1;
        ClientPlayerEntity entity = MinecraftClient.getInstance().player;
        if (entity == null) return 1;

        if (name.startsWith("\"")) name = name.replace("\"", "");

        HandbookClient.LOGGER.info("ADDING NEW LOCATION: " + name);
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("New location added: " + name +
                ". Reload resources to see it in the handbook."));

        newCount++;
        entries.add(new Location(name, WaypointManager.getShard(), entity.getX(), entity.getY(), entity.getZ()));
        return 1;
    }

    public void deleteEntry(String title) {
        for (Location entry : entries) {
            if (entry.title.equals(title)) {
                entries.remove(entry);
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Entry removed: " + title));
                return;
            }
        }
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("Unable to delete this entry"));
    }

    //returns int because it's used in command
    @SuppressWarnings({"ResultOfMethodCallIgnored", "SameReturnValue"})
    public int write() {
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
            newCount = 0;
        } catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't save locations.json.");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
        return 1;
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
