package xyz.revampes.Revamp.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.revampes.Revamp.Frosty;
import xyz.revampes.Revamp.events.impl.StrafeEvent;
import xyz.revampes.Revamp.interfaces.ICameraOverriddenEntity;
import xyz.revampes.Revamp.modules.ModuleManager;
import xyz.revampes.Revamp.modules.impl.render.FreeLook;

import static xyz.revampes.Revamp.Frosty.mc;
import static xyz.revampes.Revamp.modules.ModuleManager.freeLook;

@Mixin(Entity.class)
public abstract class EntityMixin implements ICameraOverriddenEntity {
    @Unique
    private float cameraPitch;
    @Unique
    private float cameraYaw;
    @Shadow public abstract float getYRot();

    @Shadow
    public abstract boolean isSprinting();

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Shadow
    public abstract void setDeltaMovement(Vec3 deltaMovement);

    @Shadow
    public abstract void addDeltaMovement(Vec3 momentum);

    @Shadow
    public boolean needsSync;



    @ModifyExpressionValue(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"))
    public Vec3 hookVelocityStrafe(Vec3 original, @Local(argsOnly = true) Vec3 input, @Local(argsOnly = true) float speed) {

        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }

        StrafeEvent strafeEvent = new StrafeEvent(input, speed, this.getYRot());
        Frosty.EVENT_BUS.post(strafeEvent);

        if (strafeEvent.getYaw() != this.getYRot()) {
            return Entity.getInputVector(strafeEvent.getInput(), strafeEvent.getFriction(), strafeEvent.getYaw());
        }

        return original;
    }

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void updateTurn(double xo, double yo, CallbackInfo ci) {
        if (FreeLook.freelooking && (Object) this == mc.player) {
            freeLook.cameraYaw += (float) (xo / 8);
            freeLook.cameraPitch += (float) (yo / 8);

            if (Math.abs(freeLook.cameraPitch) > 90.0F)
                freeLook.cameraPitch = freeLook.cameraPitch > 0.0F ? 90.0F : -90.0F;
            ci.cancel();
        }
    }

    @Override
    @Unique
    public float frosty$getCameraPitch() {
        return this.cameraPitch;
    }

    @Override
    @Unique
    public float frosty$getCameraYaw() {
        return this.cameraYaw;
    }

    @Override
    @Unique
    public void frosty$setCameraPitch(float pitch) {
        this.cameraPitch = pitch;
    }

    @Override
    @Unique
    public void frosty$setCameraYaw(float yaw) {
        this.cameraYaw = yaw;
    }
}