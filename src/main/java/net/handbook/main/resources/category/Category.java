package net.handbook.main.resources.category;

import net.handbook.main.resources.entry.Entry;

import java.util.List;

public class Category extends BaseCategory {

    final String type;
    final List<Entry> entries;

    public Category(String title, String text, String image, List<Entry> entries) {
        super("normal", title, text, image, entries);
        this.type = "normal";
        this.entries = entries;
    }
    @Override
    public String getType() {
        return type;
    }

    @Override
    public List<Entry> getEntries() {
        return entries;
    }

}
