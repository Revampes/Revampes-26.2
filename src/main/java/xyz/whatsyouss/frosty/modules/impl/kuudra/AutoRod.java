package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.utility.EntityDetectorUtil;
import xyz.whatsyouss.frosty.utility.kuudra.SupplyPosition;
import xyz.whatsyouss.frosty.utility.kuudra.SupplyState;

import java.util.List;

/**
 * Ported from IQAddons (features/kuudra/linus/AutoRodFeature).
 *
 * While holding a fishing rod, casts / reels it toward the closest active supply.
 * Armed state only after a manual cast is detected (hook appears), then it
 * auto-reels when the hook reaches a supply and re-casts when the hook is gone.
 * The safety timeout avoids spamming casts while waiting for the hook to sync.
 */
public class AutoRod extends Module {

    private final SupplyState supplyState = SupplyState.get();

    private static final long CAST_SAFETY_TIMEOUT_MS = 1000L;

    private int handledHookId = -1;
    private boolean castPending = false;
    private long castSentMillis = 0L;

    private boolean armed = false;
    private boolean previousHookPresent = false;

    public AutoRod() {
        super("Auto Rod", category.Kuudra);
    }

    @Override
    public void onEnable() {
        this.armed = false;
        this.previousHookPresent = false;
        this.handledHookId = -1;
        this.castPending = false;
        this.castSentMillis = 0L;
    }

    @Override
    public void onDisable() {
        this.armed = false;
        this.previousHookPresent = false;
        this.handledHookId = -1;
        this.castPending = false;
    }

    @EventHandler
    public void onTick(PreUpdateEvent event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() != Items.FISHING_ROD) {
            this.armed = false;
            this.handledHookId = -1;
            return;
        }

        FishingHook hook = mc.player.fishing;
        boolean hookPresent = hook != null;

        if (hookPresent && this.castPending) {
            this.castPending = false; // server confirmed the cast, hook has synced back
        }

        if (!this.previousHookPresent && hookPresent && !this.armed) {
            this.armed = true;
            this.handledHookId = -1;
        }

        this.previousHookPresent = hookPresent;

        if (!this.armed) return;

        Zombie closestSupplyHitbox = findClosestSupplyHitbox(mc.player);
        if (closestSupplyHitbox != null && closestSupplyHitbox.distanceTo(mc.player) < 6) {
            return;
        }

        SupplyPosition closestSupply = findClosestSupply(mc.player.position());
        if (closestSupply == null) return;

        double playerDistance = mc.player.position().distanceTo(getInterpolatedSupplyPosition(closestSupply).add(0.5, -0.5, 1.5));
        if (playerDistance <= 2) {
            this.armed = false;
            return;
        }

        if (hookPresent) {
            if (hook.getId() == this.handledHookId) return;

            double hookDistance = hook.position().distanceTo(getInterpolatedSupplyPosition(closestSupply).add(0.5, -0.5, 1.5));
            if (hookDistance > 5.5) return;

            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            this.handledHookId = hook.getId();
        } else {
            tryRecast();
        }
    }

    private void tryRecast() {
        this.handledHookId = -1;

        if (this.castPending) {
            long now = System.currentTimeMillis();
            if (now - this.castSentMillis < CAST_SAFETY_TIMEOUT_MS) return; // still waiting for hook to sync back
        }

        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        this.castPending = true;
        this.castSentMillis = System.currentTimeMillis();
    }

    private Zombie findClosestSupplyHitbox(LocalPlayer player) {
        List<Zombie> zombies = EntityDetectorUtil.getEntitiesOfType(Zombie.class);
        if (zombies.isEmpty()) return null;

        Zombie closest = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (Zombie zombie : zombies) {
            if (Math.abs(zombie.getBbHeight() - 0.975) < 0.1) continue;

            double distSq = zombie.distanceToSqr(player.position());
            if (distSq < closestDistanceSq) {
                closestDistanceSq = distSq;
                closest = zombie;
            }
        }

        return closest;
    }

    private SupplyPosition findClosestSupply(Vec3 fromPos) {
        List<SupplyPosition> supplies = supplyState.getActiveSupplies();
        if (supplies.isEmpty()) return null;

        SupplyPosition closest = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (SupplyPosition supply : supplies) {
            Vec3 middlePos = getInterpolatedSupplyPosition(supply).add(0.5, -0.5, 1.5);

            double distSq = middlePos.distanceToSqr(fromPos);
            if (distSq < closestDistanceSq) {
                closestDistanceSq = distSq;
                closest = supply;
            }
        }

        return closest;
    }

    private Vec3 getInterpolatedSupplyPosition(SupplyPosition supply) {
        if (mc.level == null) return supply.position();

        Entity entity = mc.level.getEntity(supply.entityId());
        if (!(entity instanceof Giant giant)) return supply.position();

        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        double x = giant.xo + (giant.getX() - giant.xo) * tickDelta;
        double z = giant.zo + (giant.getZ() - giant.zo) * tickDelta;

        return SupplyPosition.fromGiant(x, z, giant.getYRot(), giant.getId()).position();
    }
}
