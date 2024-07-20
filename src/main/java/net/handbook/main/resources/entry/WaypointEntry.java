package net.handbook.main.resources.entry;

import net.handbook.main.resources.waypoint.Waypoint;

public class WaypointEntry extends Entry {

    WaypointEntry[] waypoints;
    final Waypoint waypoint;
    final boolean pause;
    boolean chain = false;
    final String id;

    public WaypointEntry(String title, String text, Waypoint waypoint, boolean pause, String id) {
        super(title, text, null);
        this.waypoints = new WaypointEntry[]{this};
        this.waypoint = waypoint;
        this.pause = pause;
        this.id = id;
    }

    @Override
    public WaypointEntry[] getWaypoints() {
        return waypoints;
    }

    @Override
    public String getID() {
        return id;
    }

    public Waypoint getWaypoint() {
        return waypoint;
    }

    public void setChain(WaypointEntry[] waypoints) {
        this.waypoints = waypoints;
    }

    public WaypointEntry markAsChain() {
        chain = true;
        return this;
    }

    public boolean inChain() {
        return chain;
    }

    public boolean shouldPause() {
        return pause;
    }
}
