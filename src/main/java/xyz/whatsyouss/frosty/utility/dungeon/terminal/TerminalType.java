package xyz.whatsyouss.frosty.utility.dungeon.terminal;

import xyz.whatsyouss.frosty.modules.impl.dungeon.BetterTerminal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The six Floor 7 terminal types plus the geometry BetterTerminal needs to draw them.
 *
 * <p>The matching and slot windows follow Odin's {@code TerminalTypes} (the 26.x solver);
 * the layout numbers ({@code lines}/{@code beginLine}/{@code chestLines} and the grid rect)
 * follow yqloss' {@code TerminalOrder}/{@code TerminalColor}/... classes so the panel is
 * laid out exactly like the 1.8.9 Better Terminal UI.
 *
 * <p>Melody is the one terminal where the two sources disagree: yqloss assumes six rows
 * (target rows 0 and 5, lanes 1-4) while Odin's 26.x layout has five (target rows 0 and 4,
 * lanes 1-3, buttons at slots 16/25/34). The Odin geometry wins here because the click
 * targets have to land on the slots the server actually uses.
 */
public enum TerminalType {

    PANES("Correct all the panes!", "^Correct all the panes!$", 45,
            4, 1, 5, 5, 3, 2, 1),

    RUBIX("Change all to same color!", "^Change all to same color!$", 45,
            4, 1, 5, 3, 3, 3, 1),

    NUMBERS("Click in order!", "^Click in order!$", 36,
            3, 1, 4, 7, 2, 1, 1),

    // Hypixel used 'X' historically and "X" after the 26.x rewrite; accept both.
    STARTS_WITH("What starts with: '*'?", "^What starts with: ['\"](\\w)['\"]\\?$", 45,
            4, 1, 5, 7, 3, 1, 1),

    SELECT("Select all the '*' items!", "^Select all the ([\\w ]+) items!$", 54,
            5, 1, 6, 7, 4, 1, 1),

    MELODY("Click the button on time!", "^Click the button on time!$", 54,
            5, 0, 6, 5, 5, 1, 0);

    private final String defaultTitle;
    private final Pattern pattern;
    /** Total container size (item rows + the player inventory copy) as Odin tracks it. */
    private final int windowSize;
    /** Row count of the drawn panel (yqloss {@code Terminal#lines}). */
    private final int lines;
    /** Rows of the panel that are header/decorative and hold no clickable slot. */
    private final int beginLine;
    /** Vanilla chest row count, used only to place the panel where the chest would be. */
    private final int chestLines;

    private final int cols;
    private final int rows;
    private final int startCol;
    private final int startRow;

    TerminalType(String defaultTitle, String regex, int windowSize,
                 int lines, int beginLine, int chestLines,
                 int cols, int rows, int startCol, int startRow) {
        this.defaultTitle = defaultTitle;
        this.pattern = Pattern.compile(regex);
        this.windowSize = windowSize;
        this.lines = lines;
        this.beginLine = beginLine;
        this.chestLines = chestLines;
        this.cols = cols;
        this.rows = rows;
        this.startCol = startCol;
        this.startRow = startRow;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public int getLines() {
        return lines;
    }

    public int getBeginLine() {
        return beginLine;
    }

    public int getChestLines() {
        return chestLines;
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    /** Number of container slots the terminal itself owns, excluding the player inventory. */
    public int usableSlots() {
        return windowSize - 9;
    }

    public Matcher matcher(String title) {
        return pattern.matcher(title);
    }

    /**
     * Container slot index of a cell in the solver grid. The grid is anchored to the real
     * nine-wide container, which is what lets a click on a drawn cell map straight back to
     * the slot the server expects.
     */
    public int slotAt(int row, int col) {
        return (startRow + row) * 9 + startCol + col;
    }

    public static TerminalType match(String title) {
        if (title == null || title.isEmpty()) return null;
        for (TerminalType type : values()) {
            if (type.pattern.matcher(title).matches()) return type;
        }
        return null;
    }

    /**
     * Builds the solver for a matched title, honouring the module's per-terminal toggles.
     * Returns {@code null} when the terminal is disabled in the config.
     */
    public TerminalHandler createHandler(BetterTerminal module, String title) {
        Matcher matcher = pattern.matcher(title);
        if (!matcher.matches()) return null;

        if (module != null && !isEnabled(module)) return null;

        switch (this) {
            case PANES:
                return new PanesTerminalHandler(module);
            case RUBIX:
                return new RubixTerminalHandler(module);
            case NUMBERS:
                return new NumbersTerminalHandler(module);
            case STARTS_WITH: {
                String letter = matcher.group(1);
                return new StartsWithTerminalHandler(module, letter == null ? "" : letter.toUpperCase());
            }
            case SELECT:
                return SelectAllTerminalHandler.create(module, matcher.group(1));
            case MELODY:
                return new MelodyTerminalHandler(module);
            default:
                return null;
        }
    }

    private boolean isEnabled(BetterTerminal module) {
        switch (this) {
            case PANES:
                return module.panesEnabled.isToggled();
            case RUBIX:
                return module.rubixEnabled.isToggled();
            case NUMBERS:
                return module.orderEnabled.isToggled();
            case STARTS_WITH:
                return module.startEnabled.isToggled();
            case SELECT:
                return module.colorEnabled.isToggled();
            case MELODY:
                return module.alignEnabled.isToggled();
            default:
                return true;
        }
    }
}
