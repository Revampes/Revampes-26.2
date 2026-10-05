package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.core.particles.ParticleOptions;
import xyz.whatsyouss.frosty.events.Cancellable;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.ParticleEvent).
 * Posted from {@code ClientLevel#addParticle}; cancelling hides the particle.
 */
public class ParticleEvent extends Cancellable {

    private final ParticleOptions type;
    private final double x, y, z;

    public ParticleEvent(ParticleOptions type, double x, double y, double z) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public ParticleOptions getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}
