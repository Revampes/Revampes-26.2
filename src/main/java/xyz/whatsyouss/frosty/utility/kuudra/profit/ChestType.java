package xyz.whatsyouss.frosty.utility.kuudra.profit;

public enum ChestType {

    FREE, PAID, UNKNOWN;

    public static ChestType fromString(String str) {
        if (str == null) return UNKNOWN;
        String lower = str.toLowerCase();
        if (lower.contains("free chest")) return FREE;
        if (lower.contains("paid chest")) return PAID;

        return UNKNOWN;
    }
}
