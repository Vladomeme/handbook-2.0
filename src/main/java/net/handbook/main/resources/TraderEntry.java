package net.handbook.main.resources;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.handbook.main.HandbookClient;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class TraderEntry extends PositionedEntry {

    final String offers;

    public TraderEntry(String title, String text, Identifier image, String shard, String position, String  offers) {
        super(title, text, image, shard, position);
        this.offers = offers;
    }

    @Override
    public TradeOfferList getOffers() {
        if (this.offers.equals("")) return null;
        try {
            TradeOfferList offers = new TradeOfferList(StringNbtReader.parse(this.offers));
            for (TradeOffer offer : offers) {
                offer.disable();
            }
            return offers;
        } catch (CommandSyntaxException e) {
            HandbookClient.LOGGER.info(this.offers);
            HandbookClient.LOGGER.info("Unable to read trader's offers. Data might be damaged.");
            throw new RuntimeException(e);
        }
    }
}
