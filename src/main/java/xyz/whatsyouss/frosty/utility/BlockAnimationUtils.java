package xyz.whatsyouss.frosty.utility;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Low level helpers for the sword block animation.
 *
 * <p>Vanilla rendered a blocking item in first person with
 * {@code ItemRenderer#transformFirstPersonItem(equipProgress, swingProgress)} followed by
 * {@code ItemRenderer#doBlockTransformations()}:
 *
 * <pre>
 *   translate(-0.5F, 0.2F, 0.0F);
 *   rotate(30.0F,  0, 1, 0);
 *   rotate(-80.0F, 1, 0, 0);
 *   rotate(60.0F,  0, 1, 0);
 * </pre>
 *
 * <p>26.2 still ships that exact pose as the {@code ItemUseAnimation.BLOCK}
 * branch of {@code ItemInHandRenderer#submitArmWithItem} for every item that is
 * not a shield, expressed in the modern render pipeline. As a translation the
 * chain bakes down to {@code (-0.2 / sqrt(2), 0.08, +0.2 / sqrt(2))} and
 * the rotation down to {@code X(-102.25) * Y(13.365) * Z(78.05)}, which is why
 * the numbers below are the vanilla 26.2 ones: they are the pre-1.9 pose, already
 * adapted to the modern item display transforms. The swing progress of
 * {@code transformFirstPersonItem} is supplied separately by the caller (1.7 fed
 * the live value, 1.8.9 passed {@code 0.0F}).
 */
@Environment(EnvType.CLIENT)
public final class BlockAnimationUtils {
    private BlockAnimationUtils() {}

    /** {@code ItemInHandRenderer#applyItemArmTransform} is private, the mixin accessor is used instead. */
    public static final float BLOCK_TRANSLATE_X = -0.14142136F;
    public static final float BLOCK_TRANSLATE_Y = 0.08F;
    public static final float BLOCK_TRANSLATE_Z = 0.14142136F;
    public static final float BLOCK_ROTATE_X = -102.25F;
    public static final float BLOCK_ROTATE_Y = 13.365F;
    public static final float BLOCK_ROTATE_Z = 78.05F;

    /**
     * Applies 1.8.9's {@code doBlockTransformations()} on top of the equip
     * transform, exactly like the vanilla 26.2 {@code ItemUseAnimation.BLOCK}
     * branch does.
     */
    public static void applyFirstPersonBlockTransform(PoseStack poseStack, HumanoidArm arm) {
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(invert * BLOCK_TRANSLATE_X, BLOCK_TRANSLATE_Y, BLOCK_TRANSLATE_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(BLOCK_ROTATE_X));
        poseStack.mulPose(Axis.YP.rotationDegrees(invert * BLOCK_ROTATE_Y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(invert * BLOCK_ROTATE_Z));
    }

    /** 1.8.9 started the block pose on right click ({@code setItemInUse}). */
    public static boolean isPlayerRightClicking() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return false;
        return mc.options.keyUse.isDown();
    }

    public static boolean isSword(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(ItemTags.SWORDS);
    }

    /** The hand that holds a sword and therefore should block, {@code null} if none does. */
    public static InteractionHand getSwordHand(Player player) {
        if (player == null) return null;
        if (isSword(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (isSword(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        return null;
    }

    public static HumanoidArm getArm(Player player, InteractionHand hand) {
        HumanoidArm mainArm = player.getMainArm();
        return hand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
    }
}
