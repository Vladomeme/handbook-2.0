package net.handbook.main.resources.entry;

import net.handbook.main.resources.waypoint.Waypoint;

public class WaypointEntry extends Entry {

    final Waypoint waypoint;

    public WaypointEntry(String title, String text, Waypoint waypoint) {
        super(title, text, null);
        this.waypoint = waypoint;
    }

    @Override
    public Waypoint getWaypoint() {
        return waypoint;
    }
}
