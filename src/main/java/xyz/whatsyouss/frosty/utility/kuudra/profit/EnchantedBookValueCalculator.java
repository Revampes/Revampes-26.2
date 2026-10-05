package xyz.whatsyouss.frosty.utility.kuudra.profit;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public class EnchantedBookValueCalculator implements ItemValueCalculator {

    @Override
    public double calculateValue(ItemStack stack, String itemId, int quantity) {
        if (!"ENCHANTED_BOOK".equals(itemId)) {
            return manager.getItemPrice(itemId) * quantity;
        }

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0D;

        var tag = customData.copyTag();
        if (!tag.contains("enchantments")) return 0D;

        var enchantments = tag.getCompound("enchantments");
        if (enchantments.isEmpty()) return 0D;

        for (String enchantment : enchantments.get().keySet()) {
            var level = enchantments.get().getInt(enchantment);
            if (level.isEmpty()) return 0D;

            String enchantmentId = String.format("ENCHANTMENT_%s_%S", enchantment.toUpperCase(), level.get());
            return manager.getItemPrice(enchantmentId) * quantity;
        }

        return 0D;
    }
}
