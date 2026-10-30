package xyz.revampes.Revamp.utility.dungeon.terminal;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import xyz.revampes.Revamp.modules.impl.dungeon.BetterTerminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "What starts with: 'X'?" — Odin's {@code StartsWithHandler} solver.
 *
 * <p>A slot is a valid target when its name starts with the letter and it is not glinting
 * (glint marks "already clicked"), except for items that always glint, which are listed in
 * {@link TerminalItems#ENCHANT_OVERRIDES}. The extra override map keeps a clicked slot
 * excluded even when the item's own glint would otherwise be ignored.
 */
public class StartsWithTerminalHandler extends TerminalHandler {

    private final String letter;
    /** slot -> confirmed. Odin keeps the same map so a pending click stays visible. */
    private final Map<Integer, Boolean> clickedOverrides = new HashMap<>();

    public StartsWithTerminalHandler(BetterTerminal module, String letter) {
        super(module, TerminalType.STARTS_WITH);
        this.letter = letter == null ? "" : letter;
    }

    @Override
    protected List<Integer> solve(List<Slot> slots) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            ItemStack stack = slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            if (!stack.getHoverName().getString().regionMatches(true, 0, letter, 0, letter.length())) continue;
            if (Boolean.TRUE.equals(clickedOverrides.get(i))) continue;
            if (TerminalItems.hasGlint(stack) && !TerminalItems.isEnchantOverride(stack)) continue;
            result.add(i);
        }
        return result;
    }

    @Override
    public boolean click(int slotIndex, int button) {
        if (solution().contains(slotIndex)) clickedOverrides.putIfAbsent(slotIndex, Boolean.FALSE);
        return super.click(slotIndex, button);
    }

    @Override
    protected void onSlotConfirmed(int slotIndex) {
        if (clickedOverrides.containsKey(slotIndex)) clickedOverrides.put(slotIndex, Boolean.TRUE);
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
                    slotType = SlotType.START_CORRECT;
                } else if (TerminalItems.hasGlint(stack) || isPendingClick(slotIndex)
                        || Boolean.TRUE.equals(clickedOverrides.get(slotIndex))) {
                    slotType = SlotType.START_CLICKED;
                } else {
                    slotType = SlotType.START_WRONG;
                }

                putCell(grid, row, col, new SlotRenderInfo(slotType, null));
            }
        }

        return Arrays.asList(grid);
    }
}
