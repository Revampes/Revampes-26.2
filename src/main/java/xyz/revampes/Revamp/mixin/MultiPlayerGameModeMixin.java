package xyz.revampes.Revamp.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.revampes.Revamp.Frosty;
import xyz.revampes.Revamp.events.impl.BreakBlockEvent;
import xyz.revampes.Revamp.events.impl.ItemUseEvent;
import xyz.revampes.Revamp.events.impl.StartBreakingBlockEvent;
import xyz.revampes.Revamp.modules.ModuleManager;
import xyz.revampes.Revamp.utility.Utils;

import static xyz.revampes.Revamp.Frosty.mc;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (Frosty.EVENT_BUS.post(new ItemUseEvent(hand, stack.copy())).isCancelled()) {
            cir.cancel();
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack useStack = player.getActiveItem();
        if (Frosty.EVENT_BUS.post(new ItemUseEvent(hand, useStack.copy())).isCancelled()) {
            cir.cancel();
            return;
        }

        if (!Utils.nullCheck() || !ModuleManager.stopPlacement.isEnabled()) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);

        if (ModuleManager.stopPlacement.isPlaceable(stack.getItem())) {
            mc.getConnection().send(new ServerboundUseItemOnPacket(hand, hitResult, 0));
            if (ModuleManager.stopPlacement.swing.isToggled()) {
                mc.player.swing(hand);
            }
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (Frosty.EVENT_BUS.post(StartBreakingBlockEvent.get(pos, direction)).isCancelled()) {
            cir.cancel();
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onBreakBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (Frosty.EVENT_BUS.post(BreakBlockEvent.get(blockPos)).isCancelled()) {
            cir.setReturnValue(false);
        }
    }
}
