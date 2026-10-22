package xyz.whatsyouss.frosty.utility.dungeon.terminal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import xyz.whatsyouss.frosty.modules.impl.dungeon.BetterTerminal;
import xyz.whatsyouss.frosty.utility.StringUtils;

/**
 * Owns the terminal that is currently open on screen and keeps it in sync with the server.
 *
 * <p>Open/close detection happens on the render and tick paths (the module drives both), slot
 * updates come from {@code ClientPacketListenerMixin} after the client has applied them, and
 * the resolve timeout mirrors Odin: when a click has not been acknowledged within
 * {@code resolveTimeout} ms the prediction queue is dropped and the terminal re-solves from
 * the real container contents.
 */
public final class TerminalManager {

    private static final Minecraft mc = Minecraft.getInstance();

    private static TerminalHandler current;
    private static AbstractContainerScreen<?> currentScreen;

    private TerminalManager() {
    }

    public static TerminalHandler current() {
        return current;
    }

    public static boolean isActive() {
        return current != null;
    }

    public static AbstractContainerScreen<?> currentScreen() {
        return currentScreen;
    }

    /**
     * Matches the screen against the known terminal titles and (re)opens the solver when it
     * changed. Safe to call every frame and every tick.
     */
    public static void update(AbstractContainerScreen<?> screen, BetterTerminal module) {
        if (screen == null) {
            close();
            return;
        }

        String title = StringUtils.stripFormatting(screen.getTitle().getString()).trim();
        TerminalType type = TerminalType.match(title);
        if (type == null) {
            close();
            return;
        }

        if (current != null && current.type() == type && currentScreen == screen) {
            // Re-solve every frame instead of only on slot packets: the container contents can
            // already be complete by the time the screen is first drawn (the content packet
            // arrives before the first render), in which case there is no packet left to react
            // to and the panel would stay blank until a manual reload.
            current.sync(screen.getMenu().slots);
            current.recompute();
            return;
        }

        TerminalHandler handler = type.createHandler(module, title);
        if (handler == null) {
            close();
            return;
        }

        current = handler;
        currentScreen = screen;
        current.sync(screen.getMenu().slots);
        current.recompute();
        if (module != null) module.onTerminalChanged(handler);
    }

    /**
     * Called after the server changed the container — either a
     * {@code ClientboundContainerSetSlotPacket} or a {@code ClientboundContainerSetContentPacket}.
     *
     * <p>Purely a same-tick fast path: the panel also re-solves from the container every frame
     * and detects content changes itself, so terminals whose menu is mutated locally (such as a
     * client-side simulator that never sends a packet) stay in sync too.
     */
    public static void onContainerUpdate(int containerId) {
        TerminalHandler handler = current;
        AbstractContainerScreen<?> screen = currentScreen;
        if (handler == null || screen == null) return;
        if (screen.getMenu().containerId != containerId) return;
        handler.sync(screen.getMenu().slots);
        handler.recompute();
    }

    /** Per-tick upkeep: age the terminal and enforce Odin's resolve timeout. */
    public static void tick(BetterTerminal module) {
        TerminalHandler handler = current;
        if (handler == null) return;

        handler.tickOpened();

        long timeout = module == null ? 600L : (long) module.resolveTimeout.getInput();
        if (handler.queueSize() > 0
                && System.currentTimeMillis() - handler.lastClickTime() >= timeout) {
            handler.clearPredictions();
            if (currentScreen != null) {
                handler.sync(currentScreen.getMenu().slots);
                handler.recompute();
            }
        }
    }

    public static void close() {
        current = null;
        currentScreen = null;
    }
}
