package xyz.whatsyouss.frosty.utility.kuudra.profit;

public final class ProfitData {

    public long runs;
    public long failedRuns;
    public long totalRunMillis;
    public long bestRunMillis;

    public long chestsOpened;
    public long paidChests;
    public long freeChests;

    public long rerolls;
    public long shardRerolls;

    public long grossCoins;
    public long profit;
    public long keyCostCoins;
    public long rerollCostCoins;

    public long pricedItems;
    public long essence;

    public ProfitData() {
    }

    public long averageRunMillis() {
        if (runs <= 0) return 0;
        return totalRunMillis / runs;
    }

    public long completionRuns() {
        return Math.max(0L, runs - failedRuns);
    }

    public long hourlyRateCoins() {
        if (totalRunMillis <= 0) return 0L;
        return (long) (profit * (3_600_000d / totalRunMillis));
    }

    public ProfitData copy() {
        ProfitData copy = new ProfitData();
        copy.runs = runs;
        copy.failedRuns = failedRuns;
        copy.totalRunMillis = totalRunMillis;
        copy.bestRunMillis = bestRunMillis;
        copy.chestsOpened = chestsOpened;
        copy.paidChests = paidChests;
        copy.freeChests = freeChests;
        copy.rerolls = rerolls;
        copy.shardRerolls = shardRerolls;
        copy.grossCoins = grossCoins;
        copy.profit = profit;
        copy.keyCostCoins = keyCostCoins;
        copy.rerollCostCoins = rerollCostCoins;
        copy.pricedItems = pricedItems;
        copy.essence = essence;
        return copy;
    }
}
