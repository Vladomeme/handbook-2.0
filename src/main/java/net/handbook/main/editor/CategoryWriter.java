package net.handbook.main.editor;

import com.google.gson.Gson;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.resources.EntryType;
import net.handbook.main.resources.entry.*;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CategoryWriter<E extends Entry> implements Comparable<CategoryWriter<E>> {

    public final Category<E> category;
    public final Path path;

    public boolean shouldUpdate = false;
    private boolean locked = false;
    public List<Entry> addLog;
    public List<Entry> removeLog;

    public CategoryWriter(Path path, TypeToken<Category<E>> typeToken, JsonObject jsonObject) {
        this.path = path;
        this.category = read(typeToken, jsonObject);
    }

    public CategoryWriter(Category<E> category) {
        this.category = category;
        this.path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/" +
                category.title().toLowerCase().replaceAll("[^a-z0-9]", "") + ".json");
        setUpdate();
    }

    public Category<E> read(TypeToken<Category<E>> typeToken, JsonObject jsonObject) {
        try {
            Category<E> category = (new Gson()).fromJson(jsonObject, typeToken.getType());

            if (category.type().equals(EntryType.waypoint)) return mergeWaypointEntries(category);
            return category;
        }
        catch (Exception e) {
            HandbookClient.LOGGER.error("[Handbook 2.0] Failed to read file {}", path);
            throw e;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") //for .mkdirs()
    public void write() {
        if (!shouldUpdate) return;
        HandbookClient.LOGGER.info("[Handbook 2.0] Saved data for category {}", category.clearTitle());

        Gson gson = new Gson();
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = null;
        try {
            File file = path.toFile();
            file.getParentFile().mkdirs();
            jsonWriter = gson.newJsonWriter(stringWriter);
            jsonWriter.setIndent("    ");

            gson.toJson(category, Category.class, jsonWriter);
            String s = stringWriter.toString();
            if (s.length() < 2) {
                HandbookClient.LOGGER.warn("[Handbook 2.0] Data size for category file {} is too small: {}. Corruption suspected, skipping writing.", path, s.length());
                return;
            }
            Files.writeString(path, s, StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            HandbookClient.LOGGER.error("[Handbook 2.0] Failed to save file {}", path);
            HandbookClient.LOGGER.error(e.getMessage());
        }
        finally {
            IOUtils.closeQuietly(stringWriter);
            IOUtils.closeQuietly(jsonWriter);
        }
        shouldUpdate = false;
    }

    public List<E> entries() {
        return category.entries();
    }

    @SuppressWarnings("unchecked")
    public void add(Entry entry) {
        if (locked) {
            addLog.add(entry);
            return;
        }
        setUpdate();
        category.entries().add((E) entry);
    }

    @SuppressWarnings("unchecked")
    public boolean delete(Entry entry) {
        if (locked) {
            removeLog.add(entry);
            return false;
        }
        setUpdate();
        return category.entries().remove((E) entry);
    }

    //locking is used to delay any entry additions and removals while iterating
    public void lock() {
        locked = true;
        addLog = new ArrayList<>();
        removeLog = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public void unlock() {
        locked = false;

        if (!addLog.isEmpty() || !removeLog.isEmpty()) setUpdate();

        for (Entry entry : addLog) category.entries().add((E) entry);
        for (Entry entry : removeLog) category.entries().remove((E) entry);

        addLog = null;
        removeLog = null;
    }

    public void setUpdate() {
        if (!shouldUpdate) HandbookClient.LOGGER.info("[Handbook 2.0] Category {} data will be updated on reload.", category.clearTitle());
        shouldUpdate = true;
    }

    private Category<E> mergeWaypointEntries(Category<E> category) {
        category.entries().forEach(entry -> {
            Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/waypoints/" + entry.id() + ".json");
            try {
                ((WaypointEntry) entry).setChain(
                        (new Gson()).fromJson(Files.readString(path, StandardCharsets.UTF_8), WaypointEntry.class).waypoints());
            } catch (IOException e) {
                HandbookClient.LOGGER.error("[Handbook 2.0] Failed to read waypoint entry file {}.json." +
                        "No idea what would happen if you open it.", entry.id());
            }
        });
        return category;
    }

    @Override
    public int compareTo(@NotNull CategoryWriter<E> o) {
        return category.title().compareTo(o.category.title());
    }
}
