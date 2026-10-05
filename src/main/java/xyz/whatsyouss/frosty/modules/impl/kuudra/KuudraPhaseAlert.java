package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.kuudra.KuudraPhaseChangeEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.MessageUtil;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;

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
