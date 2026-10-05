package xyz.whatsyouss.frosty.hud.impl;

import net.minecraft.client.gui.Font;
import xyz.whatsyouss.frosty.hud.HudWidget;
import xyz.whatsyouss.frosty.modules.impl.kuudra.KuudraProfit;
import xyz.whatsyouss.frosty.utility.LocationUtils;
import xyz.whatsyouss.frosty.utility.TimeUtils;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;
import xyz.whatsyouss.frosty.utility.kuudra.profit.KuudraProfitTrackerManager;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ProfitData;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ProfitScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Ported from IQAddons (features/widgets/KuudraProfitTrackerWidget).
 */
public class KuudraProfitTrackerWidget extends HudWidget {

    private static final Set<String> ALLOWED_AREAS = Set.of(
            "Dungeon Hub",
            "Forgotten Skull",
            "Kuudra's Hollow"
    );

    private final KuudraProfit module;
    private final KuudraProfitTrackerManager tracker = KuudraProfitTrackerManager.get();

    private String netProfit = "§fProfit: §a+0";
    private String runs = "§fRuns: §70 §8(§a0C§8/§c0F§8)";
    private String chests = "§fChests: §70 §8(§e0P§8/§a0F§8)";
    private String rerolls = "§fRerolls: §b0/0 §8(§c-0§8)";
    private String avg = "§fAvg Time: §70s";
    private String bestTime = "§fBest Time: §70s";
    private String totalTime = "§fTime: §b0s";
    private String rate = "§fRate: §70/h";
    private String tracking = "§fTracking: §aSession";

    public KuudraProfitTrackerWidget(KuudraProfit module) {
        super("kuudraProfitTracker", "Profit Tracker", 0.0f, 40.0f, 1.0f);
        this.module = module;
    }

    @Override
    public void applyDefaultPosition(Font font) {
        setPosition((mc.getWindow().getGuiScaledWidth() - getUnscaledWidth(font)) / 2.0f, 40.0f);
        setScale(1.0f);
    }

    @Override
    public boolean isVisible() {
        if (!module.isEnabled()) return false;

        if (module.isAlwaysVisible()) return true;

        String area = LocationUtils.getCurrentArea();
        if (area == null || ALLOWED_AREAS.stream().noneMatch(area::contains)) return false;

        return !(module.isHideDuringRun() && KuudraState.get().isInRun());
    }

    @Override
    public List<String> lines() {
        List<String> lines = new ArrayList<>();
        lines.add("§b§lProfit Tracker");
        lines.add(netProfit);
        lines.add(runs);
        lines.add(chests);
        lines.add(rerolls);
        lines.add(avg);
        lines.add(bestTime);
        lines.add(totalTime);
        lines.add(rate);
        lines.add(tracking);
        return lines;
    }

    @Override
    public void tick() {
        ProfitScope scope = tracker.scope();
        ProfitData data = scope == ProfitScope.LIFETIME ? tracker.lifetime() : tracker.session();

        String sign = data.profit >= 0 ? "§a+" : "§c-";
        netProfit = "§fProfit: " + sign + formatCoins(Math.abs(data.profit));

        runs = String.format("§fRuns: §7%s §8(§a%sC§8/§c%sF§8)",
                data.runs, data.completionRuns(), data.failedRuns);

        chests = String.format("§fChests: §7%s §8(§e%sP§8/§a%sF§8)",
                data.chestsOpened, data.paidChests, data.freeChests);

        rerolls = String.format("§fRerolls: §b%sC/%sS §8(§c-%s§8)",
                data.rerolls, data.shardRerolls, formatCoins(data.rerollCostCoins));

        double avgTimeSeconds = data.averageRunMillis() / 1000.0;
        avg = String.format("§fAvg Time: %s%s", getAverageTimeColor(avgTimeSeconds), TimeUtils.formatTime(avgTimeSeconds));

        long bestRunMillis = scope == ProfitScope.LIFETIME
                ? module.getPersonalBestManager().getBestTimeMillis()
                : data.bestRunMillis;
        double bestRunSeconds = bestRunMillis / 1000.0;
        bestTime = String.format("§fBest Time: %s%s",
                getAverageTimeColor(bestRunSeconds), TimeUtils.formatTime(bestRunSeconds));

        totalTime = "§fTime: §b" + TimeUtils.formatTime(data.totalRunMillis);
        rate = "§fRate: " + getRateColor(data.hourlyRateCoins()) + formatCoins(Math.max(0L, data.hourlyRateCoins())) + "/h";
        tracking = "§fTracking: §a" + (scope == ProfitScope.LIFETIME ? "Lifetime" : "Session");
    }

    private String formatCoins(long coins) {
        if (coins >= 1_000_000_000L) return String.format(Locale.ROOT, "%.2fb", coins / 1_000_000_000d);
        if (coins >= 1_000_000L) return String.format(Locale.ROOT, "%.2fm", coins / 1_000_000d);
        if (coins >= 1_000L) return String.format(Locale.ROOT, "%.1fk", coins / 1_000d);
        return String.valueOf(coins);
    }

    private String getAverageTimeColor(double avgTimeSeconds) {
        if (avgTimeSeconds <= 0.0) return "§7";
        if (avgTimeSeconds <= 50.0) return "§f";
        if (avgTimeSeconds <= 59.9) return "§5";
        if (avgTimeSeconds <= 65.0) return "§9";
        if (avgTimeSeconds <= 70.0) return "§a";
        if (avgTimeSeconds <= 75.0) return "§6";
        if (avgTimeSeconds <= 80.0) return "§c";
        return "§4";
    }

    private String getRateColor(long hourlyRateCoins) {
        if (hourlyRateCoins > 150_000_000L) return "§5";
        if (hourlyRateCoins > 100_000_000L) return "§9";
        if (hourlyRateCoins >= 80_000_000L) return "§a";
        if (hourlyRateCoins >= 60_000_000L) return "§6";
        if (hourlyRateCoins >= 40_000_000L) return "§c";
        if (hourlyRateCoins >= 10_000_000L) return "§7";
        return "§4";
    }
}
