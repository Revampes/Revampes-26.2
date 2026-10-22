package xyz.whatsyouss.frosty.utility.dungeon.terminal;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import xyz.whatsyouss.frosty.modules.impl.dungeon.BetterTerminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * "Correct all the panes!" — Odin's {@code PanesHandler} solver with yqloss' visuals: every
 * pane in the grid is painted, red for "still wrong" and green for "already correct".
 */
public class PanesTerminalHandler extends TerminalHandler {

    public PanesTerminalHandler(BetterTerminal module) {
        super(module, TerminalType.PANES);
    }

    @Override
    protected List<Integer> solve(List<Slot> slots) {
        Item red = TerminalItems.pane(DyeColor.RED);
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).getItem().getItem() == red) result.add(i);
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
                DyeColor color = TerminalItems.paneColor(stack);
                if (color == null || color == DyeColor.BLACK) continue;

                SlotType type = solution().contains(slotIndex) ? SlotType.PANES_OFF : SlotType.PANES_ON;
                putCell(grid, row, col, new SlotRenderInfo(type, null));
            }
        }

        return Arrays.asList(grid);
    }
}
