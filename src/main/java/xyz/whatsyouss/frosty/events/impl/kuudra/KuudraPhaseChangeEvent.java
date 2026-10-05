package xyz.whatsyouss.frosty.events.impl.kuudra;

import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;

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
