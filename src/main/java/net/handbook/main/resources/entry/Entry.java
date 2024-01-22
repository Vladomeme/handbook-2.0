package net.handbook.main.resources.entry;

import net.handbook.main.feature.HandbookScreen;
import net.minecraft.village.TradeOfferList;

public class Entry {

    final String title;
    final String text;
    final String image;

    public Entry(String title, String text, String image) {
        this.title = title;
        this.text = text;
        this.image = image;
    }

    public void mouseClicked() {
        HandbookScreen.displayWidget.setEntry(this);
        HandbookScreen.openDisplay();
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

    public TradeOfferList getOffers() {
        return null;
    }

    public String getID() {
        return null;
    }

    public WaypointEntry[] getWaypoints() {
        return null;
    }
}
