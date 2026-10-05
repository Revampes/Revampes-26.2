package xyz.whatsyouss.frosty.utility.kuudra;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;

import java.util.Optional;

/**
 * Ported from IQAddons (net.iqaddons.mod.utils.KuudraLocationUtil), restricted to the
 * boss lookup the health display / hitbox use.
 */
public final class KuudraLocationUtil {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final int KUUDRA_SIZE = 30;
    private static final float MIN_KUUDRA_MAX_HEALTH = 10_000f;

    private static MagmaCube cachedKuudra = null;

    private KuudraLocationUtil() {
    }

    public static Optional<MagmaCube> findKuudra() {
        ClientLevel level = mc.level;
        if (level == null) {
            cachedKuudra = null;
            return Optional.empty();
        }

        if (cachedKuudra != null && cachedKuudra.isAlive() && isKuudra(cachedKuudra)) {
            return Optional.of(cachedKuudra);
        }

        cachedKuudra = findKuudraBoss(level);
        return Optional.ofNullable(cachedKuudra);
    }

    public static boolean isKuudra(MagmaCube entity) {
        if (entity == null) return false;
        if (entity.getMaxHealth() < MIN_KUUDRA_MAX_HEALTH) return false;

        return entity.getSize() == KUUDRA_SIZE && entity.getHealth() > 0;
    }

    public static void invalidateCache() {
        cachedKuudra = null;
    }

    private static MagmaCube findKuudraBoss(ClientLevel level) {
        MagmaCube kuudra = null;
        double maxY = 0;
        int cubesFound = 0;

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof MagmaCube cube)) continue;
            if (cube.getSize() != KUUDRA_SIZE) continue;

            double y = cube.getY();
            cubesFound++;
            if (y > maxY) {
                kuudra = cube;
                maxY = y;
            }
        }

        if (kuudra == null || cubesFound == 0) return null;
        if (kuudra.getHealth() <= 0) return null;

        return kuudra;
    }
}
