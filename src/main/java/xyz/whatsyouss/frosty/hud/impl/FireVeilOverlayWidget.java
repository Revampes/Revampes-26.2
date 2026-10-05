package xyz.whatsyouss.frosty.hud.impl;

import xyz.whatsyouss.frosty.hud.HudWidget;
import xyz.whatsyouss.frosty.modules.impl.kuudra.FireVeilOverlay;
import xyz.whatsyouss.frosty.utility.kuudra.FireVeilOverlayManager;

import java.util.List;
import java.util.Locale;

/**
 * Ported from IQAddons (features/widgets/FireVeilOverlayWidget).
 */
public class FireVeilOverlayWidget extends HudWidget {

    private final FireVeilOverlayManager manager = FireVeilOverlayManager.get();
    private final FireVeilOverlay module;

    private String countdown = "§b§lFire Veil: §a§l5.00s";

    public FireVeilOverlayWidget(FireVeilOverlay module) {
        super("fireVeilOverlay", "Fire Veil Countdown", 424.0f, 278.0f, 1.2f);
        this.module = module;
    }

    @Override
    public boolean isVisible() {
        return module.isEnabled()
                && module.isCountdownEnabled()
                && manager.shouldDisplayCountdown(System.currentTimeMillis());
    }

    @Override
    public List<String> lines() {
        return List.of(countdown);
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();

        if (manager.isReadyVisible(now)) {
            countdown = "§b§lFire Veil: §a§lREADY";
            return;
        }

        long remainingMs = manager.getRemainingMs(now);
        countdown = String.format(
                Locale.ROOT,
                "§b§lFire Veil: %s§l%.2fs",
                getCountdownColor(remainingMs),
                remainingMs / 1000.0
        );
    }

    private String getCountdownColor(long remainingMs) {
        double ratio = Math.min(1.0, Math.max(0.0, remainingMs / (double) FireVeilOverlayManager.ABILITY_DURATION_MS));
        if (ratio > 0.75) return "§a";
        if (ratio > 0.50) return "§e";
        if (ratio > 0.25) return "§6";
        return "§c";
    }
}
