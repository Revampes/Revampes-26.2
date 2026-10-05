package xyz.whatsyouss.frosty.utility.pearl;

import java.util.List;

/**
 * Ported from IQAddons (net.iqaddons.mod.model.pearl.WaypointArea).
 */
public record WaypointArea(
        String name,
        BoundingBox2D bounds,
        List<PearlWaypoint> waypoints,
        Boolean invertForwardBackward,
        Boolean invertLeftRight
) {
    public WaypointArea {
        waypoints = List.copyOf(waypoints);
    }

    public boolean containsPlayer(double x, double z) {
        return bounds.contains(x, z);
    }
}
