package xyz.whatsyouss.frosty.events.impl.kuudra;

import net.minecraft.world.inventory.Slot;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ChestType;

import java.util.List;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.skyblock.KuudraChestOpenEvent).
 */
public record KuudraChestOpenEvent(
        int windowId,
        String title,
        List<Slot> slots,
        ChestType chestType
) {
}
