package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.SendPacketEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;
import xyz.whatsyouss.frosty.utility.kuudra.SupplyState;

/**
 * Ported from IQAddons (features/kuudra/linus/PearlCancelFeature).
 * Cancels the first pearl throw packet after a click and re-issues it once the
 * server tick lines up, so the pearl only leaves on the intended tick.
 */
public class PearlCancel extends Module {

    private final SupplyState supplyState = SupplyState.get();

    private int ticksSincePearl = 0;

    public PearlCancel() {
        super("Pearl Cancel", category.Kuudra);
    }

    @Override
    public void onDisable() {
        this.ticksSincePearl = 0;
    }

    @EventHandler
    public void onClientTick(PreUpdateEvent event) {
        this.ticksSincePearl++;
        if (mc.player == null) return;

        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() != Items.ENDER_PEARL
                || supplyState.getCurrentSupplyProgress() <= 0
                || KuudraState.get().phase() != KuudraPhase.SUPPLIES) return;
        if (!mc.options.keyUse.isDown()) return;
        if (mc.player.getXRot() < 82.0F) return;
        if (this.ticksSincePearl >= 2) {
            throwPearl();
        }
    }

    @EventHandler
    public void onPacketSend(SendPacketEvent event) {
        if (event.getPacket() instanceof ServerboundUseItemPacket) {
            if (mc.player == null) return;
            ItemStack held = mc.player.getMainHandItem();
            if (held.getItem() != Items.ENDER_PEARL) return;
            if (this.ticksSincePearl == 0) event.setCancelled(true);
            this.ticksSincePearl = 0;
            return;
        }

        if (event.getPacket() instanceof ServerboundUseItemOnPacket) {
            if (mc.player == null) return;
            ItemStack held = mc.player.getMainHandItem();
            if (held.getItem() != Items.ENDER_PEARL
                    || supplyState.getCurrentSupplyProgress() <= 0
                    || KuudraState.get().phase() != KuudraPhase.SUPPLIES) return;
            event.setCancelled(true);
        }
    }

    private void throwPearl() {
        if (mc.player == null) return;

        mc.player.connection.send(
                new ServerboundUseItemPacket(
                        InteractionHand.MAIN_HAND,
                        0,
                        mc.player.getYRot(),
                        mc.player.getXRot()
                )
        );
    }
}
