package xyz.whatsyouss.frosty.utility.kuudra;

import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ported from IQAddons (net.iqaddons.mod.manager.SupplyStateManager), covering the
 * state the Kuudra waypoint/alert modules consume (active supplies, remaining piles,
 * detected pre spot, missing pre, progress and phase timing).
 */
public final class SupplyState {

    private static final SupplyState INSTANCE = new SupplyState();

    private final List<SupplyPosition> activeSupplies = new CopyOnWriteArrayList<>();
    private final List<PileLocation> remainingPiles = new CopyOnWriteArrayList<>(PileLocation.DEFAULT_PILES);

    private volatile PreSpot detectedPreSpot = null;
    private volatile boolean preSpotLocked = false;
    private volatile long suppliesPhaseStart = -1L;
    private volatile Long lastNoPreCheckAtMillis = null;
    private volatile int missingPre = 0;
    private volatile int suppliesCollected = 0;
    private volatile int currentSupplyProgress = 0;

    private SupplyState() {
    }

    public static SupplyState get() {
        return INSTANCE;
    }

    public void startSuppliesPhase() {
        suppliesPhaseStart = System.currentTimeMillis();
    }

    public void updateSupplyPositions(List<SupplyPosition> positions) {
        activeSupplies.clear();
        activeSupplies.addAll(positions);
    }

    public void markNoPreCheckCompleted() {
        lastNoPreCheckAtMillis = System.currentTimeMillis();
    }

    public Long getLastNoPreCheckAtMillis() {
        return lastNoPreCheckAtMillis;
    }

    public List<SupplyPosition> getActiveSupplies() {
        return Collections.unmodifiableList(activeSupplies);
    }

    public List<PileLocation> getRemainingPiles() {
        return remainingPiles;
    }

    public PreSpot getDetectedPreSpot() {
        return detectedPreSpot;
    }

    public boolean tryDetectPreSpot(Vec3 playerPos) {
        if (preSpotLocked) return false;

        PreSpot detected = PreSpot.fromPlayerPosition(playerPos);
        if (detected != null) {
            detectedPreSpot = detected;
            preSpotLocked = true;
            return true;
        }

        return false;
    }

    public boolean hasPreSupply() {
        if (detectedPreSpot == null) return false;

        Vec3 preLoc = detectedPreSpot.getLocation();
        double radius = 18.0;

        return activeSupplies.stream()
                .anyMatch(supply -> supply.isNear(preLoc, radius));
    }

    public Boolean hasSecondarySupply() {
        if (detectedPreSpot == null || !detectedPreSpot.hasSecondaryLocation()) {
            return null;
        }

        Vec3 secondaryLoc = detectedPreSpot.getSecondaryLocation();
        double radius = detectedPreSpot.getSecondaryCheckRadius();

        return activeSupplies.stream()
                .anyMatch(supply -> supply.isNear(secondaryLoc, radius));
    }

    public SupplyPosition findSupplyNear(Vec3 location, double radius) {
        return activeSupplies.stream()
                .filter(supply -> supply.isNear(location, radius))
                .findFirst()
                .orElse(null);
    }

    public void markPileCompleted(Vec3 armorStandPos) {
        remainingPiles.removeIf(pile -> pile.isNearby(armorStandPos));
    }

    public int getMissingPre() {
        return missingPre;
    }

    public void setMissingPre(int value) {
        missingPre = value;
    }

    public int getCurrentSupplyProgress() {
        return currentSupplyProgress;
    }

    public void setSupplyProgress(int value) {
        if (value - currentSupplyProgress > 15) return;
        if (value != currentSupplyProgress) {
            currentSupplyProgress = value;
        }
        if (value == 100) currentSupplyProgress = 0;
    }

    public long getElapsedTimeMillis() {
        if (suppliesPhaseStart < 0) return 0;
        return System.currentTimeMillis() - suppliesPhaseStart;
    }

    public void reset() {
        activeSupplies.clear();
        remainingPiles.clear();
        remainingPiles.addAll(PileLocation.DEFAULT_PILES);

        detectedPreSpot = null;
        preSpotLocked = false;
        suppliesPhaseStart = -1L;
        lastNoPreCheckAtMillis = null;

        missingPre = 0;
        suppliesCollected = 0;
        currentSupplyProgress = 0;
    }
}
