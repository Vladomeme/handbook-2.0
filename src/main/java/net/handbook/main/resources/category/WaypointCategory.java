package net.handbook.main.resources.category;

import net.handbook.main.resources.entry.WaypointEntry;
import net.handbook.main.resources.waypoint.Waypoint;

import java.util.ArrayList;
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

    public List<Waypoint> getWaypoints() {
        List<Waypoint> waypoints = new ArrayList<>();
        entries.forEach(entry -> waypoints.add(entry.getWaypoint()));
        return waypoints;
    }
}
