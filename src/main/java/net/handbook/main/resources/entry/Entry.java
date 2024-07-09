package net.handbook.main.resources.entry;

import net.handbook.main.HandbookClient;
import net.minecraft.village.TradeOfferList;
import org.jetbrains.annotations.NotNull;

public class Entry implements Comparable<Entry> {

    String title;
    String text;
    final String image;

    public Entry(String title, String text, String image) {
        this.title = title;
        this.text = text;
        this.image = image;
    }

    public void mouseClicked() {
        HandbookClient.handbookScreen.displayWidget.setEntry(this);
        HandbookClient.handbookScreen.openDisplay();
    }

    public void update(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public String getClearTitle() {
        return title.replaceAll("§.", "");
    }

    public String getText() {
        return text;
    }

    public String getImage() {
        return image;
    }

    public boolean hasImage() {
        return image != null && !image.isEmpty();
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

    public TradeOfferList getOffers() {
        return null;
    }

    public boolean hasOffers() {
        return false;
    }

    public String getID() {
        return null;
    }

    public WaypointEntry[] getWaypoints() {
        return null;
    }

    @Override
    public int compareTo(@NotNull Entry entry) {
        int result = getClearTitle().compareTo(entry.getClearTitle());
        if (result != 0) return result;

        if (getShard() == null) return 0;
        result = getShard().compareTo(entry.getShard());
        if (result != 0) return result;

        if (getID() == null) return 0;
        return getID().compareTo(entry.getID());
    }
}
