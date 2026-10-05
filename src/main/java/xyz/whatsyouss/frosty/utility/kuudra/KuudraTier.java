package xyz.whatsyouss.frosty.utility.kuudra;

import java.util.Locale;
import java.util.Optional;

/**
 * Ported from IQAddons (net.iqaddons.mod.model.kuudra.KuudraTier).
 * Tier of the current Kuudra run, detected from the scoreboard area line ("(T1)".."(T5)").
 */
public enum KuudraTier {

    UNKNOWN(0, "Unknown"),
    BASIC(1, "Basic"),
    HOT(2, "Hot"),
    BURNING(3, "Burning"),
    FIERY(4, "Fiery"),
    INFERNAL(5, "Infernal");

    private final int level;
    private final String displayName;

    KuudraTier(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAssetCode() {
        return displayName.toLowerCase(Locale.ROOT);
    }

    public static Optional<KuudraTier> fromLevel(int level) {
        for (KuudraTier tier : values()) {
            if (tier.level == level) {
                return Optional.of(tier);
            }
        }

        return Optional.empty();
    }
}
