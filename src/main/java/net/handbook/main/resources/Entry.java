package net.handbook.main.resources;

import net.handbook.main.HandbookScreen;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOfferList;

import java.util.HashMap;

public class Entry {

    final String title;
    final String text;
    final Identifier image;

    public Entry(String title, String text, Identifier image) {
        this.title = title;
        this.text = text;
        this.image = image;
    }

    public void mouseClicked() {
        HandbookScreen.displayWidget.setEntry(this);
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

    public Identifier getImage() {
        return image;
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
}
