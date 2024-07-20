package net.handbook.main.resources.category;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MarkCategory {

    @SuppressWarnings("unused")
    final String type = "mark";
    @SuppressWarnings("unused")
    HashMap<String, List<String>> entries;

    public void addCategory(String name) {
        entries.put(name, new ArrayList<>());
    }

    public List<String> getMarkedEntries(String category) {
        return entries.get(category);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored") //for .mkdirs()
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
            HandbookClient.LOGGER.error(e.getMessage());
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }
}
