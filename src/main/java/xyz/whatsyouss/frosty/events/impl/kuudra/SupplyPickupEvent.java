package xyz.whatsyouss.frosty.events.impl.kuudra;

import xyz.whatsyouss.frosty.utility.kuudra.SupplyPosition;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.skyblock.supply.SupplyPickupEvent).
 * The IQAddons event also carried a PreSpot, which the consuming module never reads;
 * it is omitted here.
 */
public record SupplyPickupEvent(
        SupplyPosition position,
        long pickupAt
) {
}
