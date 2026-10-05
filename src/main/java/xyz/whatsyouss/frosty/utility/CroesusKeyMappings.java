package xyz.whatsyouss.frosty.utility;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Ported from IQAddons (net.iqaddons.mod.IQKeyBindings) — only the Croesus page
 * navigation keys used by the Croesus helper. Rebindable in Options -> Controls.
 */
public final class CroesusKeyMappings {

    private static final KeyMapping.Category FROSTY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("frosty", "croesus"));

    private static KeyMapping advancePageKey;
    private static KeyMapping goBackPageKey;
    private static boolean registered = false;

    private CroesusKeyMappings() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        advancePageKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.frosty.advance-croesus-page",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT,
                FROSTY_CATEGORY
        ));

        goBackPageKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.frosty.go-back-croesus-page",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT,
                FROSTY_CATEGORY
        ));
    }

    public static KeyMapping getAdvancePageKey() {
        return advancePageKey;
    }

    public static KeyMapping getGoBackPageKey() {
        return goBackPageKey;
    }
}
