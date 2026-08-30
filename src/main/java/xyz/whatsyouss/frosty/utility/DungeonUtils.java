package xyz.whatsyouss.frosty.utility;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;

public class DungeonUtils {
    public static boolean isInDungeon() {
        return LocationUtils.isInDungeon();
    }

    public static boolean isStarMob(ArmorStand armorStand) {
        Component text = armorStand.getCustomName();
        if (text == null) return false;

        String name = text.getString();
        return name.contains("\u272A") || name.contains("\u2728") || name.contains("✯") || name.contains("✪");
    }
}
