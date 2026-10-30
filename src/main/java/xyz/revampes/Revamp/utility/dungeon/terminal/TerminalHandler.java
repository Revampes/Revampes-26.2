package xyz.revampes.Revamp.utility.dungeon.terminal;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import xyz.revampes.Revamp.modules.impl.dungeon.BetterTerminal;

import java.util.ArrayList;
import java.util.List;

/**
 * One live terminal.
 *
 * <p>The solving/clicking half is Odin's {@code TerminalHandler}: a {@link #solution} of the
 * slots that still need clicking, a {@link #clickedSlots} prediction queue of clicks the
 * server has not acknowledged yet, and a first-click protection window.
 *
 * <p>The rendering half is yqloss': each subtype turns its own state into a full
 * {@code lines * 9} array of {@link SlotRenderInfo}, which the module then paints as the
 * rounded coloured cells of the Better Terminal panel.
 */
public abstract class TerminalHandler {

    public static final int LEFT_CLICK = 0;
    public static final int RIGHT_CLICK = 1;
    /** Odin sends every non-Rubix-right click as a middle click, which the modern server
     * accepts on any terminal slot without failing the puzzle. */
    public static final int MIDDLE_CLICK = 2;

    protected static final Minecraft mc = Minecraft.getInstance();

    protected final BetterTerminal module;
    protected final TerminalType type;

    /** Clicks we sent but have not seen confirmed, as {@code {slotIndex, button}}. */
    private final List<int[]> clickedSlots = new ArrayList<>();
    private final List<Integer> solution = new ArrayList<>();
    /** The container contents as of the last {@link #sync}. */
    private List<Slot> slots = List.of();
    /** Per-slot fingerprints of the container contents, used to detect changes. */
    private int[] signatures;

    private final long timeOpened = System.currentTimeMillis();
    private long lastClickTime = 0L;
    private int ticksOpened = -1;
    /** Per-terminal override for the click-prediction queue (yqloss' "NQ" button). */
    private boolean prediction = true;

    protected TerminalHandler(BetterTerminal module, TerminalType type) {
        this.module = module;
        this.type = type;
    }

    public TerminalType type() {
        return type;
    }

    public List<Integer> solution() {
        return solution;
    }

    public int queueSize() {
        return clickedSlots.size();
    }

    /** Whether clicks are predicted ahead of the server (yqloss' "queue" mode). */
    public boolean prediction() {
        return prediction && (module == null || module.clickPrediction.isToggled());
    }

    /** Forces this terminal into yqloss' non-queue behaviour until it is reopened. */
    public void forceNonQueue() {
        prediction = false;
        clearPredictions();
        recompute();
    }

    public long lastClickTime() {
        return lastClickTime;
    }

    public void tickOpened() {
        ticksOpened++;
    }

    public String title() {
        return type.getDefaultTitle();
    }

    // ------------------------------------------------------------------ solving

    /** Adopts the current container contents without re-solving. */
    public void sync(List<Slot> allSlots) {
        this.slots = allSlots;
    }

    /** The container contents as of the last {@link #sync}. */
    protected List<Slot> container() {
        return slots;
    }

    /** Called when the container reports a change for {@code slotIndex}, before the re-solve. */
    protected void onSlotConfirmed(int slotIndex) {
    }

    /** Re-solves from scratch against the container contents we can currently see. */
    public void recompute() {
        // Confirm pending clicks from the content itself before solving, so a click is retired
        // as soon as the slot really changed — regardless of how that change reached the menu.
        confirmChangedSlots();

        int usable = Math.min(type.usableSlots(), slots.size());
        solution.clear();
        if (usable > 0) solution.addAll(solve(slots.subList(0, usable)));
        replayPredictions();
    }

    /**
     * Compares every slot against the last seen fingerprint and treats a change as the server
     * (or whatever is driving the container) acknowledging the clicks up to that slot.
     *
     * <p>This replaces relying on {@code ClientboundContainerSetSlotPacket} alone: Odin's
     * terminal simulator writes straight into the menu and never sends a packet, so a
     * packet-only signal would leave the prediction queue stuck until the resolve timeout.
     */
    private void confirmChangedSlots() {
        int size = slots.size();
        if (signatures == null || signatures.length != size) {
            // First sight of this container: record the baseline without confirming anything.
            signatures = new int[size];
            for (int i = 0; i < size; i++) signatures[i] = TerminalItems.signature(slots.get(i).getItem());
            return;
        }

        for (int i = 0; i < size; i++) {
            int signature = TerminalItems.signature(slots.get(i).getItem());
            if (signature == signatures[i]) continue;
            signatures[i] = signature;
            confirmSlot(i);
        }
    }

