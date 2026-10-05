package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.client.gui.screens.Screen;
import xyz.whatsyouss.frosty.events.Cancellable;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.ScreenKeyPressEvent).
 * Posted from {@code AbstractContainerScreen#keyPressed}; cancelling consumes the key.
 */
public class ScreenKeyPressEvent extends Cancellable {

    private final Screen screen;
    private final int keyCode;
    private final int scanCode;
    private final int modifiers;

    public ScreenKeyPressEvent(Screen screen, int keyCode, int scanCode, int modifiers) {
        this.screen = screen;
        this.keyCode = keyCode;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
    }

    public Screen getScreen() {
        return screen;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public int getScanCode() {
        return scanCode;
    }

    public int getModifiers() {
        return modifiers;
    }

    public String getScreenTitle() {
        return screen.getTitle().getString();
    }
}
