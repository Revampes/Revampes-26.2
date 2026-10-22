package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.utility.dungeon.terminal.TerminalManager;

/**
 * Feeds Better Terminal the server's container updates <em>after</em> the client menu has
 * applied them, which is what Odin's {@code SetSlotEvent} does for its solver. Hooking the
 * packet at receive time would hand the solver a stale {@code menu.slots}.
 *
 * <p>This is only a same-tick fast path. Confirmation of queued clicks no longer depends on it:
 * {@code TerminalHandler} fingerprints the container while re-solving, so a menu mutated
 * locally (Odin's terminal simulator never sends these packets) is handled identically.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void frosty$onContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        TerminalManager.onContainerUpdate(packet.getContainerId());
    }

    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void frosty$onContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        TerminalManager.onContainerUpdate(packet.containerId());
    }
}
