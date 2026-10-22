package xyz.whatsyouss.frosty.utility.dungeon.terminal;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.StainedGlassPaneBlock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 26.2 item lookups for the terminal solvers.
 *
 * <p>1.8.9 (and yqloss) identified terminal items by {@code ItemStack#getMetadata()}; 26.x
 * flattened those into per-colour items exposed as {@code ColorCollection}s, so
 * {@code Items.STAINED_GLASS_PANE.pick(DyeColor.RED)} replaces {@code metadata == 14}.
 */
public final class TerminalItems {

    /** Items whose glint comes from the item definition rather than "this was clicked". */
    public static final Set<Item> ENCHANT_OVERRIDES;

    static {
        Set<Item> overrides = new HashSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item.components().has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)) overrides.add(item);
        }
        overrides.add(Items.GOLDEN_APPLE);
        ENCHANT_OVERRIDES = Collections.unmodifiableSet(overrides);
    }

    private TerminalItems() {
    }

    public static Item pane(DyeColor color) {
        return Items.STAINED_GLASS_PANE.pick(color);
    }

    public static Item blackPane() {
        return pane(DyeColor.BLACK);
    }

    public static Item terracotta(DyeColor color) {
        return Items.DYED_TERRACOTTA.pick(color);
    }

    /** The dye colour of a stained glass pane stack, or {@code null} for anything else. */
    public static DyeColor paneColor(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return null;
        if (!(blockItem.getBlock() instanceof StainedGlassPaneBlock pane)) return null;
        return pane.getColor();
    }

    /**
     * Odin's {@code hasGlint}: only the glint override counts, because Hypixel's terminal
     * items can carry real enchantments that must not be mistaken for the "already done" flag.
     */
    public static boolean hasGlint(ItemStack stack) {
        return stack.getComponents().get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) != null;
    }

    /** Whether the stack is a vanilla enchanted-looking item (so its glint is not a marker). */
    public static boolean isEnchantOverride(ItemStack stack) {
        return ENCHANT_OVERRIDES.contains(stack.getItem());
    }

    /**
     * Cheap per-slot fingerprint used to notice that the container content changed.
     *
     * <p>Captures exactly what the solvers read — which item, how many, and whether it carries
     * the "already clicked" glint — so any transport is detected the same way: a real server
     * packet, or a local simulator mutating the menu without ever sending one. A collision can
     * only delay a confirmation until the resolve timeout, never corrupt the solution.
     */
    public static int signature(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;

        int hash = System.identityHashCode(stack.getItem());
        hash = 31 * hash + stack.getCount();
        hash = 31 * hash + (hasGlint(stack) ? 1 : 0);
        return hash;
    }
}
