package xyz.whatsyouss.frosty.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import xyz.whatsyouss.frosty.events.impl.GuiKeyEvents;
import xyz.whatsyouss.frosty.mixin.accessor.KeyMappingAccessor;

public class Input {
    private static final boolean[] keys = new boolean[512];
    private static final boolean[] buttons = new boolean[16];

    private Input() {
    }

    public static void setKeyState(int key, boolean pressed) {
        if (key >= 0 && key < keys.length) keys[key] = pressed;
    }

    public static void setButtonState(int button, boolean pressed) {
        if (button >= 0 && button < buttons.length) buttons[button] = pressed;
    }

    public static int getKey(KeyMapping bind) {
        return ((KeyMappingAccessor) bind).getKey().getValue();
    }

    public static KeyMapping getKeyBindingFromCode(int keyCode) {
        for (KeyMapping keyBinding : Minecraft.getInstance().options.keyMappings) {
            if (keyBinding.getDefaultKey().getValue() == keyCode) {
                return keyBinding;
            }
        }
        return null;
    }

    public static void setKeyState(KeyMapping bind, boolean pressed) {
        setKeyState(getKey(bind), pressed);
    }

    public static boolean isPressed(KeyMapping bind) {
        return isKeyPressed(getKey(bind));
    }

    public static boolean isKeyPressed(int key) {
        if (!GuiKeyEvents.canUseKeys) return false;

        if (key == GLFW.GLFW_KEY_UNKNOWN) return false;
        return key < keys.length && keys[key];
    }

    public static boolean isButtonPressed(int button) {
        if (button == -1) return false;
        return button < buttons.length && buttons[button];
    }

    public static int getModifier(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> GLFW.GLFW_MOD_SHIFT;
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> GLFW.GLFW_MOD_CONTROL;
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> GLFW.GLFW_MOD_ALT;
            case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> GLFW.GLFW_MOD_SUPER;
            default -> 0;
        };
    }

    public static boolean isKeyDown(int keyCode) {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
    }
}