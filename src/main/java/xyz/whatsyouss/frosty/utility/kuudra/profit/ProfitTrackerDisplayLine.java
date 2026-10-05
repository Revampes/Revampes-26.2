package xyz.whatsyouss.frosty.utility.kuudra.profit;

public enum ProfitTrackerDisplayLine {

    PROFIT("profit"),
    RUNS("runs"),
    CHESTS("chests"),
    REROLLS("rerolls"),
    AVG_TIME("avgTime"),
    BEST_TIME("bestTime"),
    TIME("time"),
    RATE("rate"),
    TRACKING("tracking");

    private final String id;

    ProfitTrackerDisplayLine(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ProfitTrackerDisplayLine fromId(String id) {
        if (id == null || id.isBlank()) return null;

        for (ProfitTrackerDisplayLine line : values()) {
            if (line.id.equalsIgnoreCase(id) || line.name().equalsIgnoreCase(id)) {
                return line;
            }
        }
        return null;
    }
}
