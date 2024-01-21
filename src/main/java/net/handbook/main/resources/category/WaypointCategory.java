package net.handbook.main.resources.category;

import net.handbook.main.resources.entry.WaypointEntry;

import java.util.List;

public class WaypointCategory extends BaseCategory {

    final String type;
    final List<WaypointEntry> entries;

    public WaypointCategory(String title, String text, String image, List<WaypointEntry> entries) {
        super("waypoint", title, text, image, entries);
        this.type = "waypoint";
        this.entries = entries;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public List<WaypointEntry> getEntries() {
        return entries;
    }
}
