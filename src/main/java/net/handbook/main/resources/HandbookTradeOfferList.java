package net.handbook.main.resources;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.handbook.main.editor.NPCWriter;

import java.util.List;

public record HandbookTradeOfferList(List<HandbookTradeOffer> entries, int lastChanged) {

    public static final HandbookTradeOfferList EMPTY = new HandbookTradeOfferList(List.of());

    public static final Codec<HandbookTradeOfferList> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            HandbookTradeOffer.LIST_CODEC.fieldOf("entries").forGetter(HandbookTradeOfferList::entries),
            Codec.INT.optionalFieldOf("lastChanged", Math.toIntExact(System.currentTimeMillis() / 1000)).forGetter(HandbookTradeOfferList::lastChanged)
    ).apply(instance, HandbookTradeOfferList::new));

    public HandbookTradeOfferList(List<HandbookTradeOffer> offers) {
        this(offers, Math.toIntExact(System.currentTimeMillis() / 1000));
    }

    public static int lastChangedOf(byte[] bytes, boolean bundled) {
        String s = NPCWriter.decompressTrades(bytes);
        int pos1 = s.indexOf("lastChanged");
        if (pos1 == -1) return bundled ? 1 : 0;

        pos1 += 12;
        int pos2 = pos1;
        while (pos2 != s.length() && 47 <= s.charAt(pos2) && s.charAt(pos2) < 58) pos2++;

        try {
            return Integer.parseInt(s.substring(pos1, pos2));
        }
        catch (NumberFormatException e) {
            return bundled ? 1 : 0;
        }
    }
}
