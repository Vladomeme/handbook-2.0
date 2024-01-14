package net.handbook.main.resources.entry;

import net.handbook.main.feature.HandbookScreen;
import net.handbook.main.resources.waypoint.Waypoint;
import net.minecraft.village.TradeOfferList;

import java.util.HashMap;

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
        return image != null && !image.equals("");
    }

    public HashMap<String, String> getTextFields() {
        return null;
    }

    public TradeOfferList getOffers() {
        return null;
    }

    public String getID() {
        return null;
    }

    public Waypoint getWaypoint() {
        return null;
    }
}
