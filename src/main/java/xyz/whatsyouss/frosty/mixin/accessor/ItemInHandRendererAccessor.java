package xyz.whatsyouss.frosty.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
    /**
     * Equip offset of the arm ({@code translate(invert * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F)}),
     * the modern counterpart of the translation inside 1.8.9's
     * {@code ItemRenderer#transformFirstPersonItem}.
     */
    @Invoker("applyItemArmTransform")
    void frosty$applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight);

    /**
     * Swing offset of the arm, the modern counterpart of the swing rotations inside
     * 1.8.9's {@code ItemRenderer#transformFirstPersonItem}. 1.7 applied it while the
     * sword was held in the blocking pose, which is the "1.7 block hit" animation.
     */
    @Invoker("applyItemArmAttackTransform")
    void frosty$applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float swingProgress);
}
