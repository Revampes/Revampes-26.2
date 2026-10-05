package xyz.whatsyouss.frosty.events.impl.kuudra;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent).
 */
public record SupplyPlaceEvent(
        String originalMessage,
        String playerName,
        int currentSupply,
        double placedAt
) {
}
