package xyz.whatsyouss.frosty.events.impl.kuudra;

import xyz.whatsyouss.frosty.utility.kuudra.PreSpot;
import xyz.whatsyouss.frosty.utility.kuudra.SupplyPosition;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.skyblock.supply.SupplyProgressEvent),
 * posted from the supply progress title/subtitle and read by pearl / pile waypoints.
 */
public class SupplyProgressEvent {

    private final SupplyPosition position;
    private final PreSpot spot;
    private final String progressText;
    private final int currentProgress;

    private boolean cancelled;

    public SupplyProgressEvent(SupplyPosition position, PreSpot spot, String progressText, int currentProgress) {
        this.position = position;
        this.spot = spot;
        this.progressText = progressText;
        this.currentProgress = currentProgress;
    }

    public SupplyPosition getPosition() {
        return position;
    }

    public PreSpot getSpot() {
        return spot;
    }

    public String getProgressText() {
        return progressText;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
