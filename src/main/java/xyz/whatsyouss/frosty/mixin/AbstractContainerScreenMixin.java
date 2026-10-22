package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.ContainerMouseClickEvent;
import xyz.whatsyouss.frosty.events.impl.ContainerScreenRenderEvent;
import xyz.whatsyouss.frosty.events.impl.Render2DEvent;
import xyz.whatsyouss.frosty.events.impl.ScreenClickEvent;
import xyz.whatsyouss.frosty.events.impl.ScreenDrawSlotEvent;
import xyz.whatsyouss.frosty.events.impl.ScreenKeyPressEvent;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.modules.impl.dungeon.BetterTerminal;

/**
 * Ported from IQAddons (net.iqaddons.mod.mixin.HandledScreenMixin) — the parts the
 * Croesus helper needs (slot draw overlay + container key press).
 *
 * <p>Extended for Better Terminal: the slot contents can be hidden or shrunk to a thumbnail
 * (the chest texture itself is handled in {@code ContainerScreenMixin}, because 26.2 draws it
 * from {@code extractBackground}), tooltips are muted while the custom panel is up, and mouse
 * clicks on the panel are routed through {@link ContainerMouseClickEvent} so the vanilla slot
 * click never fires.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Unique
    private static String frosty$lastLoggedSlotTitle = "";

    @Unique
    private boolean frosty$contentsScaled;

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
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        Frosty.EVENT_BUS.post(Render2DEvent.get(context, context.guiWidth(), context.guiHeight(), delta));
        Frosty.EVENT_BUS.post(new ContainerScreenRenderEvent(screen, context, mouseX, mouseY, delta));
    }

    /**
     * Mirrors {@code ContainerScreenMixin}'s background handling so the slots, labels and
     * highlights follow the same hide/scale decision as the chest texture. The terminal state
     * itself is refreshed earlier, in {@code ScreenMixin}.
     */
    @Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
    private void frosty$beforeExtractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        frosty$contentsScaled = false;

        BetterTerminal module = ModuleManager.betterTerminal;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
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
        frosty$contentsScaled = true;
    }

    @Inject(method = "extractContents", at = @At("RETURN"))
    private void frosty$afterExtractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!frosty$contentsScaled) return;
        frosty$contentsScaled = false;
        context.pose().popMatrix();
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void frosty$hideTooltips(GuiGraphicsExtractor context, int mouseX, int mouseY, CallbackInfo ci) {
        BetterTerminal module = ModuleManager.betterTerminal;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (module != null && module.isTerminalScreen(screen)) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void frosty$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        ContainerMouseClickEvent clickEvent = Frosty.EVENT_BUS.post(
                new ContainerMouseClickEvent(screen, event.x(), event.y(), event.button())
        );
        if (clickEvent.isCancelled()) {
            cir.setReturnValue(true);
        }
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
