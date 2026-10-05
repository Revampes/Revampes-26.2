package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.ScreenClickEvent).
 * Posted from {@code AbstractContainerScreen#slotClicked}.
 */
public record ScreenClickEvent(Screen screen, Slot slot) {
}
