package net.handbook.main.resources.category;

import net.handbook.main.resources.entry.TraderEntry;

import java.util.List;

public class TraderCategory extends BaseCategory {

    final String type;
    final List<TraderEntry> entries;

    public TraderCategory(String title, String text, String image, List<TraderEntry> entries) {
        super("trader", title, text, image, entries);
        this.type = "trader";
        this.entries = entries;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public List<TraderEntry> getEntries() {
        return entries;
    }
}
