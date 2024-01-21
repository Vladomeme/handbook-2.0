package net.handbook.main.resources.entry;

import net.handbook.main.resources.waypoint.Waypoint;

public class WaypointEntry extends Entry {

    WaypointChain waypoints;
    Waypoint waypoint;
    boolean pause;
    boolean chain = false;
    final String id;

    public WaypointEntry(String title, String text, Waypoint waypoint, boolean pause, String id) {
        super(title, text, null);
        this.waypoints = new WaypointChain(new WaypointEntry[]{this});
        this.waypoint = waypoint;
        this.pause = pause;
        this.id = id;
    }

    public Waypoint getWaypoint() {
        return waypoint;
    }

    public void setChain(WaypointChain waypoints) {
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
    @Override
    public WaypointEntry[] getWaypoints() {
        return waypoints.getWaypoints();
    }

    @Override
    public String getID() {
        return id;
    }
}
