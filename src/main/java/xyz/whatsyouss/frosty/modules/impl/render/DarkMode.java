package xyz.whatsyouss.frosty.modules.impl.render;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.Render2DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

public class DarkMode extends Module {

    private final SliderSetting opacity =
            new SliderSetting("Opacity", 150, 0, 255, 1);

    public DarkMode() {
        super("Dark Mode", "暗黑模式", category.Render);
        registerSetting(opacity);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!Utils.nullCheck()) return;

        int width  = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        int alpha = (int) opacity.getInput();
        if (alpha < 0)   alpha = 0;
        if (alpha > 255) alpha = 255;

        int color = (alpha << 24);

        RenderUtils.rect(event.drawContext, 0, 0, width, height, color);
    }
}