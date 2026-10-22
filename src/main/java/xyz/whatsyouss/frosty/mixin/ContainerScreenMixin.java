package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.dungeon.BetterTerminal;

/**
 * Better Terminal has to suppress the chest texture here rather than in
 * {@code extractContents}: 26.2 renders a screen in two steps, and
 * {@code Screen#extractRenderStateWithTooltipAndSubtitles} calls {@code extractBackground}
 * (the chest panel, via this override) before {@code extractRenderState} (which reaches
 * {@code AbstractContainerScreen#extractContents}). Cancelling only {@code extractContents}
 * leaves the chest texture on screen.
 */
@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin {

    @Unique
    private boolean frosty$backgroundScaled;

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void frosty$beforeBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        frosty$backgroundScaled = false;

        BetterTerminal module = ModuleManager.betterTerminal;
        ContainerScreen screen = (ContainerScreen) (Object) this;
        if (module == null) return;

        if (module.hidesVanillaChest(screen)) {
            ci.cancel();
            return;
        }

        float scale = module.chestDisplayScale(screen);
        if (scale == 1.0f) return;

        Matrix3x2fStack pose = context.pose();
        pose.pushMatrix();
        pose.scale(scale, scale);
        frosty$backgroundScaled = true;
    }

    @Inject(method = "extractBackground", at = @At("RETURN"))
    private void frosty$afterBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!frosty$backgroundScaled) return;
        frosty$backgroundScaled = false;
        context.pose().popMatrix();
    }
}
