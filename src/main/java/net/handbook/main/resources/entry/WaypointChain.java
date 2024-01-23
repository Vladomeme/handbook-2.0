package net.handbook.main.resources.entry;

public class WaypointChain extends Entry {

    final WaypointEntry[] waypoints;

    public WaypointChain(WaypointEntry[] waypoints) {
        super(null, null, null);
        this.waypoints = waypoints;
    }

    @Override
    public WaypointEntry[] getWaypoints() {
        return waypoints;
    }
}
