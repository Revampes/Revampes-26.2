package xyz.revampes.Revamp.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xyz.revampes.Revamp.modules.impl.other.MoveFix;
import xyz.revampes.Revamp.utility.DirectionalInput;
import xyz.revampes.Revamp.utility.Rotations;

import static xyz.revampes.Revamp.Frosty.mc;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInputMixin {
    @Shadow @Final private Options options;

    @Unique
    private DirectionalInput frosty$getCorrectedInput() {
        if (MoveFix.shouldApply() &&  mc.player != null) {
            Rotations.updateServerRotation();

            DirectionalInput originalInput = new DirectionalInput(
                    this.options.keyUp.isDown(),
                    this.options.keyDown.isDown(),
                    this.options.keyLeft.isDown(),
                    this.options.keyRight.isDown()
            );

            return Rotations.calculateCorrectedInput(
                    originalInput,
                    mc.player.getYRot(),
                    Rotations.lastServerYaw
            );
        }

        return new DirectionalInput(
                this.options.keyUp.isDown(),
                this.options.keyDown.isDown(),
                this.options.keyLeft.isDown(),
                this.options.keyRight.isDown()
        );
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input modifyInput(Input original) {
        if (!MoveFix.shouldApply() || mc.player == null) {
            return original;
        }

        DirectionalInput corrected = frosty$getCorrectedInput();

        return new Input(
                corrected.isForwards(),
                corrected.isBackwards(),
                corrected.isLeft(),
                corrected.isRight(),
                original.jump(),
                original.shift(),
                original.sprint()
        );
    }
}
