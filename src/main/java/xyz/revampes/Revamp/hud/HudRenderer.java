package xyz.revampes.Revamp.hud;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import xyz.revampes.Revamp.events.impl.PreUpdateEvent;
import xyz.revampes.Revamp.events.impl.Render2DEvent;
import xyz.revampes.Revamp.utility.Utils;

/**
 * Draws every registered {@link HudWidget} on the in-game HUD and keeps them alive
 * independently of any module being toggled.
 */
public class HudRenderer {

    private static final Minecraft mc = Minecraft.getInstance();

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!Utils.nullCheck()) return;
        if (mc.gui.screen() instanceof HudEditorScreen) return;
        if (mc.gui.hud.isHidden()) return;

        HudManager.load();
        HudManager.render(event.drawContext, mc.font);
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return;
        HudManager.tick();
    }
}
