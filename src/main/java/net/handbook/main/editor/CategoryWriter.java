package net.handbook.main.editor;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.resources.category.BaseCategory;
import net.handbook.main.resources.category.WaypointCategory;
import net.handbook.main.resources.entry.WaypointChain;
import net.handbook.main.resources.entry.WaypointEntry;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CategoryWriter<T extends BaseCategory>
        implements Comparable<CategoryWriter<? extends BaseCategory>> {

    private final Class<T> c;
    public final T category;
    private final Path path;

    public boolean shouldUpdate = false;

    public CategoryWriter(Class<T> c, Path path) {
        this.c = c;
        this.path = path;
        this.category = read();
    }

    public T read() {
        try {
            T category = (new Gson()).fromJson(Files.readString(path, StandardCharsets.UTF_8), c);

            if (c == WaypointCategory.class)
                return mergeWaypointEntries(category);
            return (new Gson()).fromJson(Files.readString(path, StandardCharsets.UTF_8), c);
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
            gson.toJson(category, category.getClass(), writer);
        } catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't save file {}", path);
            HandbookClient.LOGGER.error(e.getMessage());
        } finally {
            IOUtils.closeQuietly(writer);
        }
        shouldUpdate = false;
    }

//    public void add(Entry entry) {
//        category.getEntries().add(entry);
//    }
//
//    public void delete(Entry entry) {
//        category.getEntries().remove(entry);
//    }

    public T mergeWaypointEntries(T category) {
        category.getEntries().forEach(entry -> {
            Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/waypoints/" + entry.getID() + ".json");
            try {
                ((WaypointEntry) entry).setChain(new WaypointChain(
                        (new Gson()).fromJson(Files.readString(path, StandardCharsets.UTF_8), WaypointChain.class).getWaypoints()));
            } catch (IOException e) {
                HandbookClient.LOGGER.error("Failed to read waypoint entry file {}.json. Trying to open it in-game will likely cause a crash.", entry.getID());
            }
        });
        return category;
    }

    @Override
    public int compareTo(@NotNull CategoryWriter o) {
        return category.getTitle().compareTo(o.category.getTitle());
    }
}
