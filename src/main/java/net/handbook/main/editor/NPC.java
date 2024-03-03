package net.handbook.main.editor;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class NPC {

    final String title;
    final String text;
    final String image;
    final String shard;
    final int[] position;
    transient String offers;
    final String id;

    public NPC(String title, String world, double x, double y, double z) {
        this.title = title;
        this.text = "";
        this.image = "";
        this.shard = world.replace("monumenta:", "").split("-")[0];
        this.position = new int[]{(int) x, (int) y, (int) z};
        this.offers = "";
        this.id = getID(title, x, y, z);
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

    public static String getID(String title, double x, double y, double z) {
        return title.toLowerCase().replaceAll("§.", "").replaceAll("[^A-Za-z0-9]", "")
                + ((int) x + (int) y + (int) z);
    }
}
