package xyz.whatsyouss.frosty.utility.dungeon.terminal;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import xyz.whatsyouss.frosty.modules.impl.dungeon.BetterTerminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * "Click the button on time!" — Odin's {@code MelodyHandler} solver drawn with yqloss'
 * {@code TerminalAlign} visuals.
 *
 * <p>Odin's 26.x layout is the one that is used for the geometry: five rows, the target
 * marker lives in rows 0 and 4 (hence the {@code magenta + 36} entry, one row further down),
 * the moving lane sits in rows 1-3 and the press button is column 7 of those rows
 * (slots 16 / 25 / 34). yqloss' styling is layered on top: the target marker is drawn in the
 * two marker rows, the lane the pointer is currently in is highlighted, and every other lane
 * plus its button is dimmed.
 */
public class MelodyTerminalHandler extends TerminalHandler {

    /** Container column of the clickable "press" button (slots 16 / 25 / 34). */
    private static final int BUTTON_CONTAINER_COL = 7;
    /** Rows that show the target marker: the first and the last row of the 5-row layout. */
    private static final int TOP_ROW = 0;
    private static final int BOTTOM_ROW = 4;
    /** Container columns 1-5 hold the moving lane. */
    private static final int LANE_CONTAINER_COL = 1;
    private static final int LANE_COLUMNS = 5;

    public MelodyTerminalHandler(BetterTerminal module) {
        super(module, TerminalType.MELODY);
    }

    @Override
    protected List<Integer> solve(List<Slot> slots) {
        Item magenta = TerminalItems.pane(DyeColor.MAGENTA);
        Item limePane = TerminalItems.pane(DyeColor.LIME);
        Item limeClay = TerminalItems.terracotta(DyeColor.LIME);

        int magentaPane = indexOfFirst(slots, magenta);
        int greenPane = indexOfLast(slots, limePane);
        int greenClay = indexOfLast(slots, limeClay);
        if (magentaPane < 0 || greenPane < 0) return new ArrayList<>();

        List<Integer> result = new ArrayList<>(4);
        result.add(greenPane);
        result.add(magentaPane);
        result.add(magentaPane + 36);
        if (greenPane % 9 == magentaPane % 9 && greenClay >= 0) result.add(greenClay);
        return result;
    }

    /** The three press buttons are the only slots a melody click may target. */
    @Override
    protected boolean canClick(int slotIndex, int button) {
        return slotIndex == 16 || slotIndex == 25 || slotIndex == 34;
    }

    /** Melody has nothing to predict: the lane moves on its own. */
    @Override
    protected void simulateClick(int slotIndex, int button) {
    }

    private int targetColumn() {
        List<Integer> solution = solution();
        return solution.size() > 1 ? solution.get(1) % 9 : -1;
    }

    private int pointerRow() {
        List<Integer> solution = solution();
        return solution.isEmpty() ? -1 : solution.get(0) / 9;
    }

    private int pointerColumn() {
        List<Integer> solution = solution();
        return solution.isEmpty() ? -1 : solution.get(0) % 9;
    }

    @Override
    public List<SlotRenderInfo> render() {
        SlotRenderInfo[] grid = blankGrid(type);

        int target = targetColumn();
        int pointerRow = pointerRow();
        int pointerCol = pointerColumn();

        for (int row = 0; row < type.getRows(); row++) {
            boolean markerRow = row == TOP_ROW || row == BOTTOM_ROW;
            boolean activeRow = row == pointerRow;

            for (int containerCol = 0; containerCol < 9; containerCol++) {
                SlotRenderInfo info = SlotRenderInfo.EMPTY;

                if (markerRow) {
                    if (containerCol == target) info = new SlotRenderInfo(SlotType.ALIGN_TARGET, null);
                } else if (containerCol >= LANE_CONTAINER_COL
                        && containerCol < LANE_CONTAINER_COL + LANE_COLUMNS) {
                    if (activeRow) {
                        info = new SlotRenderInfo(containerCol == pointerCol
                                ? SlotType.ALIGN_ACTIVE_CURRENT
                                : SlotType.ALIGN_ACTIVE_OTHER, null);
                    } else {
                        info = new SlotRenderInfo(SlotType.ALIGN_INACTIVE, null);
                    }
                } else if (containerCol == BUTTON_CONTAINER_COL) {
                    info = new SlotRenderInfo(activeRow
                            ? SlotType.ALIGN_ACTIVE_BUTTON
                            : SlotType.ALIGN_INACTIVE_BUTTON, null);
                }

                putSlot(grid, row * 9 + containerCol, info);
            }
        }

        return Arrays.asList(grid);
    }

    private static int indexOfFirst(List<Slot> slots, Item item) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).getItem().getItem() == item) return i;
        }
        return -1;
    }

    private static int indexOfLast(List<Slot> slots, Item item) {
        for (int i = slots.size() - 1; i >= 0; i--) {
            if (slots.get(i).getItem().getItem() == item) return i;
        }
        return -1;
    }
}
