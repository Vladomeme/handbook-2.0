package net.handbook.main.resources;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;

import java.util.List;
import java.util.Optional;

public record HandbookTradeOffer(ItemStack buyItem1, Optional<ItemStack> buyItem2, ItemStack sellItem) {

    public static final Codec<HandbookTradeOffer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("buyItem1").forGetter(HandbookTradeOffer::buyItem1),
            ItemStack.CODEC.optionalFieldOf("buyItem2").forGetter(HandbookTradeOffer::buyItem2),
            ItemStack.CODEC.fieldOf("sellItem").forGetter(HandbookTradeOffer::sellItem)
    ).apply(instance, HandbookTradeOffer::new));

    public static final Codec<List<HandbookTradeOffer>> LIST_CODEC = CODEC.listOf();

    public static HandbookTradeOffer fromTradeOffer(TradeOffer tradeOffer) {
        ItemStack item1 = tradeOffer.getOriginalFirstBuyItem();
        Optional<ItemStack> item2 = tradeOffer.getSecondBuyItem().map(TradedItem::itemStack);
        ItemStack item3 = tradeOffer.getSellItem();

        return new HandbookTradeOffer(item1, item2, item3);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HandbookTradeOffer(ItemStack item1, Optional<ItemStack> item2, ItemStack item))) return false;
        return buyItem1.hashCode() == item1.hashCode()
                && (buyItem2.isEmpty() && item2.isEmpty() || (buyItem2.isPresent() && item2.isPresent() && buyItem2.hashCode() == item2.hashCode()))
                && sellItem.hashCode() == item.hashCode();
    }
}
