package xyz.whatsyouss.frosty.utility.pearl;

import net.minecraft.world.phys.Vec3;

/**
 * Ported from IQAddons (net.iqaddons.mod.model.pearl.PearlAimSolution).
 * Normalized throw direction plus the solved flight time in ticks.
 */
public record PearlAimSolution(
        Vec3 direction,
        double flightTicks
) {
}
