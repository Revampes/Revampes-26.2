package xyz.whatsyouss.frosty.utility.kuudra.profit;

import net.minecraft.world.item.ItemStack;

public class SalvageValueCalculator implements ItemValueCalculator {

    private static final int BASE_CRIMSON_ESSENCE = 108;
    private static final double KUUDRA_STAR_MULTIPLIER = 0.63;

    @Override
    public double calculateValue(ItemStack stack, String itemId, int quantity) {
        int stars = countStars(stack.getHoverName().getString());
        int totalStarCost = 0;
        for (int star = 1; star <= stars; star++) {
            totalStarCost += 20 + (star * 5);
        }

        int bonus = (int) Math.floor(totalStarCost * KUUDRA_STAR_MULTIPLIER);
        int salvageValue = BASE_CRIMSON_ESSENCE + bonus;

        if (ProfitTrackerConfig.armorValueType == ProfitTrackerConfig.ArmorValueType.SALVAGE) {
            return (manager.getItemPrice(ChestProfitUtil.CRIMSON_ESSENCE_ID) * salvageValue) * quantity;
        }

        return manager.getItemPrice(itemId) * quantity;
    }

    private int countStars(String name) {
        return (int) name.chars().filter(character -> character == '\u272A').count();
    }
}
