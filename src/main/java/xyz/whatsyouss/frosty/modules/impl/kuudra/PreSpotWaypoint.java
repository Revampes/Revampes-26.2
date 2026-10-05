package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.SupplyPlaceEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraRenderUtil;
import xyz.whatsyouss.frosty.utility.kuudra.PreSpot;
import xyz.whatsyouss.frosty.utility.kuudra.SupplyState;

import java.awt.Color;

/**
 * Ported from IQAddons (features/kuudra/linus/PreSpotWaypointFeature).
 * After the 6th supply is placed, briefly shows the pre-spot stand waypoint.
 */
public class PreSpotWaypoint extends Module {

    private static final long BUILD_START_COUNTDOWN_MS = 6200L;

    private final SupplyState supplyState = SupplyState.get();

    private PreSpot savedPreSpot = null;
    private long countdownEndMs = -1L;

    private final ColorSetting color = new ColorSetting("Color", new Color(255, 215, 0, 220));
    private final SelectSetting style = new SelectSetting("Style", 0, new String[]{"Solid", "Outline", "Both", "None"});

    public PreSpotWaypoint() {
        super("Pre Spot Waypoint", category.Kuudra);
        this.registerSetting(color);
        this.registerSetting(style);
    }

    @Override
    public void onDisable() {
        this.savedPreSpot = null;
        this.countdownEndMs = -1L;
    }

    @EventHandler
    public void onSupplyPlace(SupplyPlaceEvent event) {
        if (this.savedPreSpot == null) {
            this.savedPreSpot = supplyState.getDetectedPreSpot();
        }
        if (event.currentSupply() < 6) return;

        this.countdownEndMs = System.currentTimeMillis() + BUILD_START_COUNTDOWN_MS;
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (this.savedPreSpot == null) return;
        if (this.countdownEndMs < 0 || System.currentTimeMillis() > this.countdownEndMs) return;

        Vec3 pos = switch (this.savedPreSpot) {
            case X -> new Vec3(-106, 79, -113);
            case TRIANGLE -> new Vec3(-98, 79, -113);
            case EQUALS -> new Vec3(-98, 79, -99);
            case SLASH -> new Vec3(-106, 79, -99);
        };

        float half = 0.5f;
        AABB box = new AABB(
                pos.x() - half, pos.y(), pos.z() - half,
                pos.x() + half, pos.y() + 1.0, pos.z() + half
        );

        KuudraRenderUtil.drawStyledBox(event.getMatrix(), box, true,
                color.getColor(), KuudraRenderUtil.parseStyle(style.getOption()));
    }
}
