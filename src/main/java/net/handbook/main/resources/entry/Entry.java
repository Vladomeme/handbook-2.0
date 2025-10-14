package net.handbook.main.resources.entry;

import net.handbook.main.HandbookClient;
import net.handbook.main.resources.HandbookTradeOffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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

    public List<HandbookTradeOffer> getOffers() {
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
        if (getSortableTitle() != null && entry.getSortableTitle() != null) {
            result = getSortableTitle().compareTo(entry.getSortableTitle());
            if (result != 0) return result;
        }
        if (getShard() != null && entry.getShard() != null) {
            result = getShard().compareTo(entry.getShard());
            if (result != 0) return result;
        }
        if (getID() != null && entry.getID() != null) {
            return getID().compareTo(entry.getID());
        }
        return result;
    }
}
