package net.handbook.main.resources.category;

import net.handbook.main.resources.entry.PositionedEntry;

import java.util.List;

public class PositionedCategory extends BaseCategory {

    final String type;
    final List<PositionedEntry> entries;

    public PositionedCategory(String title, String text, String image, List<PositionedEntry> entries) {
        super("positioned", title, text, image, entries);
        this.type = "positioned";
        this.entries = entries;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public List<PositionedEntry> getEntries() {
        return entries;
    }
}
