package xyz.whatsyouss.frosty.utility.kuudra.profit;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ChestContents(List<ItemStack> items, ChestKeyType keyType) {
}
