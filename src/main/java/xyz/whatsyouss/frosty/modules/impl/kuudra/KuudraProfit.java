package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.kuudra.KuudraChestOpenEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.KuudraChestRerollEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.KuudraRunEndEvent;
import xyz.whatsyouss.frosty.hud.HudManager;
import xyz.whatsyouss.frosty.hud.impl.ChestValueWidget;
import xyz.whatsyouss.frosty.hud.impl.KuudraProfitTrackerWidget;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ChestData;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ChestProfitUtil;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ChestType;
import xyz.whatsyouss.frosty.utility.kuudra.profit.CrimsonFaction;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ItemPriceManager;
import xyz.whatsyouss.frosty.utility.kuudra.profit.KuudraProfitTrackerManager;
import xyz.whatsyouss.frosty.utility.kuudra.profit.PersonalBestManager;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ProfitScope;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ProfitTrackerConfig;

/**
 * Ported from IQAddons (features/kuudra/tracker/KuudraProfitTrackerFeature) plus its
 * "Profit Tracker" HUD widget. Runs are timed from the Kuudra state manager, chests are
 * valued by parsing the reward window against bazaar / lowest BIN prices.
 */
public class KuudraProfit extends Module {

    private final KuudraProfitTrackerManager manager = KuudraProfitTrackerManager.get();
    private final ItemPriceManager priceCache = ItemPriceManager.get();
    private final PersonalBestManager personalBestManager = PersonalBestManager.get();

    private final ButtonSetting lifetimeScope = new ButtonSetting("Lifetime Scope", false);
    private final SelectSetting visibility = new SelectSetting("Visibility", 1, new String[]{"Kuudra Areas", "Always"});
    private final ButtonSetting hideDuringRun = new ButtonSetting("Hide During Run", false);
    private final SliderSetting sessionResetMinutes = new SliderSetting("Session Reset", 20, 5, 120, 1);
    private final SelectSetting pricing = new SelectSetting("Bazaar Pricing", 1, new String[]{"Instant Sell", "Sell Order"});
    private final SelectSetting armorValue = new SelectSetting("Armor Value", 0, new String[]{"Salvage", "Lowest Bin"});
    private final SelectSetting faction = new SelectSetting("Faction", 0, new String[]{"Mage", "Barbarian"});
    private final SliderSetting petBonus = new SliderSetting("Kuudra Pet Bonus", 20, 0, 100, 1);
    private final SliderSetting attributeBonus = new SliderSetting("Attribute Bonus", 0, 0, 100, 1);
    private final ButtonSetting resetSession = new ButtonSetting("Reset Session", this::resetSession);
    private final ButtonSetting resetLifetime = new ButtonSetting("Reset Lifetime", this::resetLifetime);

    private int tickCounter = 0;

    public KuudraProfit() {
        super("Kuudra Profit", category.Kuudra);
        this.defaultEnabled = true;
        this.registerSetting(lifetimeScope);
        this.registerSetting(visibility);
        this.registerSetting(hideDuringRun);
        this.registerSetting(sessionResetMinutes);
        this.registerSetting(pricing);
        this.registerSetting(armorValue);
        this.registerSetting(faction);
        this.registerSetting(petBonus);
        this.registerSetting(attributeBonus);
        this.registerSetting(resetSession);
        this.registerSetting(resetLifetime);

        HudManager.register(new KuudraProfitTrackerWidget(this));
        HudManager.register(new ChestValueWidget(this));
    }

    @Override
    public void guiUpdate() {
        syncConfig();
    }

    private void syncConfig() {
        ProfitTrackerConfig.sessionResetMinutes = (int) sessionResetMinutes.getInput();
        ProfitTrackerConfig.bazaarPricingMode = pricing.getValue() == 0
                ? ProfitTrackerConfig.BazaarPricingMode.INSTANT_SELL
                : ProfitTrackerConfig.BazaarPricingMode.SELL_ORDER;
        ProfitTrackerConfig.armorValueType = armorValue.getValue() == 0
                ? ProfitTrackerConfig.ArmorValueType.SALVAGE
                : ProfitTrackerConfig.ArmorValueType.LOWEST_BIN;
        ProfitTrackerConfig.crimsonIsleFaction = faction.getValue() == 0 ? CrimsonFaction.MAGE : CrimsonFaction.BARBARIAN;
        ProfitTrackerConfig.kuudraPetBonus = (int) petBonus.getInput();
        ProfitTrackerConfig.attributeBonus = attributeBonus.getInput();
        ProfitTrackerConfig.hideWidgetDuringRunPhase = hideDuringRun.isToggled();

        ProfitScope scope = lifetimeScope.isToggled() ? ProfitScope.LIFETIME : ProfitScope.SESSION;
        if (manager.scope() != scope) {
            manager.setScope(scope);
        }
    }

    @Override
    public void onUpdate() {
        syncConfig();

        tickCounter++;
        if (tickCounter % 20 != 0) return;

        manager.expireSessionIfNeeded();
    }

    public KuudraProfitTrackerManager getTracker() {
        return manager;
    }

    public PersonalBestManager getPersonalBestManager() {
        return personalBestManager;
    }

    public boolean isAlwaysVisible() {
        return visibility.getValue() == 1;
    }

    public boolean isHideDuringRun() {
        return hideDuringRun.isToggled();
    }

    private void resetSession() {
        manager.resetSession();
    }

    private void resetLifetime() {
        manager.resetLifetime();
    }

    @EventHandler
    public void onRunEnd(KuudraRunEndEvent event) {
        if (event.isUnexpectedlyEnded()) return;

        long totalMillis = event.totalDuration().toMillis();
        manager.onRunEnd(totalMillis, event.isFailed());

        if (event.isCompleted()) {
            personalBestManager.updatePersonalBest(totalMillis, KuudraState.get().tier());
        }
    }

    @EventHandler
    public void onChestOpen(KuudraChestOpenEvent event) {
        if (event.chestType() == ChestType.UNKNOWN) return;

        ChestData parsed = ChestProfitUtil.parseChest(event.slots(), priceCache, event.chestType());
        manager.onChestBought(parsed);
    }

    @EventHandler
    public void onChestReroll(KuudraChestRerollEvent event) {
        if (event.rerollType() == KuudraChestRerollEvent.RerollType.ITEMS) {
            manager.onReroll(false, priceCache.getKismetPrice());
        } else if (event.rerollType() == KuudraChestRerollEvent.RerollType.SHARD) {
            manager.onReroll(true, priceCache.getWheelOfFatePrice());
        }
    }
}
