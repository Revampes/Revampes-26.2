package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.sounds.SoundEvent;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.HudNotificationEvent).
 * Consumed by {@code KuudraNotificationsWidget}.
 */
public record HudNotificationEvent(String text, int durationTicks, SoundEvent soundEvent) {

    public HudNotificationEvent(String text, int durationTicks) {
        this(text, durationTicks, null);
    }
}
