package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xyz.whatsyouss.frosty.events.impl.ItemUseEvent;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;

public class SupplySwap extends Module {

    private int pendingPearlSlot = -1;

    public SupplySwap() {
        super("Supply Swap", "快速切珍珠", category.Kuudra);
    }

    @Override
    public void onDisable() {
        this.pendingPearlSlot = -1;
    }

    @EventHandler
    public void onItemUse(ItemUseEvent event) {
        if (!KuudraState.get().isInRun()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (mc.player == null || mc.gameMode == null) return;

        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !held.getHoverName().getString().contains("Elle's Supplies")) return;

        event.setCancelled(true);

        int pearlSlot = findHotbarPearlSlot();
        if (pearlSlot == -1) {
            return;
        }

        // Schedule the swap + use for next tick to avoid re-entrant useItem call
        this.pendingPearlSlot = pearlSlot;
    }

    @EventHandler
    public void onTick(PreUpdateEvent event) {
        if (this.pendingPearlSlot == -1) return;
        if (mc.player == null || mc.gameMode == null) {
            this.pendingPearlSlot = -1;
            return;
        }

        int slot = this.pendingPearlSlot;
        this.pendingPearlSlot = -1;

        mc.player.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
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
