package net.handbook.main.resources.category;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MarkCategory extends BaseCategory {

    final String type;
    final HashMap<String, List<String>> entries;

    public MarkCategory(String title, String text, String image, HashMap<String, List<String>> entries) {
        super("mark", title, text, image, null);
        this.type = "mark";
        this.entries = entries;
    }
    @Override
    public String getType() {
        return type;
    }

    public void addCategory(String name) {
        entries.put(name, new ArrayList<>());
    }

    public List<String> getMarkedEntries(String category) {
        return entries.get(category);
    }

    public void write() {
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            File file = new File(FabricLoader.getInstance().getConfigDir() + "/handbook", "favourite.json");
            file.getParentFile().mkdirs();
            writer = gson.newJsonWriter(new FileWriter(file));
            writer.setIndent("    ");
            gson.toJson(this, MarkCategory.class, writer);
        } catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't save favourite.json. Entries might have been deleted. :(");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    public static MarkCategory read() {
        Gson gson = new Gson();
        try {
            File file = new File(FabricLoader.getInstance().getConfigDir() + "/handbook", "favourite.json");
            return gson.fromJson(Files.readString(Path.of(file.getPath()), StandardCharsets.UTF_8), MarkCategory.class);
        }
        catch (Exception e) {
            HandbookClient.LOGGER.error("Could not find favourite.json in config/handbook/.");
        }
        return null;
    }
}
