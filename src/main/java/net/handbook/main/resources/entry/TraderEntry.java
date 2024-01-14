package net.handbook.main.resources.entry;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.handbook.main.HandbookClient;
import net.handbook.main.scanner.NPCWriter;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TraderEntry extends PositionedEntry {

    final String id;

    public TraderEntry(String title, String text, String image, String shard, String position, String id) {
        super(title, text, image, shard, position);
        this.id = id;
    }

    @Override
    public TradeOfferList getOffers() {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir() + "/handbook/trades/" + id + ".txt");
        if (!Files.exists(path)) return null;
        try {
            TradeOfferList offers = new TradeOfferList(StringNbtReader.parse(NPCWriter.decompressTrades(Files.readString(path))));
            for (TradeOffer offer : offers) {
                offer.disable();
            }
            return offers;
        } catch (CommandSyntaxException | IOException e) {
            HandbookClient.LOGGER.error("Unable to read trader's offers. Data might be damaged.");
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getID() {
        return id;
    }
}
