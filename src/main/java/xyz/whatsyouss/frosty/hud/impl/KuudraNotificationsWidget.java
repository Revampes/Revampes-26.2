package xyz.whatsyouss.frosty.hud.impl;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.joml.Matrix3x2fStack;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.HudNotificationEvent;
import xyz.whatsyouss.frosty.hud.HudWidget;

import java.util.List;

import static xyz.whatsyouss.frosty.Frosty.mc;

/**
 * Ported from IQAddons (features/widgets/KuudraNotificationsWidget).
 * Centred, fading on-screen notification used by the Kuudra phase alerts.
 */
public class KuudraNotificationsWidget extends HudWidget {

    private static final int FADE_IN_TICKS = 4;
    private static final int FADE_OUT_TICKS = 4;
    private static final String EXAMPLE_TEXT = "§e§lNOTIFICATIONS";

    private FadeState fadeState = FadeState.HIDDEN;
    private int displayTicksRemaining;
    private int fadeTicksRemaining;
    private String text = "";

    public KuudraNotificationsWidget() {
        super("kuudraNotifications", "Kuudra Notifications", 0.0f, 96.0f, 3.0f);
        Frosty.EVENT_BUS.subscribe(this);
    }

    @Override
    public void applyDefaultPosition(Font font) {
        setPosition((mc.getWindow().getGuiScaledWidth() - font.width(EXAMPLE_TEXT)) / 2.0f, 96.0f);
        setScale(3.0f);
    }

    @EventHandler
    private void onNotification(HudNotificationEvent event) {
        text = event.text();
        displayTicksRemaining = Math.max(event.durationTicks(), 1);
        fadeTicksRemaining = FADE_IN_TICKS;
        fadeState = FadeState.FADING_IN;

        if (mc.player != null) {
            mc.player.playSound(
                    event.soundEvent() != null ? event.soundEvent() : SoundEvents.NOTE_BLOCK_PLING.value(),
                    2.0f,
                    1.0f
            );
        }
    }

    @Override
    public void tick() {
        switch (fadeState) {
            case FADING_IN -> {
                if (fadeTicksRemaining > 0) fadeTicksRemaining--;
                if (fadeTicksRemaining <= 0) {
                    fadeState = FadeState.VISIBLE;
                    fadeTicksRemaining = 0;
                }
            }
            case VISIBLE -> {
                if (displayTicksRemaining > 0) displayTicksRemaining--;
                if (displayTicksRemaining <= 0) {
                    fadeState = FadeState.FADING_OUT;
                    fadeTicksRemaining = FADE_OUT_TICKS;
                }
            }
            case FADING_OUT -> {
                if (fadeTicksRemaining > 0) fadeTicksRemaining--;
                if (fadeTicksRemaining <= 0) reset();
            }
            case HIDDEN -> {
            }
        }
    }

    @Override
    public boolean isVisible() {
        return fadeState != FadeState.HIDDEN && !text.isEmpty();
    }

    @Override
    public List<String> lines() {
        return List.of(text.isEmpty() ? EXAMPLE_TEXT : text);
    }

    @Override
    public void render(GuiGraphicsExtractor context, Font font, float alpha) {
        if (!isVisible()) return;

        int alphaByte = Math.max(0, Math.min(255, (int) (fadeAlpha() * 255.0f)));

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(getX(), getY());
        matrices.scale(getScale(), getScale());
        context.text(font, Component.literal(text), 0, 0, (alphaByte << 24) | 0xFFFFFF, true);
        matrices.popMatrix();
    }

    private float fadeAlpha() {
        return switch (fadeState) {
            case FADING_IN -> clamp01(1.0f - (fadeTicksRemaining / (float) FADE_IN_TICKS));
            case FADING_OUT -> clamp01(fadeTicksRemaining / (float) FADE_OUT_TICKS);
            case VISIBLE -> 1.0f;
            case HIDDEN -> 0.0f;
        };
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private void reset() {
        displayTicksRemaining = 0;
        fadeTicksRemaining = 0;
        fadeState = FadeState.HIDDEN;
        text = "";
    }

    private enum FadeState {
        HIDDEN,
        FADING_IN,
        VISIBLE,
        FADING_OUT
    }
}
