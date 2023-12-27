package net.handbook.main.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Path;

public class HandbookConfig {

    public boolean enabled = true;
    public boolean enableScanner = false;

    private static final File FILE = new File(Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook").toFile(), "handbook.json");

    public static final HandbookConfig INSTANCE = read();

    public static HandbookConfig read() {
        if (!FILE.exists())
            return new HandbookConfig().write();

        Reader reader = null;
        try {
            return new Gson().fromJson(reader = new FileReader(FILE), HandbookConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public HandbookConfig write() {
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            writer = gson.newJsonWriter(new FileWriter(FILE));
            writer.setIndent("    ");
            gson.toJson(gson.toJsonTree(this, HandbookConfig.class), writer);
        } catch (Exception e) {
            HandbookClient.LOGGER.error("Couldn't save config");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
        return this;
    }

}
