package xyz.revampes.Revamp.events.impl.kuudra;

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
