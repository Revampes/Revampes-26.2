package xyz.whatsyouss.frosty.utility.kuudra.profit;

public final class ProfitTrackerConfig {

    public enum BazaarPricingMode {
        INSTANT_SELL, SELL_ORDER
    }

    public enum ArmorValueType {
        SALVAGE, LOWEST_BIN
    }

    public enum ProfitTrackerVisibility {
        ALWAYS, KUUDRA_AREAS
    }

    public static int sessionResetMinutes = 20;
    public static ProfitTrackerVisibility profitTrackerVisibility = ProfitTrackerVisibility.KUUDRA_AREAS;
    public static BazaarPricingMode bazaarPricingMode = BazaarPricingMode.SELL_ORDER;
    public static CrimsonFaction crimsonIsleFaction = CrimsonFaction.MAGE;
    public static ArmorValueType armorValueType = ArmorValueType.SALVAGE;
    public static int kuudraPetBonus = 20;
    public static double attributeBonus = 0.0;
    public static boolean hideWidgetDuringRunPhase = false;

    private ProfitTrackerConfig() {
    }
}
