package net.handbook.main.resources.category;

import net.handbook.main.resources.entry.AreaEntry;

import java.util.List;

public class AreaCategory extends BaseCategory {

    final String type;
    final List<AreaEntry> entries;

    public AreaCategory(String title, String text, String image, List<AreaEntry> entries) {
        super("area", title, text, image, entries);
        this.type = "area";
        this.entries = entries;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public List<AreaEntry> getEntries() {
        return entries;
    }
}
