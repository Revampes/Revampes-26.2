package xyz.revampes.Revamp.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import xyz.revampes.Revamp.events.impl.kuudra.KuudraPhaseChangeEvent;
import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.utility.MessageUtil;
import xyz.revampes.Revamp.utility.kuudra.KuudraPhase;

/**
 * Ported from IQAddons (features/kuudra/alerts/KuudraPhaseAlertFeature).
 */
public class KuudraPhaseAlert extends Module {

    public KuudraPhaseAlert() {
        super("Kuudra Phase Alert", category.Kuudra);
    }

    @EventHandler
    public void onPhaseChange(KuudraPhaseChangeEvent event) {
        KuudraPhase currentPhase = event.currentPhase();
        if (!currentPhase.isActive()) return;

        MessageUtil.showAlert("§6§l" + currentPhase.getDisplayName(), 25);
    }
}
