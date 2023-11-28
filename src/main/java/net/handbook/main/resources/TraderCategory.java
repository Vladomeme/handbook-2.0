package net.handbook.main.resources;

import net.minecraft.util.Identifier;

import java.util.List;

public class TraderCategory extends BaseCategory {

    final String type;
    final List<TraderEntry> entries;

    public TraderCategory(String title, String text, Identifier image, List<TraderEntry> entries) {
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
