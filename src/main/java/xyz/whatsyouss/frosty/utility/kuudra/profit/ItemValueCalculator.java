package xyz.whatsyouss.frosty.utility.kuudra.profit;

import net.minecraft.world.item.ItemStack;

public interface ItemValueCalculator {

    ItemPriceManager manager = ItemPriceManager.get();

    double calculateValue(ItemStack stack, String itemId, int quantity);
}
