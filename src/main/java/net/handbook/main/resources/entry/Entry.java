package net.handbook.main.resources.entry;

import net.handbook.main.HandbookClient;
import net.minecraft.village.TradeOfferList;
import org.jetbrains.annotations.NotNull;

public class Entry extends BaseEntry implements Comparable<Entry> {

    public Entry(String title, String text, String image) {
        super(title, text, image);
    }

    @Override
    public void mouseClicked() {
        HandbookClient.handbookScreen.displayWidget.setEntry(this);
        HandbookClient.handbookScreen.openDisplay();
    }

    public String getShard() {
        return null;
    }

    public int[] getPosition() {
        return null;
    }

    public int[] getArea() {
        return null;
    }

    public boolean hasOffers() {
        return false;
    }

    public TradeOfferList getOffers() {
        return null;
    }

    public String getID() {
        return null;
    }

    public WaypointEntry[] getWaypoints() {
        return null;
    }

    @Override
    public int compareTo(@NotNull Entry entry) {
        int result = 0;
        if (getClearTitle() != null) {
            result = getClearTitle().compareTo(entry.getClearTitle());
            if (result != 0) return result;
        }
        if (getShard() != null) {
            result = getShard().compareTo(entry.getShard());
            if (result != 0) return result;
        }
        if (getID() != null) {
            return getID().compareTo(entry.getID());
        }
        return result;
    }
}
