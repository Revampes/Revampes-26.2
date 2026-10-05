package xyz.whatsyouss.frosty.utility.pearl;

import net.minecraft.world.phys.Vec3;

import java.awt.Color;

/**
 * Ported from IQAddons (net.iqaddons.mod.model.pearl.PearlWaypoint).
 * Uses {@link Color} instead of the IQAddons RenderColor type.
 *
 * <p>Every waypoint now carries the throw family it belongs to
 * ({@link PearlTrajectoryType}), the block the pearl is thrown from
 * ({@code standBlock}) and the block the trajectory is solved towards
 * ({@code aimTarget}). The aim/timer is no longer a heuristic offset: the module
 * solves the actual projectile arc with {@code PearlTrajectorySolver}.
 */
public record PearlWaypoint(
        Vec3 target,
        Color color,
        Vec3 standBlock,
        Vec3 aimTarget,
        PearlTrajectoryType trajectoryType,
        double projectionDistance,
        Integer landingTick,
        double landingOffsetTicks,
        Integer preSupply,
        Integer hideForPre,
        float size,
        String label,
        boolean alert
) {

    public static final float DEFAULT_SIZE = 0.4f;

    public boolean shouldShow(int missingPre) {
        if (hideForPre != null && hideForPre == missingPre) {
            return false;
        }

        if (preSupply != null && missingPre > 0) {
            return preSupply == missingPre;
        }

        return true;
    }

    public boolean hasStandBlock() {
        return standBlock != null && !(standBlock.x == 0 && standBlock.y == 0 && standBlock.z == 0);
    }

    public boolean usesTrajectoryProjection() {
        return aimTarget != null && !(aimTarget.x == 0 && aimTarget.y == 0 && aimTarget.z == 0);
    }
}
