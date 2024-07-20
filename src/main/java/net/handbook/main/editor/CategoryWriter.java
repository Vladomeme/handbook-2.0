package net.handbook.main.editor;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.resources.category.Category;
import net.handbook.main.resources.entry.Entry;
import net.handbook.main.resources.entry.WaypointEntry;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CategoryWriter implements Comparable<CategoryWriter> {

    public final Category category;
    private final Path path;

    public boolean shouldUpdate = false;

    public CategoryWriter(Path path) {
        this.path = path;
        this.category = read();
    }

    public Category read() {
        try {
            Category category = (new Gson()).fromJson(Files.readString(path, StandardCharsets.UTF_8), Category.class);

            if (category.getType().equals("waypoint")) return mergeWaypointEntries(category);
            return category;
        }
        catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't read file {}" , path);
            return null;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") //for .mkdirs()
    public void write() {
        if (!shouldUpdate) return;

        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            File file = path.toFile();
            file.getParentFile().mkdirs();
            writer = gson.newJsonWriter(new FileWriter(file));
            writer.setIndent("    ");
            gson.toJson(category, Category.class, writer);
        } catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't save file {}", path);
            HandbookClient.LOGGER.error(e.getMessage());
        } finally {
            IOUtils.closeQuietly(writer);
        }
        shouldUpdate = false;
    }

    public void add(Entry entry) {
        category.getEntries().add(entry);
    }

    public void delete(Entry entry) {
        category.getEntries().remove(entry);
    }

    public Category mergeWaypointEntries(Category category) {
        category.getEntries().forEach(entry -> {
            Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/waypoints/" + entry.getID() + ".json");
            try {
                ((WaypointEntry) entry).setChain(
                        (new Gson()).fromJson(Files.readString(path, StandardCharsets.UTF_8), WaypointEntry[].class));
            } catch (IOException e) {
                HandbookClient.LOGGER.error("Failed to read waypoint entry file {}.json. No idea what would happen if you open it.", entry.getID());
            }
        });
        return category;
    }

    @Override
    public int compareTo(@NotNull CategoryWriter o) {
        return category.getTitle().compareTo(o.category.getTitle());
    }
}
