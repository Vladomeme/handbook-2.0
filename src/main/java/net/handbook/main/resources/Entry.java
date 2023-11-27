package net.handbook.main.resources;

import net.handbook.main.HandbookScreen;
import net.minecraft.util.Identifier;

public class Entry {

    private final String title;
    private final String text;
    private final Identifier image;

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
}
