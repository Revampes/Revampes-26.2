package xyz.revampes.Revamp.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.revampes.Revamp.events.impl.PreUpdateEvent;
import xyz.revampes.Revamp.events.impl.Render3DEvent;
import xyz.revampes.Revamp.events.impl.kuudra.KuudraPhaseChangeEvent;
import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.settings.impl.ButtonSetting;
import xyz.revampes.Revamp.settings.impl.ColorSetting;
import xyz.revampes.Revamp.utility.EntityDetectorUtil;
import xyz.revampes.Revamp.utility.kuudra.KuudraPhase;
import xyz.revampes.Revamp.utility.kuudra.KuudraRenderUtil;
import xyz.revampes.Revamp.utility.kuudra.KuudraState;
import xyz.revampes.Revamp.utility.kuudra.PileLocation;
import xyz.revampes.Revamp.utility.kuudra.SupplyState;

import java.awt.Color;
import java.util.List;

/**
 * Ported from IQAddons (features/kuudra/waypoints/PileWaypointsFeature).
 * Marks each remaining supply pile with a beam and optional name label during the
 * supplies phase, removing piles once their "SUPPLIES RECEIVED" stand appears.
 */
public class PileWaypoints extends Module {

    private static final int UPDATE_INTERVAL_TICKS = 5;
    private static final int BEACON_HEIGHT = 40;

    private final SupplyState supplyState = SupplyState.get();

    private final ButtonSetting names = new ButtonSetting("Names", true);
    private final ColorSetting normalColor = new ColorSetting("Normal Color", new Color(255, 255, 255, 52));
    private final ColorSetting noPreColor = new ColorSetting("No Pre Color", new Color(0, 255, 144, 50));

    private int tickCounter = 0;

    public PileWaypoints() {
        super("Pile Waypoints", category.Kuudra);
        this.registerSetting(names);
        this.registerSetting(normalColor);
        this.registerSetting(noPreColor);
    }

    @Override
    public void onEnable() {
        this.tickCounter = 0;
        supplyState.reset();
    }

    @Override
    public void onDisable() {
        supplyState.getRemainingPiles().clear();
    }

    @EventHandler
    public void onPhaseChange(KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            supplyState.reset();
        }
    }

    @EventHandler
    public void onTick(PreUpdateEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (KuudraState.get().phase() != KuudraPhase.SUPPLIES) return;

        tickCounter++;
        if (tickCounter % UPDATE_INTERVAL_TICKS != 0) return;

        List<ArmorStand> completedStands = EntityDetectorUtil.getCompletedPileStands();
        for (ArmorStand stand : completedStands) {
            Vec3 standPos = new Vec3(stand.getX(), stand.getY(), stand.getZ());
            supplyState.markPileCompleted(standPos);
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (KuudraState.get().phase() != KuudraPhase.SUPPLIES) return;

        List<PileLocation> piles = supplyState.getRemainingPiles();
        if (piles.isEmpty()) return;

        int missingPre = supplyState.getMissingPre();
        for (PileLocation pile : piles) {
            Color color = pile.isNoPrePile(missingPre) ? noPreColor.getColor() : normalColor.getColor();

            KuudraRenderUtil.drawStyledWithBeam(
                    event.getMatrix(),
                    AABB.unitCubeFromLowerCorner(pile.position()),
                    BEACON_HEIGHT, false, color, KuudraRenderUtil.RenderStyle.BOTH
            );

            if (names.isToggled()) {
                KuudraRenderUtil.drawText(event.getMatrix(), pile.position().add(0, 2.5, 0),
                        pile.name(), 0.05f, true, KuudraRenderUtil.withOpacity(color, 100));
            }
        }
    }
}
