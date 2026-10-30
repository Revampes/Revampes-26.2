package xyz.revampes.Revamp.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.revampes.Revamp.modules.ModuleManager;
import xyz.revampes.Revamp.modules.impl.dungeon.BetterTerminal;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void onExtractTransparentBackground(CallbackInfo ci) {
        if (ModuleManager.noBlur.isEnabled()) {
            ci.cancel();
        }
    }

    /**
     * Refreshes the Better Terminal state before anything else of the screen is drawn.
     *
     * <p>{@code extractRenderStateWithTooltipAndSubtitles} runs {@code extractBackground} and
     * then {@code extractRenderState}, so this is the only point that is early enough for the
     * background mixin to know whether the chest should be hidden.
     */
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
    private void frosty$refreshTerminal(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BetterTerminal module = ModuleManager.betterTerminal;
        if (module == null) return;

        AbstractContainerScreen<?> container =
                (Object) this instanceof AbstractContainerScreen<?> screen ? screen : null;
        module.refresh(container);
    }
}
