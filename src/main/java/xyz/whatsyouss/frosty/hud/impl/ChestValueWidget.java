package xyz.whatsyouss.frosty.hud.impl;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import xyz.whatsyouss.frosty.hud.HudWidget;
import xyz.whatsyouss.frosty.modules.impl.kuudra.KuudraProfit;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ChestItemValue;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ChestProfitUtil;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ChestType;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ChestValueBreakdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ported from IQAddons (features/widgets/ChestValueWidget). While a paid / free Kuudra
 * chest reward window is open it lists the total value, the profit after the key cost and
 * every priced reward, so the chest can be judged before buying it.
 */
public class ChestValueWidget extends HudWidget {

    private static final int REFRESH_INTERVAL_TICKS = 20;

    private final KuudraProfit module;

    private final List<String> displayLines = new ArrayList<>();
    private int tickCounter = 0;
    private String lastScreenTitle = "";

    public ChestValueWidget(KuudraProfit module) {
        super("chestValueWidget", "Chest Value", 148.0f, 170.0f, 1.0f);
        this.module = module;
    }

    @Override
    public void applyDefaultPosition(Font font) {
        setPosition(10.0f, Math.max(20.0f, mc.getWindow().getGuiScaledHeight() / 2.0f - 60.0f));
        setScale(1.0f);
    }

    @Override
    public boolean isVisible() {
        return module.isEnabled() && isChestScreenOpen() && !displayLines.isEmpty();
    }

    @Override
    public List<String> lines() {
        return displayLines;
    }

    @Override
    protected List<String> exampleLines() {
        return List.of(
                "§7Total Value: §612.50m",
                "§7Profit: §a+9.83m",
                "",
                "§5Molten Bracelet§f: §6720.0k",
                "§dCrimson Essence §8x2000§f: §62.54m"
        );
    }

    @Override
    public void tick() {
        String screenTitle = currentChestTitle();
        if (!screenTitle.equals(lastScreenTitle)) {
            lastScreenTitle = screenTitle;
            tickCounter = 0;
            updateFromCurrentChest();
            return;
        }

        tickCounter++;
        if (tickCounter < REFRESH_INTERVAL_TICKS) return;

        tickCounter = 0;
        updateFromCurrentChest();
    }

    private String currentChestTitle() {
        if (!(mc.gui.screen() instanceof ContainerScreen screen)) return "";

        String title = screen.getTitle().getString();
        return ChestType.fromString(title) == ChestType.UNKNOWN ? "" : title;
    }

    private void updateFromCurrentChest() {
        displayLines.clear();

        if (!(mc.gui.screen() instanceof ContainerScreen screen)) return;

        ChestValueBreakdown breakdown = ChestProfitUtil.analyzeChest(screen.getMenu().slots);
        if (breakdown.totalValue() <= 0.0 && breakdown.items().isEmpty()) return;

        displayLines.add("§7Total Value: §6" + formatCoins(breakdown.totalValue()));
        displayLines.add("§7Profit: " + (breakdown.profit() >= 0 ? "§a+" : "§c-")
                + formatCoins(Math.abs(breakdown.profit())));
        displayLines.add("");

        for (ChestItemValue item : breakdown.items()) {
            displayLines.add(item.displayName().getString()
                    + (item.count() > 1 ? " §8x" + item.count() : "")
                    + "§f: §6" + formatCoins(item.value()));
        }
    }

    private boolean isChestScreenOpen() {
        if (!(mc.gui.screen() instanceof ContainerScreen screen)) return false;

        return ChestType.fromString(screen.getTitle().getString()) != ChestType.UNKNOWN;
    }

    private String formatCoins(double value) {
        if (value >= 1_000_000_000d) return String.format(Locale.ROOT, "%.2fb", value / 1_000_000_000d);
        if (value >= 1_000_000d) return String.format(Locale.ROOT, "%.2fm", value / 1_000_000d);
        if (value >= 1_000d) return String.format(Locale.ROOT, "%.1fk", value / 1_000d);
        return String.format(Locale.ROOT, "%.0f", value);
    }
}
