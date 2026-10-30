package xyz.revampes.Revamp.utility.kuudra.profit;

import java.util.List;

public record ChestValueBreakdown(double totalValue, double keyCost, double profit, List<ChestItemValue> items) {
}
