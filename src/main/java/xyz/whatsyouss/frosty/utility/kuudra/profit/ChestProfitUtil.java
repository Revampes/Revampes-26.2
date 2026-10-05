package xyz.whatsyouss.frosty.utility.kuudra.profit;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import xyz.whatsyouss.frosty.utility.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChestProfitUtil {

    public static final String CRIMSON_ESSENCE_ID = "ESSENCE_CRIMSON";

    private static final ChestProfitCalculator CHEST_PROFIT_CALCULATOR;
    private static final int BUY_SLOT = 31;
    private static final int INFO_SLOT = 49;

    public static final Map<String, String> KUUDRA_DROPS_NAME_TO_API_ID = Map.of(
            "CRIMSON ESSENCE", CRIMSON_ESSENCE_ID,
            "KUUDRA TEETH", "KUUDRA_TEETH",
            "KISMET FEATHER", "KISMET_FEATHER",
            "WHEEL OF FATE", "WHEEL_OF_FATE"
    );

    static {
        Map<String, ItemValueCalculator> calculators = new HashMap<>();
        calculators.put(CRIMSON_ESSENCE_ID, new EssenceValueCalculator());
        calculators.put("ENCHANTED_BOOK", new EnchantedBookValueCalculator());

        SalvageValueCalculator salvageCalculator = new SalvageValueCalculator();
        for (String armor : new String[]{"AURORA", "CRIMSON", "TERROR", "FERVOR", "HOLLOW"}) {
            for (String piece : new String[]{"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"}) {
                calculators.put(armor + "_" + piece, salvageCalculator);
            }
        }

        CHEST_PROFIT_CALCULATOR = new ChestProfitCalculator(
                new GenericValueCalculator(),
                calculators,
                ChestProfitUtil::resolveItemId
        );
    }

    private ChestProfitUtil() {
    }

    public static ChestValueBreakdown analyzeChest(List<Slot> slots) {
        List<ItemStack> chestItems = new ArrayList<>();
        List<ChestItemValue> items = new ArrayList<>();

        for (int slotId = 9; slotId <= 17; slotId++) {
            if (slotId >= slots.size()) continue;

            ItemStack stack = slots.get(slotId).getItem();
            if (stack == null || stack.isEmpty()) continue;

            chestItems.add(stack);

            String itemId = resolveItemId(stack);
            int quantity = Math.max(1, resolveItemQuantity(stack));
            double itemValue = CHEST_PROFIT_CALCULATOR.calculateItemValue(stack);

            if (CRIMSON_ESSENCE_ID.equals(itemId)) {
                double totalBonus = (ProfitTrackerConfig.kuudraPetBonus + ProfitTrackerConfig.attributeBonus) / 100.0;
                int bonusQuantity = (int) Math.round(quantity * totalBonus);
                if (bonusQuantity > 0) {
                    double bonusValue = ItemPriceManager.get().getItemPrice(CRIMSON_ESSENCE_ID) * bonusQuantity;
                    itemValue -= bonusValue;

                    items.add(new ChestItemValue(Component.literal("§dCrimson Essence Bonus"), bonusQuantity, bonusValue));
                }
            }

            items.add(new ChestItemValue(stack.getHoverName(), Math.max(1, stack.getCount()), itemValue));
        }

        ChestContents contents = new ChestContents(chestItems, parseKeyType(slots));
        double totalValue = CHEST_PROFIT_CALCULATOR.calculateTotalValue(contents);
        double keyCost = ItemPriceManager.get().calculateKeyPrice(contents.keyType());

        return new ChestValueBreakdown(
                totalValue,
                keyCost,
                totalValue - keyCost,
                items.stream()
                        .filter(chestItemValue -> chestItemValue.value() > 0)
                        .sorted(Comparator.comparingDouble(ChestItemValue::value).reversed())
                        .toList()
        );
    }

    public static ChestData parseChest(List<Slot> slots, ItemPriceManager priceManager, ChestType chestType) {
        List<ItemStack> chestItems = new ArrayList<>();
        int essence = 0;
        int pricedItems = 0;

        for (int slotId = 9; slotId <= 17; slotId++) {
            if (slotId >= slots.size()) continue;

            ItemStack stack = slots.get(slotId).getItem();
            if (stack == null || stack.isEmpty()) continue;

            chestItems.add(stack);
            String itemId = resolveItemId(stack);
            if (CRIMSON_ESSENCE_ID.equals(itemId)) {
                essence += resolveItemQuantity(stack);
            }

            if (itemId != null && priceManager.getItemPrice(itemId) > 0L) {
                pricedItems++;
            }
        }

        ChestContents contents = new ChestContents(chestItems, parseKeyType(slots));
        long grossValue = Math.max(0L, Math.round(CHEST_PROFIT_CALCULATOR.calculateTotalValue(contents)));
        long keyCost = chestType == ChestType.PAID
                ? Math.max(0L, Math.round(priceManager.calculateKeyPrice(contents.keyType())))
                : 0L;

        return new ChestData(chestType, grossValue, keyCost, grossValue - keyCost, essence, pricedItems);
    }

    public static boolean canUseReroll(ItemStack stack, String blockedPhrase) {
        if (stack == null || stack.isEmpty()) return false;

        String title = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();
        if (!title.contains("reroll")) return false;

        String loreJoined = getLoreLines(stack).stream()
                .map(StringUtils::stripFormatting)
                .map(String::toLowerCase)
                .reduce("", (left, right) -> left + "\n" + right);

        return !loreJoined.contains(blockedPhrase.toLowerCase());
    }

    public static int resolveItemQuantity(ItemStack stack) {
        String name = StringUtils.stripFormatting(stack.getHoverName().getString());
        int index = name.lastIndexOf(" x");
        if (index == -1) return stack.getCount();

        String numberPart = name.substring(index + 2).replace(",", "");
        try {
            return Integer.parseInt(numberPart);
        } catch (NumberFormatException ignored) {
            return stack.getCount();
        }
    }

    public static List<String> getLoreLines(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return List.of();

        List<String> lines = new ArrayList<>();
        lore.lines().forEach(line -> lines.add(line.getString()));
        return lines;
    }

    private static ChestKeyType parseKeyType(List<Slot> slots) {
        if (isFreeChest(slots)) return ChestKeyType.FREE;
        if (INFO_SLOT >= slots.size()) return ChestKeyType.UNKNOWN;

        ItemStack infoStack = slots.get(INFO_SLOT).getItem();
        if (infoStack == null || infoStack.isEmpty()) return ChestKeyType.UNKNOWN;

        for (String rawLine : getLoreLines(infoStack)) {
            String lower = StringUtils.stripFormatting(rawLine).toLowerCase();
            if (!lower.contains("kuudra")) continue;

            if (lower.contains("infernal")) return ChestKeyType.INFERNAL;
            if (lower.contains("fiery")) return ChestKeyType.FIERY;
            if (lower.contains("burning")) return ChestKeyType.BURNING;
            if (lower.contains("hot")) return ChestKeyType.HOT;
            if (lower.contains("basic")) return ChestKeyType.BASIC;
        }

        return ChestKeyType.UNKNOWN;
    }

    private static boolean isFreeChest(List<Slot> slots) {
        if (BUY_SLOT >= slots.size()) return false;

        ItemStack buyStack = slots.get(BUY_SLOT).getItem();
        if (buyStack == null || buyStack.isEmpty()) return false;

        return getLoreLines(buyStack).stream()
                .map(StringUtils::stripFormatting)
                .map(String::toLowerCase)
                .anyMatch(line -> line.contains("free") || line.contains("free reward chest"));
    }

    private static String resolveItemId(ItemStack stack) {
        String itemId = getSkyblockItemId(stack);
        if (itemId != null && !itemId.isBlank()) return itemId;

        String shardId = resolveShardId(stack);
        if (shardId != null) return shardId;

        String displayName = stripTrailingQuantity(StringUtils
                .stripFormatting(stack.getHoverName().getString())
                .toUpperCase()
                .trim());

        return findMappedItemId(displayName, KUUDRA_DROPS_NAME_TO_API_ID);
    }

    private static String resolveShardId(ItemStack stack) {
        String name = stripTrailingQuantity(StringUtils
                .stripFormatting(stack.getHoverName().getString())
                .trim()
                .toUpperCase());

        if (!name.endsWith(" SHARD")) return null;

        String base = name.substring(0, name.length() - " SHARD".length()).trim();
        return "SHARD_" + base.replace(" ", "_");
    }

    private static String stripTrailingQuantity(String name) {
        int index = name.lastIndexOf(" X");
        if (index == -1) return name;

        String possibleNumber = name.substring(index + 2).replace(",", "");
        if (possibleNumber.matches("\\d+")) {
            return name.substring(0, index).trim();
        }

        return name;
    }

    private static String findMappedItemId(String displayName, Map<String, String> mapping) {
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (displayName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static String getSkyblockItemId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;

        var tag = customData.copyTag();
        if (!tag.contains("id")) return null;

        return tag.getString("id").orElse("UNKNOWN");
    }
}
