package net.handbook.main.resources.entry;

import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.resources.HandbookTradeOfferList;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

public class Entry extends BaseEntry implements Comparable<Entry> {

    public Entry(String title, String text, String image) {
        super(title, text, image);
    }

    @Override
    public void mouseClicked() {
        if (MinecraftClient.getInstance().currentScreen instanceof HandbookScreen screen) {
            screen.displayEntry(this);
            screen.openDisplay();
        }
    }

    public String shard() {
        return null;
    }

    public int[] position() {
        return null;
    }

    public String icon() {
        return null;
    }

    public int[] area() {
        return null;
    }

    public HandbookTradeOfferList offers() {
        return null;
    }

    public boolean hasOffers() {
        return false;
    }

    public String id() {
        return null;
    }

    public WaypointEntry[] waypoints() {
        return null;
    }

    @Override
    public int compareTo(@NotNull Entry entry) {
        int result = 0;
        if (sortableTitle() != null && entry.sortableTitle() != null) {
            result = sortableTitle().compareTo(entry.sortableTitle());
            if (result != 0) return result;
        }
        if (shard() != null && entry.shard() != null) {
            result = shard().compareTo(entry.shard());
            if (result != 0) return result;
        }
        if (id() != null && entry.id() != null) {
            return id().compareTo(entry.id());
        }
        return result;
    }
}
