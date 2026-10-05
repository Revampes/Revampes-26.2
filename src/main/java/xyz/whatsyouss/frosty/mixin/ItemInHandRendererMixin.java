package xyz.whatsyouss.frosty.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.mixin.accessor.ItemInHandRendererAccessor;
import xyz.whatsyouss.frosty.modules.impl.render.blockanimation.BlockAnimation;
import xyz.whatsyouss.frosty.utility.BlockAnimationUtils;

/**
 * First person half of the block animation.
 *
 * <p>{@code ItemInHandRenderer#submitArmWithItem} is 26.2's
 * {@code ItemRenderer#renderItemInFirstPerson}. When the local player blocks with a
 * sword we reproduce the pre-1.9 branch with the sword instead of a shield:
 *
 * <pre>
 *   applyItemArmTransform(poseStack, arm, equipProgress);  // 1.8.9 transformFirstPersonItem(...)
 *   applyItemArmAttackTransform(poseStack, arm, swing);    // the 1.7 block hit swing
 *   transformSwordBlockingPosition(poseStack, arm);        // 1.8.9 doBlockTransformations()
 *   renderItem(...)                                        // 1.8.9 renderItem(FIRST_PERSON)
 * </pre>
 *
 * <p>1.7 passed the live swing progress into the blocking pose, so hitting while
 * holding right click made the sword swing. 1.8 froze the pose by passing 0.0F.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(
            method = "submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void frosty$renderBlockingSword(
            AbstractClientPlayer player,
            float frameInterp,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        if (!BlockAnimation.shouldRenderFirstPersonBlock(player, hand, itemStack)) return;

        HumanoidArm arm = BlockAnimationUtils.getArm(player, hand);
        boolean rightArm = arm == HumanoidArm.RIGHT;

        poseStack.pushPose();
        ((ItemInHandRendererAccessor) this).frosty$applyItemArmTransform(poseStack, arm, inverseArmHeight);
        ((ItemInHandRendererAccessor) this).frosty$applyItemArmAttackTransform(poseStack, arm, attack);
        BlockAnimationUtils.applyFirstPersonBlockTransform(poseStack, arm);
        ((ItemInHandRenderer) (Object) this).renderItem(
                player,
                itemStack,
                rightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                poseStack,
                submitNodeCollector,
                lightCoords
        );
        poseStack.popPose();

        ci.cancel();
    }
}
