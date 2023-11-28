package net.handbook.main.scanner;

import net.handbook.main.HandbookClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class NPC {

    final String title;
    final String text;
    final String shard;
    final String position;
    String offers;

    public NPC(String title, String world, double x, double y, double z) {
        this.title = title;
        this.text = "";
        this.shard = "Shard: " + world.replace("monumenta:", "").split("-")[0];
        this.position = "Position: " +
                String.valueOf(x).split("\\.")[0] + ", " +
                String.valueOf(y).split("\\.")[0] + ", " +
                String.valueOf(z).split("\\.")[0];
        this.offers = "";
    }

    public void setOffers(TradeOfferList offers) {
        NbtCompound offersNbt = new NbtCompound();
        NbtList offerList = new NbtList();
        for (TradeOffer tradeOffer : offers) {
            NbtCompound tradeNbt = new NbtCompound();

            tradeNbt.put("buy", stripNbt(tradeOffer.getOriginalFirstBuyItem().writeNbt(new NbtCompound())));
            tradeNbt.put("buyB", stripNbt(tradeOffer.getSecondBuyItem().writeNbt(new NbtCompound())));
            tradeNbt.put("sell", stripNbt(tradeOffer.getSellItem().writeNbt(new NbtCompound())));
            offerList.add(tradeNbt);
        }
        offersNbt.put("Recipes", offerList);
        HandbookClient.LOGGER.info(offersNbt.toString());
        this.offers = offersNbt.toString()
                .replace("\\\"", "\"")
                .replace("\\\"", "\\\\\"")
                .replace("\\u0027", "'");
    }

    public NbtCompound stripNbt(NbtCompound item) {
        item.getCompound("tag").remove("Monumenta");
        item.getCompound("tag").remove("AttributeModifiers");
        return item;
    }
}
