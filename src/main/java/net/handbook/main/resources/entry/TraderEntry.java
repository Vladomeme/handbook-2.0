package net.handbook.main.resources.entry;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.editor.NPCWriter;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.village.TradeOfferList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TraderEntry extends PositionedEntry {

    final String id;

    public TraderEntry(String title, String text, String image, String shard, int[] position, String id) {
        super(title, text, image, shard, position);
        this.id = id;
    }

    @Override
    public TradeOfferList getOffers() {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + id + ".txt");
        if (!Files.exists(path)) return null;
        try {
            return new TradeOfferList(StringNbtReader.parse(NPCWriter.decompressTrades(Files.readString(path))));
        } catch (CommandSyntaxException | IOException e) {
            HandbookClient.LOGGER.error("Unable to read trader's offers: {}. Data might be damaged. {}", id, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean hasOffers() {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + id + ".txt");
        return Files.exists(path);
    }

    public static String getOffersRaw(String id) {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + id + ".txt");
        if (!Files.exists(path)) return null;
        try {
            return NPCWriter.decompressTrades(Files.readString(path));
        } catch (IOException e) {
            HandbookClient.LOGGER.error("Unable to read trader's offers: {}.", id);
            return null;
        }
    }

    @Override
    public String getID() {
        return id;
    }
}
