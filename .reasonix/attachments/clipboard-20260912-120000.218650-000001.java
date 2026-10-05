package net.iqaddons.mod.features.kuudra.linus;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.ItemUseEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class SupplySwapFeature extends Feature {

    private final KuudraStateManager stateManager = KuudraStateManager.get();
    private int pendingPearlSlot = -1;
    public SupplySwapFeature() {
        super(
                "SupplySwap",
                "Supply Swap",
                () -> KuudraGeneralConfig.SupplySwap
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ItemUseEvent.class, this::onItemUse);
        subscribe(ClientTickEvent.class, this::onTick);
    }

    private void onItemUse(@NotNull ItemUseEvent event) {
        if (!stateManager.context().isInRun()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (mc.player == null || mc.gameMode == null) return;

        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !held.getHoverName().getString().contains("Elle's Supplies")) return;

        event.setCancelled(true);

        int pearlSlot = findHotbarPearlSlot();
        if (pearlSlot == -1) {
            log.info("[SupplySwap] No Ender Pearls found in hotbar");
            return;
        }

        // Schedule the swap + use for next tick to avoid re-entrant useItem call
        pendingPearlSlot = pearlSlot;
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (pendingPearlSlot == -1) return;
        if (mc.player == null || mc.gameMode == null) {
            pendingPearlSlot = -1;
            return;
        }

        int slot = pendingPearlSlot;
        pendingPearlSlot = -1;

        mc.player.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        log.info("[SupplySwap] Switched to pearl slot {} and used pearl", slot);
    }

    private int findHotbarPearlSlot() {
        if (mc.player == null) return -1;

        // Hotbar is slots 0-8
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.ENDER_PEARL) return i;
        }
        return -1;
    }
}
