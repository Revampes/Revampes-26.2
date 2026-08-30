package xyz.whatsyouss.frosty.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;

public class HotbarSwapUtils {
    public static final int NOT_FOUND = -1;

    private static final Minecraft mc = Minecraft.getInstance();

    // Movement key bindings (26.2: keyUp = forward, keyDown = back, keyLeft, keyRight)
    private static final KeyMapping[] MOVEMENT_KEYS = {
        mc.options.keyUp,
        mc.options.keyDown,
        mc.options.keyLeft,
        mc.options.keyRight
    };

    // Store original key states
    private static boolean[] originalKeyStates = new boolean[4];
    private static boolean movementLocked = false;

    public static void stopInputs() {
        if (!movementLocked) {
            // Save current key states
            for (int i = 0; i < MOVEMENT_KEYS.length; i++) {
                originalKeyStates[i] = MOVEMENT_KEYS[i].isDown();
            }
            movementLocked = true;
        }

        // Set all movement keys to not pressed
        for (KeyMapping key : MOVEMENT_KEYS) {
            key.setDown(false);
        }
    }

    public static void restartMovement() {
        if (movementLocked) {
            // Restore original key states
            for (int i = 0; i < MOVEMENT_KEYS.length; i++) {
                MOVEMENT_KEYS[i].setDown(originalKeyStates[i]);
            }
            movementLocked = false;
        }
    }

    /**
     * Returns the UUID of a SkyBlock item from its NBT.
     */
    public static String getUUID(ItemStack stack) {
        if (stack == null) return null;
        try {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) return null;
            CompoundTag tag = customData.copyTag();
            if (tag == null) return null;
            if (!tag.contains("ExtraAttributes")) return null;

            CompoundTag extra = tag.getCompound("ExtraAttributes").orElse(null);
            if (extra == null) return null;

            return extra.getString("uuid").orElse(null);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Returns the SkyBlock ID from the item's NBT.
     */
    public static String getSkyblockID(ItemStack stack) {
        if (stack == null) return null;
        try {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) return null;
            CompoundTag tag = customData.copyTag();
            if (tag == null) return null;
            if (!tag.contains("ExtraAttributes")) return null;

            CompoundTag extra = tag.getCompound("ExtraAttributes").orElse(null);
            if (extra == null) return null;

            return extra.getString("id").orElse(null);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Returns the display name of the item as a plain string.
     */
    public static String getDisplayName(ItemStack stack) {
        if (stack == null) return "None";
        Component name = stack.getHoverName();
        return name.getString();
    }
}
