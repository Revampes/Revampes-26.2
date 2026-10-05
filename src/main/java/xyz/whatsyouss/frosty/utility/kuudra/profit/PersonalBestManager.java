package xyz.whatsyouss.frosty.utility.kuudra.profit;

import xyz.whatsyouss.frosty.utility.kuudra.KuudraTier;

public final class PersonalBestManager {

    private static final PersonalBestManager INSTANCE = new PersonalBestManager();

    private final KuudraProfitStore store = KuudraProfitStore.get();

    private PersonalBestManager() {
    }

    public static PersonalBestManager get() {
        return INSTANCE;
    }

    public synchronized void updatePersonalBest(long totalMillis, KuudraTier tier) {
        if (totalMillis <= 0L) return;
        if (store.bestTimeMillis > 0L && totalMillis >= store.bestTimeMillis) return;

        store.bestTimeMillis = totalMillis;
        store.bestTier = (tier == null ? KuudraTier.UNKNOWN : tier).name();
        store.save();
    }

    public long getBestTimeMillis() {
        return store.bestTimeMillis;
    }

    public KuudraTier getTier() {
        try {
            return KuudraTier.valueOf(store.bestTier);
        } catch (IllegalArgumentException e) {
            return KuudraTier.UNKNOWN;
        }
    }

    public boolean hasPersonalBest() {
        return store.bestTimeMillis > 0L;
    }
}
