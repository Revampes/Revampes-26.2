package xyz.whatsyouss.frosty.hud.impl;

import net.minecraft.client.gui.Font;
import xyz.whatsyouss.frosty.hud.HudWidget;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.kuudra.KuudraHealth;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraBossInfo;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ported from IQAddons (features/widgets/KuudraHealthWidget).
 */
public class KuudraHealthWidget extends HudWidget {

    private static final float BOSS_PHASE_MAX_HEALTH = 24_000f;
    private static final float PRE_BOSS_MIN_HEALTH = 25_000f;
    private static final float PRE_BOSS_MAX_HEALTH = 100_000f;

    private static final String RED = "§c";
    private static final String DARK_RED = "§4";
    private static final String DARK_GRAY = "§8";
    private static final String BOLD = "§l";
    private static final String HEART = "\u2764";

    private final KuudraHealth module;

    private String healthText = String.format(Locale.ROOT, "%s%s 100,000 %s(%s100.0%%%s)",
            RED, HEART, DARK_GRAY, RED, DARK_GRAY);

    public KuudraHealthWidget(KuudraHealth module) {
        super("kuudraHealth", "Kuudra Health", 439.0f, 20.0f, 1.0f);
        this.module = module;
    }

    @Override
    public boolean isVisible() {
        KuudraPhase phase = KuudraState.get().phase();
        return module.isEnabled() && KuudraPhase.COMBAT_PHASES.contains(phase);
    }

    @Override
    public List<String> lines() {
        List<String> lines = new ArrayList<>();
        if (!module.isHealthOnly()) {
            lines.add(DARK_RED + BOLD + "Kuudra Health");
        }
        lines.add(healthText);
        return lines;
    }

    @Override
    public void tick() {
        KuudraPhase phase = KuudraState.get().phase();
        KuudraBossInfo bossInfo = KuudraState.get().bossInfo();
        if (!bossInfo.isAlive()) return;

        float currentHealth = bossInfo.currentHealth();
        double percentage = getHealthPercentage(phase, currentHealth, bossInfo);
        String percentageColor = getPercentageColor(percentage);

        String healthValue = phase == KuudraPhase.BOSS
                ? String.format(Locale.ROOT, "%.1fM", currentHealth / 100f)
                : String.format(Locale.ROOT, "%,.0f", currentHealth);

        healthText = String.format(Locale.ROOT, "%s%s %s %s(%s%.1f%%%s)",
                RED, HEART, healthValue, DARK_GRAY, percentageColor, percentage, DARK_GRAY);
    }

    private double getHealthPercentage(KuudraPhase phase, float currentHealth, KuudraBossInfo bossInfo) {
        if (phase == KuudraPhase.BOSS) {
            return clampPercentage((currentHealth / BOSS_PHASE_MAX_HEALTH) * 100.0);
        }

        if (phase == KuudraPhase.STUN || phase == KuudraPhase.DPS || phase == KuudraPhase.SKIP) {
            float normalizedHealth = currentHealth - PRE_BOSS_MIN_HEALTH;
            float preBossRange = PRE_BOSS_MAX_HEALTH - PRE_BOSS_MIN_HEALTH;
            return clampPercentage((normalizedHealth / preBossRange) * 100.0);
        }

        if (bossInfo.maxHealth() <= 0f) return 0.0;
        return clampPercentage((currentHealth / bossInfo.maxHealth()) * 100.0);
    }

    private double clampPercentage(double value) {
        return Math.min(100.0, Math.max(0.0, value));
    }

    private String getPercentageColor(double percentage) {
        if (percentage >= 75.0) return "§a";
        if (percentage >= 50.0) return "§e";
        if (percentage >= 25.0) return "§6";
        return "§c";
    }

    @Override
    public void applyDefaultPosition(Font font) {
        int width = mc.getWindow().getGuiScaledWidth();
        setPosition(Math.max(0.0f, width - 78.0f), 20.0f);
        setScale(1.0f);
    }
}
