package xyz.whatsyouss.frosty.utility;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from IQAddons' ChestProfitUtil lore helper (only the piece the Croesus
 * helper uses).
 */
public final class ChestUtils {

    private ChestUtils() {
    }

    public static List<String> getLoreLines(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        lore.lines().forEach(line -> lines.add(line.getString()));
        return lines;
    }
}
