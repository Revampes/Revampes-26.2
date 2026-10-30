package xyz.revampes.Revamp.utility.dungeon.terminal;

/**
 * What a single drawn cell shows: a colour source ({@link SlotType}, resolved to a
 * {@code ColorSetting} by the module) and the centred label, if any.
 * Port of yqloss' {@code Terminal.SlotRenderInfo}.
 */
public final class SlotRenderInfo {

    public static final SlotRenderInfo EMPTY = new SlotRenderInfo(SlotType.EMPTY, null);

    private final SlotType type;
    private final String text;

    public SlotRenderInfo(SlotType type, String text) {
        this.type = type;
        this.text = text;
    }

    public SlotType getType() {
        return type;
    }

    /** The label drawn in the middle of the cell, or {@code null} for none. */
    public String getText() {
        return text;
    }
}
