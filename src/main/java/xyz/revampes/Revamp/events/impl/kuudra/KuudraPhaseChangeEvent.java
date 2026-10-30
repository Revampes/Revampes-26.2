package xyz.revampes.Revamp.events.impl.kuudra;

import xyz.revampes.Revamp.utility.kuudra.KuudraPhase;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent).
 */
public record KuudraPhaseChangeEvent(
        KuudraPhase previousPhase,
        KuudraPhase currentPhase
) {
    public boolean isEnteringKuudra() {
        return !previousPhase.isInRun() && currentPhase.isInRun();
    }
}
