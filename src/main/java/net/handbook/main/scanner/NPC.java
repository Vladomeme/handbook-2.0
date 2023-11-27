package net.handbook.main.scanner;

import net.minecraft.village.TradeOfferList;

public class NPC {

    final String title;
    final String text;
    //TradeOfferList offers;

    public NPC(String title, String world, double x, double y, double z) {
        this.title = title;
        this.text = "Shard: " + world.replace("monumenta", "").split("-")[0] +
                "\nPosition: " +
                String.valueOf(x).split("\\.")[0] + ", " +
                String.valueOf(y).split("\\.")[0] + ", " +
                String.valueOf(z).split("\\.")[0];
        //this.offers = new TradeOfferList();
    }

    public void setOffers(TradeOfferList offers) {
        //this.offers = offers;
    }
}
