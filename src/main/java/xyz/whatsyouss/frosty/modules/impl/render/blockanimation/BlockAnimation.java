package xyz.whatsyouss.frosty.modules.impl.render.blockanimation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.utility.BlockAnimationUtils;

/**
 * Mimics the 1.7 sword block animation.
 *
 * <p>In 1.7 holding right click with a sword put the item into an item use state, which
 * the renderer translated into
 * <ul>
 *     <li>first person: {@code ItemRenderer#transformFirstPersonItem(equip, swingProgress)
 *     + doBlockTransformations()}, and</li>
 *     <li>third person: {@code RenderPlayer#setModelVisibilities} sets
 *     {@code ModelBiped.heldItemRight = 3}, which lifts the arm into the block pose.</li>
 * </ul>
 *
 * <p>Because 1.9 removed sword blocking, 26.2 never puts a sword into that state and the
 * pose is never rendered. This module fakes the state client side: while the use key is
 * held with a sword in hand the held item is drawn with the vanilla block transform and
 * the player model is drawn with {@link HumanoidModel.ArmPose#BLOCK}.
 *
 * <p>1.7 is distinct from 1.8.9 in one visible way: it fed the live swing progress into
 * the blocking pose, so attacking while blocking made the sword swing ("block hit").
 * 1.8.9 froze the pose by passing a swing progress of {@code 0.0F}, which is what this
 * module replaced with the 1.7 behaviour.
 */
@Environment(EnvType.CLIENT)
public class BlockAnimation extends Module {
    public BlockAnimation() {
        super("BlockAnimation", category.Render);
    }

    @Override
    public String getDesc() {
        return "Reproduces the 1.7 sword block animation, including the block hit swing (first and third person)";
    }

    /**
     * The hand that should be blocking right now, or {@code null} when the block
     * pose must not be applied.
     */
    public static InteractionHand getBlockingHand(Player player) {
        if (ModuleManager.blockAnimation == null || !ModuleManager.blockAnimation.isEnabled()) return null;
        // Other players' right click is not observable client side, so only the
        // local player can be animated.
        if (player == null || player != mc.player) return null;
        if (!player.isAlive()) return null;
        // Real use animations (shield, bow, food, spyglass, riptide) always win,
        // just like they did in 1.8.9 where the sword was only "blocking" when it
        // was the item in use.
        if (player.isUsingItem() || player.isScoping() || player.isAutoSpinAttack()) return null;
        if (!BlockAnimationUtils.isPlayerRightClicking()) return null;
        return BlockAnimationUtils.getSwordHand(player);
    }

    /**
     * First person equivalent of the 1.8.9 {@code EnumAction.BLOCK} branch:
     * only the hand that holds the blocking sword, and only when the item that is
     * actually being drawn for that hand is a sword (handles hotbar swaps).
     */
    public static boolean shouldRenderFirstPersonBlock(Player player, InteractionHand hand, ItemStack visibleItem) {
        InteractionHand blockingHand = getBlockingHand(player);
        return blockingHand != null && blockingHand == hand && BlockAnimationUtils.isSword(visibleItem);
    }

    /**
     * Third person equivalent of 1.8.9's {@code heldItemRight = 3}: puts the arm
     * that holds the sword into {@link HumanoidModel.ArmPose#BLOCK}, which
     * {@code HumanoidModel#poseBlockingArm} resolves to
     * {@code xRot = xRot * 0.5 - 0.9424779F} (i.e. {@code -3 * PI / 10}) and
     * {@code yRot = +-30°} - the 1.8.9 block pose.
     */
    public static void applyThirdPersonBlockPose(AvatarRenderState state, Player player) {
        InteractionHand hand = getBlockingHand(player);
        if (hand == null) return;

        HumanoidArm arm = BlockAnimationUtils.getArm(player, hand);
        if (arm == HumanoidArm.RIGHT) {
            state.rightArmPose = HumanoidModel.ArmPose.BLOCK;
        } else {
            state.leftArmPose = HumanoidModel.ArmPose.BLOCK;
        }
        // 1.8.9 only applied the pose while the item was in use, mirror that so the
        // rest of the model behaves like a blocking player.
        state.isUsingItem = true;
        state.useItemHand = hand;
    }
}
