package xyz.whatsyouss.frosty.utility;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Environment(EnvType.CLIENT)
public final class BlockAnimationUtils {
    private BlockAnimationUtils() {}

    public static final int DEFAULT_ITEM_USE_DURATION = 72_000;

    public static boolean isPlayerBlockingWithSword(Player player) {
        if (player == null) return false;
        return isPlayerRightClicking() && canSwordBlock(player);
    }

    public static boolean isPlayerRightClicking() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) return false;
        return client.options.keyUse.isDown();
    }

    public static boolean canSwordBlock(Player player) {
        if (!ModuleManager.blockAnimation.isEnabled()) return false;
        if (player == null) return false;
        Item mainHandItem = player.getMainHandItem().getItem();
        Item offHandItem = player.getOffhandItem().getItem();
        return isSword(mainHandItem) || isSword(offHandItem);
    }

    public static boolean isSword(Item item) {
        return item == Items.WOODEN_SWORD ||
               item == Items.STONE_SWORD ||
               item == Items.IRON_SWORD ||
               item == Items.GOLDEN_SWORD ||
               item == Items.DIAMOND_SWORD ||
               item == Items.NETHERITE_SWORD;
    }
}
