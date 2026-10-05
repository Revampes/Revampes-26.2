package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import xyz.whatsyouss.frosty.events.Cancellable;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.ItemUseEvent).
 * Posted from {@code MultiPlayerGameMode#useItem / #useItemOn}; cancelling prevents
 * the vanilla use from happening (used by SupplySwap).
 */
public class ItemUseEvent extends Cancellable {

    private final InteractionHand hand;
    private final ItemStack itemStack;

    public ItemUseEvent(InteractionHand hand, ItemStack itemStack) {
        this.hand = hand;
        this.itemStack = itemStack;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
