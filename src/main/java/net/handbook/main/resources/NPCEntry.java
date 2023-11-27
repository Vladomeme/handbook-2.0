package net.handbook.main.resources;

import net.minecraft.village.TradeOfferList;

public class NPCEntry extends Entry {

    private final TradeOfferList offers;

    public NPCEntry(TradeOfferList offers) {
        super(null, null, null);
        this.offers = offers;
    }

    public TradeOfferList getOffers() {
        return offers;
    }
}
