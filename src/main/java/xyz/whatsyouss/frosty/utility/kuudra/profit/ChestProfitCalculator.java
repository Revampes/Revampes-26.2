package xyz.whatsyouss.frosty.utility.kuudra.profit;

import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Function;

public class ChestProfitCalculator {

    private final GenericValueCalculator defaultCalculator;
    private final Map<String, ItemValueCalculator> calculators;
    private final Function<ItemStack, String> itemIdResolver;

    public ChestProfitCalculator(
            GenericValueCalculator defaultCalculator,
            Map<String, ItemValueCalculator> calculators,
            Function<ItemStack, String> itemIdResolver
    ) {
        this.defaultCalculator = defaultCalculator;
        this.calculators = calculators;
        this.itemIdResolver = itemIdResolver;
    }

    public double calculateTotalValue(ChestContents contents) {
        return contents.items().stream().mapToDouble(this::calculateItemValue).sum();
    }

    public double calculateItemValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0D;

        String itemId = itemIdResolver.apply(stack);
        if (itemId == null || itemId.isBlank()) return 0D;

        int quantity = ChestProfitUtil.resolveItemQuantity(stack);
        if (quantity <= 0) quantity = stack.getCount();

        return calculators.getOrDefault(itemId, defaultCalculator).calculateValue(stack, itemId, quantity);
    }
}
