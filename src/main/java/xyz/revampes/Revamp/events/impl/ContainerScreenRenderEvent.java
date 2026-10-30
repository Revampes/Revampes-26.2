package xyz.revampes.Revamp.events.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Posted at the tail of {@code AbstractContainerScreen#extractRenderState}, i.e. after the
 * vanilla container contents have been submitted, so listeners can paint on top of the chest.
 *
 * <p>{@link #screen()} is the screen being drawn and {@link #mouseX()}/{@link #mouseY()} are
 * its GUI-scaled cursor position, which is what the Better Terminal panel needs to resolve
 * hover state without polling GLFW.
 */
public record ContainerScreenRenderEvent(
        AbstractContainerScreen<?> screen,
        GuiGraphicsExtractor drawContext,
        int mouseX,
        int mouseY,
        float tickDelta
) {
}
