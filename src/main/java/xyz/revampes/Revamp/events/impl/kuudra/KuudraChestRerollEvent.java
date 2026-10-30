package xyz.revampes.Revamp.events.impl.kuudra;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.skyblock.KuudraChestRerollEvent).
 */
public record KuudraChestRerollEvent(int windowId, RerollType rerollType) {

    public enum RerollType {
        ITEMS,
        SHARD
    }
}
