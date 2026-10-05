package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.EntityDetectorUtil;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraRenderUtil;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;
import xyz.whatsyouss.frosty.utility.kuudra.SupplyPosition;
import xyz.whatsyouss.frosty.utility.kuudra.SupplyState;

import java.awt.Color;
import java.util.List;

/**
 * Ported from IQAddons (features/kuudra/waypoints/SupplyWaypointsFeature).
 * Draws a beam + box on every active supply crate and, optionally, the supply
 * pickup interaction boxes. Supply positions are maintained by KuudraListener.
 */
public class SupplyWaypoints extends Module {

    private static final int BEACON_HEIGHT = 100;

    private final SupplyState supplyState = SupplyState.get();

    private final ColorSetting color = new ColorSetting("Color", new Color(0, 0, 0, 77));
    private final SliderSetting boxSize = new SliderSetting("Box Size", 1.0, 1.0, 3.0, 0.1);
    private final ButtonSetting supplyHitBox = new ButtonSetting("Supply Interaction Box", true);

    public SupplyWaypoints() {
        super("Supply Waypoints", category.Kuudra);
        this.registerSetting(color);
        this.registerSetting(boxSize);
        this.registerSetting(supplyHitBox);
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (KuudraState.get().phase() != KuudraPhase.SUPPLIES) return;

        List<SupplyPosition> supplies = supplyState.getActiveSupplies();
        if (supplies.isEmpty()) return;

        List<Zombie> zombies = supplyHitBox.isToggled()
                ? EntityDetectorUtil.getEntitiesOfType(Zombie.class)
                : List.of();

        Color renderColor = color.getColor();
        double halfBox = boxSize.getInput() / 2.0;
        for (SupplyPosition supply : supplies) {
            Vec3 renderPos = getInterpolatedSupplyPosition(event, supply);
            KuudraRenderUtil.drawStyledWithBeam(event.getMatrix(), new AABB(
                    renderPos.x + 0.5 - halfBox,
                    renderPos.y - 1,
                    renderPos.z + 1.5 - halfBox,
                    renderPos.x + 0.5 + halfBox,
                    renderPos.y,
                    renderPos.z + 1.5 + halfBox
            ), BEACON_HEIGHT, true, renderColor, KuudraRenderUtil.RenderStyle.BOTH);

            if (supplyHitBox.isToggled()) {
                zombies.stream()
                        .filter(zombie -> zombie.distanceToSqr(renderPos) < 9)
                        .forEach(zombie -> KuudraRenderUtil.drawStyledBox(
                                event.getMatrix(),
                                zombie.getBoundingBox(),
                                false, renderColor, KuudraRenderUtil.RenderStyle.BOTH)
                        );
            }
        }
    }

    private Vec3 getInterpolatedSupplyPosition(Render3DEvent event, SupplyPosition supply) {
        if (mc.level == null) return supply.position();

        Entity entity = mc.level.getEntity(supply.entityId());
        if (!(entity instanceof Giant giant)) return supply.position();

        float tickDelta = event.getDelta();
        double x = giant.xo + (giant.getX() - giant.xo) * tickDelta;
        double z = giant.zo + (giant.getZ() - giant.zo) * tickDelta;

        return SupplyPosition.fromGiant(x, z, giant.getYRot(), giant.getId()).position();
    }
}
