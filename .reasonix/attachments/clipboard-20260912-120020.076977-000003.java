package net.iqaddons.mod.features.kuudra.linus;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.spot.SupplyPosition;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Slf4j
public class AutoRodFeature extends Feature {
    private final SupplyStateManager supplyState = SupplyStateManager.get();

    private static final long CAST_SAFETY_TIMEOUT_MS = 1000L;

    private int handledHookId = -1;
    private boolean castPending = false;
    private long castSentMillis = 0L;

    private boolean armed = false;
    private boolean previousHookPresent = false;

    public AutoRodFeature() {
        super(
                "autoRod",
                "Auto Rod Supply Pull",
                () -> PhaseOneConfig.supplyCastHelper
        );
    }

    @Override
    protected void onActivate() {
        armed = false;
        previousHookPresent = false;
        handledHookId = -1;
        castPending = false;
        castSentMillis = 0L;
        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onDeactivate() {
        armed = false;
        previousHookPresent = false;
        handledHookId = -1;
        castPending = false;
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() != Items.FISHING_ROD) {
            armed = false;
            handledHookId = -1;
            return;
        }

        FishingHook hook = mc.player.fishing;
        boolean hookPresent = hook != null;

        if (hookPresent && castPending) {
            castPending = false; // server confirmed the cast, hook has synced back
        }

        if (!previousHookPresent && hookPresent && !armed) {
            armed = true;
            handledHookId = -1;
            log.info("Auto rod armed after manual cast detected");
        }

        previousHookPresent = hookPresent;

        if (!armed) return;

        Zombie closestSupplyHitbox = findClosestSupplyHitbox(mc.player);
        if (closestSupplyHitbox != null && closestSupplyHitbox.distanceTo(mc.player) < 6) {
            return;
        }

        SupplyPosition closestSupply = findClosestSupply(mc.player.position());
        if (closestSupply == null) return;

        double playerDistance = mc.player.position().distanceTo(getInterpolatedSupplyPosition(closestSupply).add(0.5, -0.5, 1.5));
        if (playerDistance <= 2) {
            armed = false;
            log.info("Auto rod disarmed, supply within {} blocks of player", playerDistance);
            return;
        }

        if (hookPresent) {
            if (hook.getId() == handledHookId) return;

            double hookDistance = hook.position().distanceTo(getInterpolatedSupplyPosition(closestSupply).add(0.5, -0.5, 1.5));
            if (hookDistance > 5.5) return;

            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            handledHookId = hook.getId();
            log.info("Auto-reeled fishing hook, distance to supply hitbox was {}", hookDistance);
        } else {
            tryRecast();
        }
    }

    private void tryRecast() {
        handledHookId = -1;

        if (castPending) {
            long now = System.currentTimeMillis();
            if (now - castSentMillis < CAST_SAFETY_TIMEOUT_MS) return; // still waiting for hook to sync back

            log.warn("Auto rod cast timed out waiting for hook to appear, retrying");
        }

        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        castPending = true;
        castSentMillis = System.currentTimeMillis();
    }



    private @Nullable Zombie findClosestSupplyHitbox(@NotNull LocalPlayer player) {
        List<Zombie> zombies = EntityDetectorUtil.getEntitiesOfType(Zombie.class);
        if (zombies.isEmpty()) return null;

        Zombie closest = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (Zombie Zombie : zombies) {
            if (Math.abs(Zombie.getBbHeight() - 0.975) < 0.1) continue;

            double distSq = Zombie.distanceToSqr(player.position());
            if (distSq < closestDistanceSq) {
                closestDistanceSq = distSq;
                closest = Zombie;
            }
        }

        return closest;
    }







    private @Nullable SupplyPosition findClosestSupply(@NotNull Vec3 fromPos) {
        List<SupplyPosition> supplies = supplyState.getActiveSupplies();
        if (supplies.isEmpty()) return null;

        SupplyPosition closest = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (SupplyPosition SupplyPosition : supplies) {
            Vec3 middlePos = getInterpolatedSupplyPosition(SupplyPosition).add(0.5, -0.5, 1.5);

            double distSq = middlePos.distanceToSqr(fromPos);
            if (distSq < closestDistanceSq) {
                closestDistanceSq = distSq;
                closest = SupplyPosition;
            }
        }

        return closest;
    }

    private @NotNull Vec3 getInterpolatedSupplyPosition(@NotNull SupplyPosition supply) {
        if (mc.level == null) return supply.position();

        Entity entity = mc.level.getEntity(supply.entityId());
        if (!(entity instanceof Giant giant)) return supply.position();

        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        double x = giant.xo + (giant.getX() - giant.xo) * tickDelta;
        double z = giant.zo + (giant.getZ() - giant.zo) * tickDelta;

        return SupplyPosition.fromGiant(x, z, giant.getYRot(), giant.getId()).position();
    }

}