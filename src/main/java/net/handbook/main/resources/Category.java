package net.handbook.main.resources;

import net.handbook.main.HandbookScreen;

import java.util.List;

public class Category extends Entry {

    private final List<Entry> entries;

    public Category(List<Entry> entries) {
        super(null, null, null);
        this.entries = entries;
    }

    @Override
    public void mouseClicked() {
        HandbookScreen.setEntries(this.entries);
    }

    public List<Entry> getEntries() {
        return entries;
    }
}
