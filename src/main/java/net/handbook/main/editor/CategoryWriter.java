package net.handbook.main.editor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.resources.category.Category;
import net.handbook.main.resources.entry.*;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CategoryWriter<E extends Entry> implements Comparable<CategoryWriter<E>> {

    public final Category<E> category;
    private final Path path;

    public boolean shouldUpdate = false;

    public CategoryWriter(Path path, TypeToken<Category<E>> typeToken) {
        this.path = path;
        this.category = read(typeToken);
    }

    public CategoryWriter(Category<E> category) {
        this.category = category;
        this.path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/" +
                category.getTitle().toLowerCase().replaceAll("[^a-z0-9]", "") + ".json");
        this.shouldUpdate = true;
    }

    public Category<E> read(TypeToken<Category<E>> typeToken) {
        try {
            Category<E> category = (new Gson()).fromJson(Files.readString(path, StandardCharsets.UTF_8), typeToken.getType());

            if (category.getType().equals("waypoint")) return mergeWaypointEntries(category);
            return category;
        }
        catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't read file {}", path);
            return null;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") //for .mkdirs()
    public void write() {
        if (!shouldUpdate) return;
        HandbookClient.LOGGER.info("Saved handbook category {}", category.getClearTitle());

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

    @SuppressWarnings("unchecked")
    public void add(Entry entry) {
        category.getEntries().add((E) entry);
    }

    @SuppressWarnings("unchecked")
    public void delete(Entry entry) {
        category.getEntries().remove((E) entry);
    }

    private Category<E> mergeWaypointEntries(Category<E> category) {
        category.getEntries().forEach(entry -> {
            Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/waypoints/" + entry.getID() + ".json");
            try {
                ((WaypointEntry) entry).setChain(
                        (new Gson()).fromJson(Files.readString(path, StandardCharsets.UTF_8), WaypointEntry.class).getWaypoints());
            } catch (IOException e) {
                HandbookClient.LOGGER.error("Failed to read waypoint entry file {}.json." +
                        "No idea what would happen if you open it.", entry.getID());
            }
        });
        return category;
    }

    @Override
    public int compareTo(@NotNull CategoryWriter<E> o) {
        return category.getTitle().compareTo(o.category.getTitle());
    }
}
