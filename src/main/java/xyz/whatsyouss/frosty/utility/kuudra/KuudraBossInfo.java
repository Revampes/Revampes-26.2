package xyz.whatsyouss.frosty.utility.kuudra;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from IQAddons (net.iqaddons.mod.model.kuudra.KuudraBossInfo).
 */
public record KuudraBossInfo(
        Entity bossEntity,
        float currentHealth,
        float maxHealth,
        double damageReceived,
        Vec3 position
) {

    public static KuudraBossInfo empty() {
        return new KuudraBossInfo(null, 0f, 0f, 0d, Vec3.ZERO);
    }

    public static KuudraBossInfo tracked(MagmaCube bossEntity) {
        float clamped = Math.max(0f, bossEntity.getHealth());
        float damageReceived = Math.max(0f, bossEntity.getMaxHealth() - clamped);

        return new KuudraBossInfo(bossEntity, clamped, bossEntity.getMaxHealth(), damageReceived, bossEntity.position());
    }

    public boolean isAlive() {
        return bossEntity != null && bossEntity.isAlive();
    }
}
