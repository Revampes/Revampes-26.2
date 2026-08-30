package xyz.whatsyouss.frosty.modules.impl.render.blockanimation;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.utility.BlockAnimationUtils;
import xyz.whatsyouss.frosty.utility.Utils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

@Environment(EnvType.CLIENT)
public class BlockAnimation extends Module {
    public BlockAnimation() {
        super("BlockAnimation", category.Render);
    }

    private static boolean isSwinging = false;
    private static InteractionHand swingInteractionHand = InteractionHand.MAIN_HAND;
    private static int swingTime = 0;
    private static final int SWING_DURATION = 6;

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return;
        updateSwing();
    }

    public static void startSwing(InteractionHand hand) {
        if (!ModuleManager.blockAnimation.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (!Utils.nullCheck()) return;
        if (!BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) return;

        isSwinging = true;
        swingInteractionHand = hand;
        swingTime = 0;
    }

    public static void updateSwing() {
        if (!isSwinging) return;
        swingTime++;
        if (swingTime >= SWING_DURATION) {
            isSwinging = false;
            swingTime = 0;
        }
    }

    public static float getSwingProgress(float partialTicks) {
        if (!isSwinging) return 0.0f;
        return (float)(swingTime + partialTicks) / SWING_DURATION;
    }

    public static boolean isSwinging() {
        return isSwinging;
    }

    public static InteractionHand getSwingInteractionHand() {
        return swingInteractionHand;
    }
}
