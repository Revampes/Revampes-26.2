package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.ScreenDrawSlotEvent).
 * Posted from {@code AbstractContainerScreen#extractSlot} so modules can overlay
 * custom drawings on container slots.
 */
public record ScreenDrawSlotEvent(
        AbstractContainerScreen<?> screen,
        GuiGraphicsExtractor drawContext,
        Slot slot,
        int x,
        int y
) {
}
