package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.Frosty;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import xyz.whatsyouss.frosty.events.impl.GuiKeyEvents;
import xyz.whatsyouss.frosty.events.impl.KeyEvent;
import xyz.whatsyouss.frosty.utility.Input;
import xyz.whatsyouss.frosty.utility.KeyAction;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    public void onKey(long handle, int action, net.minecraft.client.input.KeyEvent event, CallbackInfo ci) {
        int modifiers = event.modifiers();
        int key = event.key();
        if (key != GLFW.GLFW_KEY_UNKNOWN) {
            if (action == GLFW.GLFW_PRESS) {
                modifiers |= Input.getModifier(key);
            } else if (action == GLFW.GLFW_RELEASE) {
                modifiers &= ~Input.getModifier(key);
            }


            if (GuiKeyEvents.canUseKeys) {
                Input.setKeyState(key, action != GLFW.GLFW_RELEASE);
                if (Frosty.EVENT_BUS.post(KeyEvent.get(key, modifiers, KeyAction.get(action))).isCancelled()) ci.cancel();
            }
        }
    }
}