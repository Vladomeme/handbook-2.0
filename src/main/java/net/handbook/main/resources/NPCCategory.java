package net.handbook.main.resources;

import net.handbook.main.HandbookScreen;

import java.util.List;

public class NPCCategory extends Entry {

    private final List<NPCEntry> entries;

    public NPCCategory(List<NPCEntry> entries) {
        super(null, null, null);
        this.entries = entries;
    }

    @Override
    public void mouseClicked() {
        HandbookScreen.setEntries(this.entries);
    }

    public List<NPCEntry> getEntries() {
        return entries;
    }
}
