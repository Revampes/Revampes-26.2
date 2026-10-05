package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraBossInfo;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraRenderUtil;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;

import java.awt.Color;

/**
 * Ported from IQAddons (features/kuudra/miscellaneous/KuudraHitboxFeature).
 */
public class KuudraHitbox extends Module {

    private final ColorSetting color = new ColorSetting("Color", "颜色", new Color(255, 85, 85, 160));
    private final SelectSetting style = new SelectSetting("Style", 2, new String[]{"Solid", "Outline", "Both", "None"});

    public KuudraHitbox() {
        super("Kuudra Hitbox", category.Kuudra);
        this.defaultEnabled = true;
        this.registerSetting(color);
        this.registerSetting(style);
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (KuudraState.get().phase() == KuudraPhase.NONE) return;

        KuudraBossInfo bossInfo = KuudraState.get().bossInfo();
        if (!bossInfo.isAlive()) return;

        KuudraRenderUtil.RenderStyle renderStyle = KuudraRenderUtil.parseStyle(style.getOption());
        if (renderStyle == KuudraRenderUtil.RenderStyle.NONE) return;

        KuudraRenderUtil.drawStyledHitbox(
                event.getMatrix(),
                bossInfo.bossEntity(),
                event.getDelta(),
                true,
                color.getColor(),
                renderStyle
        );
    }
}
