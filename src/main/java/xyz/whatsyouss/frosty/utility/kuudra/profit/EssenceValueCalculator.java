package xyz.whatsyouss.frosty.utility.kuudra.profit;

import net.minecraft.world.item.ItemStack;

public class EssenceValueCalculator implements ItemValueCalculator {

    @Override
    public double calculateValue(ItemStack stack, String itemId, int quantity) {
        double totalBonus = (ProfitTrackerConfig.kuudraPetBonus + ProfitTrackerConfig.attributeBonus) / 100.0;
        int finalAmount = (int) Math.round(quantity * (1 + totalBonus));
        return manager.getItemPrice(itemId) * finalAmount;
    }
}
