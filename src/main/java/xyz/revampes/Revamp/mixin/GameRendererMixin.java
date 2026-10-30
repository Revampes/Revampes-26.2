package xyz.revampes.Revamp.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.Profiler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.revampes.Revamp.Frosty;
import xyz.revampes.Revamp.events.impl.Render3DEvent;
import xyz.revampes.Revamp.events.impl.RenderAfterWorldEvent;
import xyz.revampes.Revamp.gui.GlassRenderer;
import xyz.revampes.Revamp.modules.ModuleManager;
import xyz.revampes.Revamp.utility.Utils;

import static xyz.revampes.Revamp.Frosty.mc;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Shadow
    @Final
    public ProjectionMatrixBuffer levelProjectionMatrixBuffer;

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F", ordinal = 0))
    private float applyCameraTransformationsMathHelperLerpProxy(float original) {
        if (ModuleManager.antiDebuff.isEnabled() && ModuleManager.antiDebuff.nausea.isToggled()) {
            return 0.0f;
        }
        return original;
    }

    @ModifyVariable(method = "bobHurt", at = @At(value = "STORE"), name = "tiltAmount")
    private float modifyFinalTiltAmount(float originalAmount) {
        if (ModuleManager.noHurtCam.isEnabled()) {
            return (float) (originalAmount * (ModuleManager.noHurtCam.multiplier.getInput() / 14.0F));
        }
        return originalAmount;
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderAfterWorld(CallbackInfo info) {
        Frosty.EVENT_BUS.post(RenderAfterWorldEvent.get());
    }

    @Inject(method = "extract", at = @At("HEAD"))
    private void frosty$beginGlassFrame(DeltaTracker deltaTracker, boolean tick, CallbackInfo info) {
        GlassRenderer.beginFrame();
    }

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void frosty$renderGlass(CallbackInfo info) {
        if (GlassRenderer.render(this.minecraft)) {
            info.cancel();
        }
    }
}
