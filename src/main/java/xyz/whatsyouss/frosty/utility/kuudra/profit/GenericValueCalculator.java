package xyz.whatsyouss.frosty.utility.kuudra.profit;

import net.minecraft.world.item.ItemStack;

public class GenericValueCalculator implements ItemValueCalculator {

    @Override
    public double calculateValue(ItemStack stack, String itemId, int quantity) {
        return manager.getItemPrice(itemId) * Math.max(1, quantity);
    }
}
