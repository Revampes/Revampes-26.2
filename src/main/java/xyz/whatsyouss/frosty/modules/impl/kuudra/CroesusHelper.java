package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import xyz.whatsyouss.frosty.events.impl.ScreenDrawSlotEvent;
import xyz.whatsyouss.frosty.events.impl.ScreenKeyPressEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.ChestUtils;
import xyz.whatsyouss.frosty.utility.CroesusKeyMappings;
import xyz.whatsyouss.frosty.utility.StringUtils;

import java.util.Locale;
import java.util.Optional;

/**
 * Ported from IQAddons (features/kuudra/miscellaneous/CroesusHelperFeature).
 *
 * Highlights already-opened Croesus/Vesuvius chests and lets you page through the
 * chest menu with the (rebindable) advance / go-back keys, acting as if the
 * next/previous/back navigation slot was clicked.
 */
public class    CroesusHelper extends Module {

    private static final String OPENED_CHEST_LORE = "No more chests to open!";

    public CroesusHelper() {
        super("Croesus Helper", category.Kuudra);
        this.defaultEnabled = true;
    }

    @EventHandler
    public void onScreenDrawSlot(ScreenDrawSlotEvent event) {
        String title = StringUtils.stripFormatting(event.screen().getTitle().getString()).toLowerCase(Locale.ROOT);
        if (!title.contains("croesus") && !title.contains("vesuvius")) return;

        Slot slot = event.slot();
        boolean hasOpenedChest = ChestUtils.getLoreLines(slot.getItem()).stream()
                .map(StringUtils::stripFormatting)
                .map(line -> line.toLowerCase(Locale.ROOT))
                .anyMatch(line -> line.contains(OPENED_CHEST_LORE.toLowerCase(Locale.ROOT)));
        if (!hasOpenedChest) return;

        int left = slot.x;
        int top = slot.y;

        event.drawContext().fill(left, top, left + 16, top + 16, 0x66FF0000);
    }

    @EventHandler
    public void onScreenKeyPress(ScreenKeyPressEvent event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> handledScreen)) return;
        if (!isCroesusScreen(event.getScreenTitle())) return;

        Optional<Slot> navigationSlot;
        if (CroesusKeyMappings.getAdvancePageKey() != null
                && CroesusKeyMappings.getAdvancePageKey().matches(new KeyEvent(event.getKeyCode(), event.getScanCode(), event.getModifiers()))) {
            navigationSlot = findNavigationSlot(handledScreen, "next");
        } else if (CroesusKeyMappings.getGoBackPageKey() != null
                && CroesusKeyMappings.getGoBackPageKey().matches(new KeyEvent(event.getKeyCode(), event.getScanCode(), event.getModifiers()))) {
            navigationSlot = findNavigationSlot(handledScreen, "previous", "back");
        } else {
            return;
        }

        if (navigationSlot.isEmpty() || mc.player == null || mc.gameMode == null) return;

        event.setCancelled(true);
        mc.gameMode.handleContainerInput(
                handledScreen.getMenu().containerId,
                navigationSlot.get().index,
                0,
                ContainerInput.CLONE,
                mc.player
        );
    }

    private boolean isCroesusScreen(String title) {
        String normalizedTitle = StringUtils.stripFormatting(title).toLowerCase(Locale.ROOT);
        return normalizedTitle.contains("croesus") || normalizedTitle.contains("vesuvius");
    }

    private Optional<Slot> findNavigationSlot(AbstractContainerScreen<?> screen, String... labels) {
        return screen.getMenu().slots.stream()
                .filter(slot -> hasAnyLabel(slot, labels))
                .findFirst();
    }

    private boolean hasAnyLabel(Slot slot, String... labels) {
        if (!slot.hasItem()) return false;
        String stackName = StringUtils.stripFormatting(slot.getItem().getHoverName().getString()).toLowerCase(Locale.ROOT);
        for (String label : labels) {
            if (stackName.contains(label)) {
                return true;
            }
        }

        return false;
    }
}