    private void confirmSlot(int slotIndex) {
        onSlotConfirmed(slotIndex);

        // Everything queued before the acknowledged click must have landed too.
        int index = indexOfClick(slotIndex);
        if (index >= 0) clickedSlots.subList(0, index + 1).clear();
    }

    private void replayPredictions() {
        if (!prediction()) return;
        for (int[] click : new ArrayList<>(clickedSlots)) simulateClick(click[0], click[1]);
    }

    protected int indexOfClick(int slotIndex) {
        for (int i = 0; i < clickedSlots.size(); i++) {
            if (clickedSlots.get(i)[0] == slotIndex) return i;
        }
        return -1;
    }

    protected boolean isPendingClick(int slotIndex) {
        return indexOfClick(slotIndex) >= 0;
    }

    /** Drops the prediction queue after the resolve timeout, so a lost click cannot wedge it. */
    public void clearPredictions() {
        clickedSlots.clear();
    }

    /**
     * Odin's {@code solve(slots, updatedIndex)}. Handlers that need to know which slot
     * triggered the re-solve read it from {@link #container()} instead.
     */
    protected abstract List<Integer> solve(List<Slot> slots);

    /** What clicking {@code slotIndex} does to the predicted solution. */
    protected void simulateClick(int slotIndex, int button) {
        solution.remove((Integer) slotIndex);
    }

    /** Whether the solver allows a click on this slot with this button. */
    protected boolean canClick(int slotIndex, int button) {
        return solution.contains(slotIndex);
    }

    // ------------------------------------------------------------------ clicking

    /**
     * Handles a click that landed on a drawn cell. Returns {@code true} when the click was
     * ours to consume, so the container screen must not process it.
     */
    public boolean click(int slotIndex, int button) {
        if (shouldProtect()) return false;
        if (!canClick(slotIndex, button)) return false;

        boolean rubixRight = button == RIGHT_CLICK && type == TerminalType.RUBIX;
        int mappedButton = rubixRight ? RIGHT_CLICK : MIDDLE_CLICK;
        ContainerInput input = rubixRight ? ContainerInput.PICKUP : ContainerInput.CLONE;

        clickedSlots.add(new int[]{slotIndex, mappedButton});
        lastClickTime = System.currentTimeMillis();

        if (prediction()) simulateClick(slotIndex, mappedButton);

        sendClick(slotIndex, mappedButton, input);
        if (module != null) module.onTerminalClick(solution.isEmpty());
        return true;
    }

    /**
     * Odin's first-click protection: never click within {@code firstClickProt} ms of opening
     * a terminal, and optionally wait a few server ticks when the server is lagging.
     */
    public boolean shouldProtect() {
        if (module == null) return false;

        long protection = (long) module.firstClickProt.getInput();
        if (protection > 0 && System.currentTimeMillis() - timeOpened < protection) return true;

        return module.lagProtection.isToggled()
                && !mc.hasSingleplayerServer()
                && ticksOpened < (int) module.lagProtectionTicks.getInput();
    }

    private void sendClick(int slotIndex, int button, ContainerInput input) {
        if (mc.player == null || mc.gameMode == null) return;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == null) return;
        mc.gameMode.handleContainerInput(menu.containerId, slotIndex, button, input, mc.player);
    }

    // ------------------------------------------------------------------ rendering

    /** Full {@code lines * 9} array of cells, header rows included (they render as EMPTY). */
    public abstract List<SlotRenderInfo> render();

    // ------------------------------------------------------------------ helpers

    protected static ItemStack stackAt(List<Slot> slots, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        return slots.get(index).getItem();
    }

    /** Builds a {@code lines * 9} array with every cell empty. */
    protected static SlotRenderInfo[] blankGrid(TerminalType type) {
        SlotRenderInfo[] grid = new SlotRenderInfo[type.getLines() * 9];
        for (int i = 0; i < grid.length; i++) grid[i] = SlotRenderInfo.EMPTY;
        return grid;
    }

    /**
     * Writes the visual state of one solver-grid cell.
     *
     * <p>The grid is indexed by container slot, exactly like yqloss' {@code draw(state)}
     * arrays; the module subtracts {@code beginLine * 9} when it turns that index into a
     * position in the panel, so header rows simply fall off the top.
     */
    protected void putCell(SlotRenderInfo[] grid, int row, int col, SlotRenderInfo info) {
        putSlot(grid, type.slotAt(row, col), info);
    }

    /** Writes a cell by raw container slot, for layouts that do not follow the grid. */
    protected void putSlot(SlotRenderInfo[] grid, int containerSlot, SlotRenderInfo info) {
        if (containerSlot >= 0 && containerSlot < grid.length) grid[containerSlot] = info;
    }
}
