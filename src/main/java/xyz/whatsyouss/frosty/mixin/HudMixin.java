package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.Render2DEvent;
import xyz.whatsyouss.frosty.events.impl.TitleReceivedEvent;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(Hud.class)
public abstract class HudMixin {

    @Shadow
    private Component title;
    @Shadow
    private Component subtitle;

    @Unique
    private String frosty$lastTitleMessage = "";
    @Unique
    private String frosty$lastSubtitleMessage = "";

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onExtractScoreboardSidebar(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
        if (ModuleManager.noHudElement.isEnabled() && ModuleManager.noHudElement.scoreboard.isToggled()) ci.cancel();
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onExtractScoreboardSidebar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ModuleManager.noHudElement.isEnabled() && ModuleManager.noHudElement.scoreboard.isToggled()) ci.cancel();
    }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true)
    private void onExtractTitle(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ModuleManager.noHudElement.isEnabled() && ModuleManager.noHudElement.title.isToggled()) ci.cancel();
    }

    /**
     * Reports the currently displayed title/subtitle (deduplicated) so trackers such as
     * the Kuudra supply progress can read it, matching IQAddons' InGameHudMixin behaviour.
     */
    @Inject(method = "extractTitle", at = @At("HEAD"))
    private void onExtractTitleEvent(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        boolean hasTitle = title != null && !title.getString().isEmpty();
        boolean hasSubtitle = subtitle != null && !subtitle.getString().isEmpty();
        if (!hasTitle && !hasSubtitle) {
            frosty$lastTitleMessage = "";
            frosty$lastSubtitleMessage = "";
            return;
        }

        String currentTitleMessage = title == null ? "" : title.getString();
        String currentSubtitleMessage = subtitle == null ? "" : subtitle.getString();
        if (currentTitleMessage.equals(frosty$lastTitleMessage)
                && currentSubtitleMessage.equals(frosty$lastSubtitleMessage)) {
            return;
        }

        frosty$lastTitleMessage = currentTitleMessage;
        frosty$lastSubtitleMessage = currentSubtitleMessage;

        Frosty.EVENT_BUS.post(new TitleReceivedEvent(title, subtitle));
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void onExtractNausea(GuiGraphicsExtractor graphics, float strength, CallbackInfo ci) {
        if (ModuleManager.antiDebuff.isEnabled() && ModuleManager.antiDebuff.nausea.isToggled()) ci.cancel();
    }

    @Inject(method = "onDisconnected", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V"), cancellable = true)
    private void onClear(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "extractTabList(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V")
    private void onRenderPlayerList(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        var mc = Minecraft.getInstance();

        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        Frosty.EVENT_BUS.post(Render2DEvent.get(context, context.guiWidth(),
                context.guiHeight(), tickDelta));
    }
}
