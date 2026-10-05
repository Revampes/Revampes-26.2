package xyz.whatsyouss.frosty.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Ported from IQAddons (net.iqaddons.mod.utils.EntityDetectorUtil), trimmed to the
 * lookups the Kuudra modules use. Uses {@code ClientLevel#entitiesForRendering()}
 * exactly like the original.
 */
public final class EntityDetectorUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    private EntityDetectorUtil() {
    }

    public static <T extends Entity> List<T> getEntitiesOfType(Class<T> entityClass) {
        ClientLevel level = mc.level;
        if (level == null) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entityClass.isInstance(entity)) {
                result.add(entityClass.cast(entity));
            }
        }
        return result;
    }

    public static <T extends Entity> List<T> getEntitiesOfType(Class<T> entityClass, Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T entity : getEntitiesOfType(entityClass)) {
            if (predicate.test(entity)) {
                result.add(entity);
            }
        }
        return result;
    }

    public static List<Giant> getAllGiants() {
        return getEntitiesOfType(Giant.class);
    }

    public static List<Giant> getSupplyCarriers() {
        return getEntitiesOfType(Giant.class, giant ->
                giant.getY() < 67.0 && isHoldingSkull(giant)
        );
    }

    public static List<ArmorStand> getAllArmorStands() {
        return getEntitiesOfType(ArmorStand.class);
    }

    public static List<ArmorStand> getCompletedPileStands() {
        return getEntitiesOfType(ArmorStand.class, stand ->
                stand.hasCustomName() &&
                        stand.getCustomName() != null &&
                        stand.getCustomName().getString().contains("SUPPLIES RECEIVED")
        );
    }

    public static boolean isHoldingSkull(Giant giant) {
        ItemStack heldItem = giant.getMainHandItem();
        if (heldItem.isEmpty()) return false;

        return heldItem.is(Items.PLAYER_HEAD) || heldItem.is(Items.SKELETON_SKULL) ||
                heldItem.is(Items.WITHER_SKELETON_SKULL) || heldItem.is(Items.ZOMBIE_HEAD) ||
                heldItem.is(Items.CREEPER_HEAD) || heldItem.is(Items.PIGLIN_HEAD) ||
                heldItem.is(Items.DRAGON_HEAD);
    }
}
