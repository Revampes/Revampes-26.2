package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.Render2DEvent;
import xyz.whatsyouss.frosty.events.impl.ScreenClickEvent;
import xyz.whatsyouss.frosty.events.impl.ScreenDrawSlotEvent;
import xyz.whatsyouss.frosty.events.impl.ScreenKeyPressEvent;

/**
 * Ported from IQAddons (net.iqaddons.mod.mixin.HandledScreenMixin) — the parts the
 * Croesus helper needs (slot draw overlay + container key press).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Unique
    private static String frosty$lastLoggedSlotTitle = "";

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void frosty$onExtractSlot(GuiGraphicsExtractor context, Slot slot, int x, int y, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;

        String title = screen.getTitle().getString();
        if (!title.equals(frosty$lastLoggedSlotTitle)) {
            frosty$lastLoggedSlotTitle = title;
            Frosty.LOGGER.info("[Croesus/debug] extractSlot hook fired, screen title = '{}'", title);
        }

        Frosty.EVENT_BUS.post(new ScreenDrawSlotEvent(screen, context, slot, x, y));
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void frosty$renderHudOverContainerScreen(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Frosty.EVENT_BUS.post(Render2DEvent.get(context, context.guiWidth(), context.guiHeight(), delta));
    }

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void frosty$onSlotClicked(Slot slot, int slotId, int button, ContainerInput containerInput, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (slot != null) {
            Frosty.EVENT_BUS.post(new ScreenClickEvent(screen, slot));
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void frosty$onKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        ScreenKeyPressEvent event = Frosty.EVENT_BUS.post(
                new ScreenKeyPressEvent(screen, input.key(), input.scancode(), input.modifiers())
        );

        if (event.isCancelled()) {
            cir.setReturnValue(true);
        }
    }
}
