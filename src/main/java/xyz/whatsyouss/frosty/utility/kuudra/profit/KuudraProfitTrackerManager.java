package xyz.whatsyouss.frosty.utility.kuudra.profit;

import xyz.whatsyouss.frosty.utility.MessageUtil;

public final class KuudraProfitTrackerManager {

    private static final long SESSION_WARNING_INTERVAL_MILLIS = 5L * 60L * 1000L;
    private static final KuudraProfitTrackerManager INSTANCE = new KuudraProfitTrackerManager();

    private final KuudraProfitStore store = KuudraProfitStore.get();

    private volatile ProfitData lifetime;
    private volatile ProfitData session;
    private volatile long lastKuudraActivityAt;
    private volatile long lastSessionWarningAt;

    private KuudraProfitTrackerManager() {
        lifetime = store.lifetime;
        session = store.session;
        lastKuudraActivityAt = store.lastActivityAt;

        if (isSessionExpired()) {
            session = new ProfitData();
            store.session = session;
            save();
        }
    }

    public static KuudraProfitTrackerManager get() {
        return INSTANCE;
    }

    public synchronized void onRunEnd(long runMillis, boolean failed) {
        expireSessionIfNeeded();

        long safeRunMillis = Math.max(0L, runMillis);
        updateRun(lifetime, safeRunMillis, failed);
        updateRun(session, safeRunMillis, failed);
        lastKuudraActivityAt = System.currentTimeMillis();
        lastSessionWarningAt = 0L;

        save();
    }

    public synchronized void onChestBought(ChestData chest) {
        expireSessionIfNeeded();

        updateChest(lifetime, chest);
        updateChest(session, chest);
        lastKuudraActivityAt = System.currentTimeMillis();
        lastSessionWarningAt = 0L;
        save();
    }

    public synchronized void onReroll(boolean shard, long rerollCost) {
        expireSessionIfNeeded();

        updateReroll(lifetime, shard, rerollCost);
        updateReroll(session, shard, rerollCost);
        lastKuudraActivityAt = System.currentTimeMillis();
        lastSessionWarningAt = 0L;
        save();
    }

    public synchronized void resetSession() {
        session = new ProfitData();
        store.session = session;
        lastKuudraActivityAt = 0L;
        lastSessionWarningAt = 0L;
        save();
    }

    public synchronized void resetLifetime() {
        lifetime = new ProfitData();
        store.lifetime = lifetime;
        save();
    }

    public ProfitData lifetime() {
        return lifetime.copy();
    }

    public ProfitData session() {
        return session.copy();
    }

    public ProfitData current() {
        return store.scope() == ProfitScope.SESSION ? session() : lifetime();
    }

    public ProfitScope scope() {
        return store.scope();
    }

    public synchronized void setScope(ProfitScope scope) {
        store.setScope(scope);
        save();
    }

    public synchronized ProfitScope toggleScope() {
        ProfitScope next = store.scope() == ProfitScope.SESSION ? ProfitScope.LIFETIME : ProfitScope.SESSION;
        store.setScope(next);
        save();
        return next;
    }

    public synchronized void expireSessionIfNeeded() {
        if (lastKuudraActivityAt <= 0L) return;

        long timeSinceActivityMs = System.currentTimeMillis() - lastKuudraActivityAt;
        long sessionTimeoutMillis = sessionTimeoutMillis();

        if (timeSinceActivityMs > sessionTimeoutMillis) {
            session = new ProfitData();
            store.session = session;
            lastKuudraActivityAt = 0L;
            lastSessionWarningAt = 0L;
            save();

            MessageUtil.sendFormattedMessage("§8[§ePROFIT TRACKER§8] §fSession data has been reset after "
                    + sessionResetMinutes() + " minutes of inactivity.");
            return;
        }

        if (store.scope() == ProfitScope.SESSION && timeSinceActivityMs > SESSION_WARNING_INTERVAL_MILLIS) {
            long timeSinceWarningMs = System.currentTimeMillis() - lastSessionWarningAt;
            if (lastSessionWarningAt == 0L || timeSinceWarningMs >= SESSION_WARNING_INTERVAL_MILLIS) {
                lastSessionWarningAt = System.currentTimeMillis();
                long minutesSinceActivity = timeSinceActivityMs / (60 * 1000);
                long minutesUntilReset = (sessionTimeoutMillis - timeSinceActivityMs + 60L * 1000L - 1L) / (60L * 1000L);

                MessageUtil.sendFormattedMessage("§8[§ePROFIT TRACKER§8] §fNo runs in " + minutesSinceActivity
                        + " minutes. Session resets in " + minutesUntilReset + " minutes.");
            }
        }
    }

    private void updateRun(ProfitData data, long runMillis, boolean failed) {
        data.runs++;
        if (failed) data.failedRuns++;

        if (runMillis > 0) {
            data.totalRunMillis += runMillis;
            if (!failed && (data.bestRunMillis <= 0 || runMillis < data.bestRunMillis)) {
                data.bestRunMillis = runMillis;
            }
        }
    }

    private void updateChest(ProfitData data, ChestData record) {
        data.chestsOpened++;
        if (record.type() == ChestType.PAID) {
            data.paidChests++;
        } else {
            data.freeChests++;
        }

        data.grossCoins += record.grossValue();
        data.profit += record.netProfit();
        data.keyCostCoins += record.keyCost();
        data.pricedItems += Math.max(0, record.pricedItems());
        data.essence += Math.max(0, record.essence());
    }

    private void updateReroll(ProfitData data, boolean shard, long rerollCost) {
        if (shard) {
            data.shardRerolls++;
        } else {
            data.rerolls++;
        }

        data.rerollCostCoins += Math.max(0L, rerollCost);
        data.profit -= Math.max(0L, rerollCost);
    }

    private boolean isSessionExpired() {
        return lastKuudraActivityAt > 0L && (System.currentTimeMillis() - lastKuudraActivityAt) > sessionTimeoutMillis();
    }

    private long sessionTimeoutMillis() {
        return sessionResetMinutes() * 60L * 1000L;
    }

    private int sessionResetMinutes() {
        return Math.max(5, Math.min(120, ProfitTrackerConfig.sessionResetMinutes));
    }

    private synchronized void save() {
        store.lifetime = lifetime;
        store.session = session;
        store.lastActivityAt = lastKuudraActivityAt;
        store.save();
    }
}
