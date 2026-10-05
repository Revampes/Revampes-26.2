package xyz.whatsyouss.frosty.utility.kuudra.profit;

public enum ChestKeyType {

    FREE("", 0, 0),
    BASIC("KUUDRA_KEY", 160_000, 2),
    HOT("HOT_KUUDRA_KEY", 320_000, 4),
    BURNING("BURNING_KUUDRA_KEY", 600_000, 16),
    FIERY("FIERY_KUUDRA_KEY", 1_200_000, 40),
    INFERNAL("INFERNAL_KUUDRA_KEY", 2_400_000, 80),
    UNKNOWN("UNKNOWN", 0, 0);

    private final String itemId;
    private final long baseCoinsCost;
    private final int materialAmount;

    ChestKeyType(String itemId, long baseCoinsCost, int materialAmount) {
        this.itemId = itemId;
        this.baseCoinsCost = baseCoinsCost;
        this.materialAmount = materialAmount;
    }

    public String getItemId() {
        return itemId;
    }

    public long getBaseCoinsCost() {
        return baseCoinsCost;
    }

    public int getMaterialAmount() {
        return materialAmount;
    }

    public static ChestKeyType parseKeyType(String keyTier) {
        if (keyTier == null) return UNKNOWN;

        return switch (keyTier.toUpperCase()) {
            case "HOT" -> HOT;
            case "BURNING" -> BURNING;
            case "FIERY" -> FIERY;
            case "INFERNAL" -> INFERNAL;
            case "KUUDRA_KEY", "BASIC" -> BASIC;
            case "FREE" -> FREE;
            default -> UNKNOWN;
        };
    }
}
