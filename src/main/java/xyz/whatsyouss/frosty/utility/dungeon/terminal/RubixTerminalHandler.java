package xyz.whatsyouss.frosty.utility.dungeon.terminal;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import xyz.whatsyouss.frosty.modules.impl.dungeon.BetterTerminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Change all to same color!" — Odin's {@code RubixHandler} solver.
 *
 * <p>The target colour is locked in as soon as all nine panes are present, then every pane
 * gets the shortest number of clicks that reaches it; going "the other way round" turns the
 * clicks negative, which is what makes the module emit right clicks for those slots.
 */
public class RubixTerminalHandler extends TerminalHandler {

    /** The click wheel order of the pane colours in this terminal. */
    private static final List<DyeColor> COLOR_ORDER = List.of(
            DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.GREEN, DyeColor.BLUE, DyeColor.RED);

    private DyeColor lockedColor;
    private final Set<Integer> rightClickSlots = new HashSet<>();

    public RubixTerminalHandler(BetterTerminal module) {
        super(module, TerminalType.RUBIX);
    }

    @Override
    protected List<Integer> solve(List<Slot> slots) {
        // Only the 3x3 grid is solved. Odin scans every non-black pane in the container, which
        // relies on the terminal having exactly nine of them; restricting to the grid keeps the
        // solution (and the click targets) correct even when the chest carries other panes.
        List<int[]> panes = new ArrayList<>();
        int filledSlots = 0;
        for (int row = 0; row < type.getRows(); row++) {
            for (int col = 0; col < type.getCols(); col++) {
                int slotIndex = type.slotAt(row, col);
                if (slotIndex < 0 || slotIndex >= slots.size()) continue;

                DyeColor color = TerminalItems.paneColor(slots.get(slotIndex).getItem());
                if (color == null || color == DyeColor.BLACK) continue;
                filledSlots++;

                int colorIndex = COLOR_ORDER.indexOf(color);
                if (colorIndex < 0) continue;

                panes.add(new int[]{slotIndex, colorIndex});
            }
        }

        // Lock the goal colour as soon as the whole grid has been received. The grid holds
        // rows * cols panes (nine); comparing against COLOR_ORDER.size() would never match.
        // The trigger counts every pane in the grid, so a colour outside the click wheel still
        // lets the terminal lock instead of silently showing nothing.
        if (lockedColor == null && !panes.isEmpty()
                && filledSlots == type.getRows() * type.getCols()) {
            int best = 0;
            int bestClicks = Integer.MAX_VALUE;
            for (int goal = 0; goal < COLOR_ORDER.size(); goal++) {
                int total = 0;
                for (int[] pane : panes) total += Math.abs(clicksFor(pane[1], goal));
                if (total < bestClicks) {
                    bestClicks = total;
                    best = goal;
                }
            }
            lockedColor = COLOR_ORDER.get(best);
        }

        rightClickSlots.clear();
        List<Integer> result = new ArrayList<>();
        if (lockedColor == null) return result;

        int goal = COLOR_ORDER.indexOf(lockedColor);
        for (int[] pane : panes) {
            int clicks = clicksFor(pane[1], goal);
            if (clicks == 0) continue;
            if (clicks < 0) rightClickSlots.add(pane[0]);
            for (int i = 0; i < Math.abs(clicks); i++) result.add(pane[0]);
        }
        return result;
    }

    /** Signed number of clicks taking {@code from} to {@code to}, shortest way round. */
    private int clicksFor(int from, int to) {
        int size = COLOR_ORDER.size();
        int forward = from > to ? (to + size) - from : to - from;
        boolean fewestClicks = module == null || module.rubixMode.getValue() != 1;
        if (forward > 2 && fewestClicks) forward -= size;
        return forward;
    }

    @Override
    protected boolean canClick(int slotIndex, int button) {
        if (!solution().contains(slotIndex)) return false;
        return (button == RIGHT_CLICK) == rightClickSlots.contains(slotIndex);
    }

    @Override
    public List<SlotRenderInfo> render() {
        SlotRenderInfo[] grid = blankGrid(type);
        // Nothing is solvable until the goal colour is locked in (i.e. the whole grid arrived);
        // painting the cells before that would show a misleading all-correct board.
        if (lockedColor == null) return Arrays.asList(grid);

        boolean showNumber = module == null || module.rubixShowNumber.isToggled();

        for (int row = 0; row < type.getRows(); row++) {
            for (int col = 0; col < type.getCols(); col++) {
                int slotIndex = type.slotAt(row, col);
                ItemStack stack = stackAt(container(), slotIndex);
                if (TerminalItems.paneColor(stack) == null) continue;

                int count = Collections.frequency(solution(), slotIndex);
                SlotType slotType;
                String text;

                if (count == 0) {
                    slotType = SlotType.RUBIX_CORRECT;
                    text = null;
                } else {
                    // solution stores abs(clicks) copies of the slot, so the frequency is always
                    // positive; the direction lives in rightClickSlots. Render the signed count
                    // so a right click reads as "-1" / "-2" and a left click as "1" / "2"
                    // (and "3"/"4" in left-clicks-only mode).
                    int clicks = rightClickSlots.contains(slotIndex) ? -count : count;
                    if (clicks == -1) {
                        slotType = SlotType.RUBIX_RIGHT_1;
                    } else if (clicks < 0) {
                        slotType = SlotType.RUBIX_RIGHT_2;
                    } else if (clicks == 1) {
                        slotType = SlotType.RUBIX_LEFT_1;
                    } else {
                        slotType = SlotType.RUBIX_LEFT_2;
                    }
                    text = String.valueOf(clicks);
                }

                if (!showNumber || text == null) text = null;
                putCell(grid, row, col, new SlotRenderInfo(slotType, text));
            }
        }

        return Arrays.asList(grid);
    }
}
