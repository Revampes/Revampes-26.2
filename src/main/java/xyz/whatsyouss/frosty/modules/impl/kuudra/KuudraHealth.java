package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.hud.HudManager;
import xyz.whatsyouss.frosty.hud.impl.KuudraHealthWidget;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraBossInfo;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraRenderUtil;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;

import java.awt.Color;
import java.util.Locale;

/**
 * Ported from IQAddons (features/kuudra/miscellaneous/KuudraHealthFeature) plus its
 * "Kuudra Health" HUD widget.
 */
public class KuudraHealth extends Module {

    private final ColorSetting highColor = new ColorSetting("High Color", new Color(85, 255, 85, 255));
    private final ColorSetting midColor = new ColorSetting("Mid Color", new Color(255, 255, 85, 255));
    private final ColorSetting lowColor = new ColorSetting("Low Color", new Color(255, 170, 0, 255));
    private final ColorSetting criticalColor = new ColorSetting("Critical Color", new Color(255, 85, 85, 255));
    private final ButtonSetting healthOnly = new ButtonSetting("Health Only", false);

    private final KuudraHealthWidget widget = new KuudraHealthWidget(this);

    public KuudraHealth() {
        super("Kuudra Health", category.Kuudra);
        this.defaultEnabled = true;
        this.registerSetting(highColor);
        this.registerSetting(midColor);
        this.registerSetting(lowColor);
        this.registerSetting(criticalColor);
        this.registerSetting(healthOnly);

        HudManager.register(widget);
    }

    public int getHealthColor(float currentHealth) {
        if (currentHealth > 75_000) return highColor.getRGB();
        if (currentHealth > 50_000) return midColor.getRGB();
        if (currentHealth > 25_000) return lowColor.getRGB();
        return criticalColor.getRGB();
    }

    public boolean isHealthOnly() {
        return healthOnly.isToggled();
    }

    public String formatHealth(KuudraPhase phase, float currentHealth) {
        if (phase == KuudraPhase.BOSS) {
            return String.format(Locale.ROOT, "%.1fM/240M", currentHealth / 100f);
        }

        return String.format(Locale.ROOT, "%,.0f/100.000", currentHealth);
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        KuudraPhase phase = KuudraState.get().phase();
        if (phase == KuudraPhase.NONE) return;

        KuudraBossInfo bossInfo = KuudraState.get().bossInfo();
        if (!bossInfo.isAlive()) return;

        Vec3 position = bossInfo.position().add(0.0, 10.0, 0.0);
        KuudraRenderUtil.drawText(
                event.getMatrix(),
                position,
                formatHealth(phase, bossInfo.currentHealth()),
                0.25f,
                true,
                new Color(getHealthColor(bossInfo.currentHealth()), true)
        );
    }
}
