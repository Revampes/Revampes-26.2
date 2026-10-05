package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraRenderUtil;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;

import java.awt.Color;
import java.util.List;

/**
 * Ported from IQAddons ("DPS Waypoints" etherwarp category, features/kuudra/waypoints/DpsWaypointFeature).
 *
 * <p>The EATEN phase has two groups: the three agro stand spots (outline) and the four
 * DPS main stand spots (solid fill). Positions, alpha and render styles are the ones in
 * IQ's {@code etherwarp_config.json}; all of them carry a 100 block render distance.
 */
public class DpsWaypoints extends Module {

    private static final double MAX_RENDER_DISTANCE = 100.0;

    /** "DPS Agro (Close / Mid / Long)" - OUTLINE, alpha 0.8. */
    private static final List<Vec3> AGRO_POSITIONS = List.of(
            new Vec3(-88.5, 78, -78.5),
            new Vec3(-84.5, 78, -85.5),
            new Vec3(-82.5, 78, -92.5)
    );

    /** "DPS Main (Top / Top / Bottom / Bottom)" - SOLID, alpha 0.4. */
    private static final List<Vec3> MAIN_POSITIONS = List.of(
            new Vec3(-110.5, 78, -71.5),
            new Vec3(-110.5, 78, -72.5),
            new Vec3(-113.5, 76, -68.5),
            new Vec3(-113.5, 76, -69.5)
    );

    private final ColorSetting color = new ColorSetting("Color", new Color(0, 245, 255, 255));
    private final SelectSetting style = new SelectSetting("Agro Style", 1, new String[]{"Solid", "Outline", "Both", "None"});
    private final SelectSetting mainStyle = new SelectSetting("Main Style", 0, new String[]{"Solid", "Outline", "Both", "None"});

    public DpsWaypoints() {
        super("Dps Waypoints", category.Kuudra);
        this.registerSetting(color);
        this.registerSetting(style);
        this.registerSetting(mainStyle);
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (KuudraState.get().phase() != KuudraPhase.EATEN) return;
        if (mc.player == null) return;

        renderGroup(event, AGRO_POSITIONS, 0.8f, KuudraRenderUtil.parseStyle(style.getOption()));
        renderGroup(event, MAIN_POSITIONS, 0.4f, KuudraRenderUtil.parseStyle(mainStyle.getOption()));
    }

    private void renderGroup(Render3DEvent event, List<Vec3> positions, float alpha, KuudraRenderUtil.RenderStyle renderStyle) {
        if (renderStyle == KuudraRenderUtil.RenderStyle.NONE) return;

        Color renderColor = KuudraRenderUtil.withOpacity(color.getColor(), alpha);
        Vec3 playerPos = mc.player.position();

        for (Vec3 position : positions) {
            if (playerPos.distanceTo(position) > MAX_RENDER_DISTANCE) continue;

            KuudraRenderUtil.drawStyledBox(event.getMatrix(), blockBox(position), true, renderColor, renderStyle);
        }
    }

    private static AABB blockBox(Vec3 center) {
        return new AABB(
                center.x - 0.5, center.y, center.z - 0.5,
                center.x + 0.5, center.y + 1.0, center.z + 0.5
        );
    }
}
