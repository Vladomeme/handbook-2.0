package net.handbook.main.resources.entry;

import net.handbook.main.resources.waypoint.Waypoint;

public class WaypointEntry extends Entry {

    WaypointEntry[] waypoints;
    final Waypoint waypoint;
    final boolean shouldPause;
    boolean inChain = false;
    final String id;

    public WaypointEntry(String title, String text, Waypoint waypoint, boolean shouldPause, String id) {
        super(title, text, null);
        this.waypoints = new WaypointEntry[]{this};
        this.waypoint = waypoint;
        this.shouldPause = shouldPause;
        this.id = id;
    }

    @Override
    public WaypointEntry[] waypoints() {
        return waypoints;
    }

    @Override
    public String id() {
        return id;
    }

    public Waypoint waypoint() {
        return waypoint;
    }

    public void setChain(WaypointEntry[] waypoints) {
        this.waypoints = waypoints;
    }

    public WaypointEntry markAsChain() {
        inChain = true;
        return this;
    }

    public boolean inChain() {
        return inChain;
    }

    public boolean shouldPause() {
        return shouldPause;
    }
}
