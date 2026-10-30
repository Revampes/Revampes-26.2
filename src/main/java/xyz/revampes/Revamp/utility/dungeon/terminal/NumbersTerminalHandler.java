package xyz.revampes.Revamp.utility.dungeon.terminal;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import xyz.revampes.Revamp.modules.impl.dungeon.BetterTerminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * "Click in order!" — Odin's {@code NumbersHandler} solver (red panes sorted by stack size,
 * click the smallest first) rendered with yqloss' order palette: the next three clicks are
 * green / yellow / red and everything after that is gray.
 */
public class NumbersTerminalHandler extends TerminalHandler {

    public NumbersTerminalHandler(BetterTerminal module) {
        super(module, TerminalType.NUMBERS);
    }

    @Override
    protected List<Integer> solve(List<Slot> slots) {
        Item red = TerminalItems.pane(DyeColor.RED);
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).getItem().getItem() == red) result.add(i);
        }
        result.sort(Comparator.comparingInt(index -> slots.get(index).getItem().getCount()));
        return result;
    }

    /** Only the smallest remaining number may be clicked, which is what stops fails. */
    @Override
    protected boolean canClick(int slotIndex, int button) {
        List<Integer> solution = solution();
        return !solution.isEmpty() && solution.get(0) == slotIndex;
    }

    /**
     * Removes the clicked slot from the predicted solution.
     *
     * <p>Must be slot-based, not position-based: the base state is re-solved from the server
     * every frame, so by the time a pending prediction is replayed the clicked slot may
     * already be gone. Removing index 0 blindly would then delete the *next* target, which made
     * the following number vanish for a few frames (and become unclickable) until the click
     * registered.
     */
    @Override
    protected void simulateClick(int slotIndex, int button) {
        solution().remove((Integer) slotIndex);
    }

    @Override
    public List<SlotRenderInfo> render() {
        SlotRenderInfo[] grid = blankGrid(type);
        List<Slot> slots = container();
        List<Integer> solution = solution();

        boolean showNumber = module == null || module.orderShowNumber.isToggled();
        boolean showClicked = module != null && module.orderShowClickedNumber.isToggled();

        for (int row = 0; row < type.getRows(); row++) {
            for (int col = 0; col < type.getCols(); col++) {
                int slotIndex = type.slotAt(row, col);
                ItemStack stack = stackAt(slots, slotIndex);
                if (stack.isEmpty() || stack.getItem() == TerminalItems.blackPane()) continue;

                String text = String.valueOf(stack.getCount());
                int position = solution.indexOf(slotIndex);

                SlotType slotType;
                if (position < 0) {
                    slotType = SlotType.ORDER_CLICKED;
                    if (!showClicked) text = null;
                } else {
                    switch (position) {
                        case 0:
                            slotType = SlotType.ORDER_1;
                            break;
                        case 1:
                            slotType = SlotType.ORDER_2;
                            break;
                        case 2:
                            slotType = SlotType.ORDER_3;
                            break;
                        default:
                            slotType = SlotType.ORDER_OTHER;
                            break;
                    }
                    if (!showNumber) text = null;
                }

                putCell(grid, row, col, new SlotRenderInfo(slotType, text));
            }
        }

        return Arrays.asList(grid);
    }
}
