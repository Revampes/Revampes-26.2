package xyz.whatsyouss.frosty.utility.pearl;

import net.minecraft.client.Minecraft;
import xyz.whatsyouss.frosty.Frosty;

import java.util.List;

/**
 * Ported from IQAddons (net.iqaddons.mod.utils.AreaDetectionUtil).
 * Tracks which pearl waypoint area contains the player. The area is only recomputed
 * when the player leaves the current one, so the selected stand block / timer source
 * stays stable inside an area.
 */
public class AreaDetectionUtil {

    private final Minecraft mc = Minecraft.getInstance();

    private volatile List<WaypointArea> areas = List.of();
    private volatile WaypointArea currentArea = null;

    public void setAreas(List<WaypointArea> areas) {
        this.areas = areas;
        this.currentArea = null;
        Frosty.LOGGER.debug("[Pearl] loaded {} waypoint area(s)", areas.size());
    }

    public void update() {
        if (mc.player == null) {
            currentArea = null;
            return;
        }

        double x = mc.player.getX();
        double z = mc.player.getZ();
        if (currentArea != null && currentArea.containsPlayer(x, z)) {
            return;
        }

        WaypointArea previous = currentArea;
        currentArea = findAreaContaining(x, z);
        if (previous != currentArea) {
            String from = previous != null ? previous.name() : "none";
            String to = currentArea != null ? currentArea.name() : "none";
            Frosty.LOGGER.debug("[Pearl] area: {} -> {}", from, to);
        }
    }

    public WaypointArea getCurrentArea() {
        return currentArea;
    }

    public boolean isInArea() {
        return currentArea != null;
    }

    public void reset() {
        currentArea = null;
    }

    private WaypointArea findAreaContaining(double x, double z) {
        for (WaypointArea area : areas) {
            if (area.containsPlayer(x, z)) {
                return area;
            }
        }
        return null;
    }
}
