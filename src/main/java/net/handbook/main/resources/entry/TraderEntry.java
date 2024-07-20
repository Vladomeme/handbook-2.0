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

    public TraderEntry(String title, String text, String image, String shard, int[] position) {
        super(title, text, image, shard, position);
        this.id = NPCWriter.getID(title, position[0], position[1], position[2]);
    }

    public TraderEntry(String id) {
        super(null, null, null, null, null);
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

    public String getOffersRaw() {
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
