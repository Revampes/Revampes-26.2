package xyz.revampes.Revamp.events.impl.kuudra;

import xyz.revampes.Revamp.utility.kuudra.KuudraPhase;
import xyz.revampes.Revamp.utility.kuudra.KuudraTier;

import java.time.Duration;
import java.util.Map;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent).
 */
public record KuudraRunEndEvent(
        EndReason reason,
        KuudraTier tier,
        Duration totalDuration,
        Map<KuudraPhase, Duration> phaseDurations
) {

    public Duration getPhase(KuudraPhase phase) {
        return phaseDurations.getOrDefault(phase, Duration.ZERO);
    }

    public boolean isCompleted() {
        return reason == EndReason.COMPLETED;
    }

    public boolean isFailed() {
        return reason == EndReason.DEFEATED;
    }

    public boolean isUnexpectedlyEnded() {
        return reason == EndReason.DISCONNECTED || reason == EndReason.OTHER;
    }

    public enum EndReason {
        COMPLETED,
        DEFEATED,
        DISCONNECTED,
        OTHER
    }
}
