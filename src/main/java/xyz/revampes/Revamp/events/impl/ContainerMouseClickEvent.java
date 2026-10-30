package xyz.revampes.Revamp.events.impl;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import xyz.revampes.Revamp.events.Cancellable;

/**
 * Posted at the head of {@code AbstractContainerScreen#mouseClicked}. Cancelling it stops the
 * vanilla slot click, which is how the Better Terminal panel consumes clicks that land on its
 * own drawn cells instead of the (hidden) chest slots.
 */
public class ContainerMouseClickEvent extends Cancellable {

    private final AbstractContainerScreen<?> screen;
    private final double mouseX;
    private final double mouseY;
    private final int button;

    public ContainerMouseClickEvent(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        this.screen = screen;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.button = button;
        setCancelled(false);
    }

    public AbstractContainerScreen<?> getScreen() {
        return screen;
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    /** Vanilla mouse button: 0 left, 1 right, 2 middle. */
    public int getButton() {
        return button;
    }
}
