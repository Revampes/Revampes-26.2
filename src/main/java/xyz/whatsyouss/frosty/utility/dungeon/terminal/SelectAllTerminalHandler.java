package xyz.whatsyouss.frosty.utility.dungeon.terminal;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import xyz.whatsyouss.frosty.modules.impl.dungeon.BetterTerminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * "Select all the {@code <colour>} items!" — Odin's {@code SelectAllHandler} solver.
 *
 * <p>The candidate set is matched by item-name prefix (including the classic aliases such as
 * "Ink" for black and "Lapis" for blue) because the terminal mixes dyed blocks and vanilla
 * materials. Items carrying the glint override are the ones already clicked, so they are
 * skipped.
 */
public class SelectAllTerminalHandler extends TerminalHandler {

    private static final Map<DyeColor, Set<String>> PREFIXES = buildPrefixes();

    private final Set<String> validPrefixes;

    private SelectAllTerminalHandler(BetterTerminal module, Set<String> validPrefixes) {
        super(module, TerminalType.SELECT);
        this.validPrefixes = validPrefixes;
    }

    /** Builds the handler for the colour named in the screen title, or {@code null}. */
    public static SelectAllTerminalHandler create(BetterTerminal module, String titleColor) {
        if (titleColor == null) return null;

        String normalized = titleColor.replace("SILVER", "LIGHT GRAY");
        for (DyeColor color : DyeColor.values()) {
            if (!color.name().replace('_', ' ').equalsIgnoreCase(normalized)) continue;
            return new SelectAllTerminalHandler(module, prefixesFor(color));
        }
        return null;
    }

    private static Map<DyeColor, Set<String>> buildPrefixes() {
        Map<DyeColor, Set<String>> prefixes = new HashMap<>();
        prefixes.put(DyeColor.BLACK, Set.of("black", "ink"));
        prefixes.put(DyeColor.BLUE, Set.of("blue", "lapis"));
        prefixes.put(DyeColor.BROWN, Set.of("brown", "cocoa"));
        prefixes.put(DyeColor.WHITE, Set.of("white", "bone", "wool"));
        prefixes.put(DyeColor.GREEN, Set.of("green", "cactus"));
        prefixes.put(DyeColor.RED, Set.of("red", "rose"));
        prefixes.put(DyeColor.YELLOW, Set.of("yellow", "dandelion"));
        prefixes.put(DyeColor.LIGHT_GRAY, Set.of("silver", "light gray"));
        return prefixes;
    }

    private static Set<String> prefixesFor(DyeColor color) {
        Set<String> known = PREFIXES.get(color);
        if (known != null) return known;
        return Set.of(color.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    @Override
    protected List<Integer> solve(List<Slot> slots) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty() || stack.getItem() == TerminalItems.blackPane()) continue;
            if (TerminalItems.hasGlint(stack)) continue;

            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            for (String prefix : validPrefixes) {
                if (name.startsWith(prefix)) {
                    result.add(i);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public List<SlotRenderInfo> render() {
        SlotRenderInfo[] grid = blankGrid(type);
        List<Slot> slots = container();

        for (int row = 0; row < type.getRows(); row++) {
            for (int col = 0; col < type.getCols(); col++) {
                int slotIndex = type.slotAt(row, col);
                ItemStack stack = stackAt(slots, slotIndex);
                if (stack.isEmpty() || stack.getItem() == TerminalItems.blackPane()) continue;

                SlotType slotType;
                if (solution().contains(slotIndex)) {
                    slotType = SlotType.COLOR_CORRECT;
                } else if (TerminalItems.hasGlint(stack) || isPendingClick(slotIndex)) {
                    slotType = SlotType.COLOR_CLICKED;
                } else {
                    slotType = SlotType.COLOR_WRONG;
                }

                putCell(grid, row, col, new SlotRenderInfo(slotType, null));
            }
        }

        return Arrays.asList(grid);
    }
}
