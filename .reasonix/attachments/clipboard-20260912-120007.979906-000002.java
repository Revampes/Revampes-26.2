package net.iqaddons.mod.features.kuudra.linus;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPickupEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.spot.SupplyPosition;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.StringUtils;
import net.iqaddons.mod.utils.render.RenderColor;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

@Slf4j
public class SupplyPickupAuraFeature extends KuudraFeature {
    private String username = "";
    private  boolean isPickingUpsupply = false;
    private final SupplyStateManager supplyState = SupplyStateManager.get();
    public SupplyPickupAuraFeature() {
        super(
                "SupplyPickupAura",
                "Supply Pickup Aura",
                () -> PhaseOneConfig.supplyPickupAura,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        username = mc.player.getName().getString();
        isPickingUpsupply = false;
        subscribe(WorldRenderEvent.class, this::onRender);
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(SupplyDropEvent.class, this::onDrop);
        subscribe(SupplyPlaceEvent.class, this::onPlace);
        subscribe(SupplyPickupEvent.class, event -> isPickingUpsupply = false);
        subscribe(ChatReceivedEvent.class, this::onChat);
    }

    private void onChat (@NotNull ChatReceivedEvent event) {
        if (event.getStrippedMessage().equals("Someone else is currently trying to pick up these supplies!")) {
            isPickingUpsupply = false;
        }
        if (event.getStrippedMessage().equals("You are already currently picking up some supplies!")) {
            isPickingUpsupply = true;
        }
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        if (mc.player == null) return;
        Zombie closestHitbox = findClosestSupplyHitbox(mc.player);
        if (closestHitbox == null || closestHitbox.distanceToSqr(mc.player) > 25) return;
        RenderColor color = RenderColor.fromArgb(new Color(0, 160, 255, 255).getRGB());
        event.drawStyledHitbox(closestHitbox, true, color, WorldRenderUtils.RenderStyle.BOTH);
    }

    private void onDrop(@NotNull SupplyDropEvent event) {
        log.info("onDrop with data: eventName: {}, username: {}", StringUtils.stripFormatting(event.playerName()), username);
        if (!StringUtils.stripFormatting(event.playerName()).equals(username)) return;
        isPickingUpsupply = false;
    }

    private void onPlace(@NotNull SupplyPlaceEvent event) {
        log.info("onPlace with data: eventName: {}, username: {}", StringUtils.stripFormatting(event.playerName()), username);
        if (!StringUtils.stripFormatting(event.playerName()).equals(username)) return;
        isPickingUpsupply = false;
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (mc.player == null || mc.gameMode == null) return;
        if (isPickingUpsupply) return;
        Zombie closestSupplyHitbox = findClosestSupplyHitbox(mc.player);
        if (closestSupplyHitbox != null && closestSupplyHitbox.distanceToSqr(mc.player) < 25) {

            AABB box = closestSupplyHitbox.getBoundingBox();
            Vec3 eyePos = mc.player.getEyePosition();

            double x = Mth.clamp(eyePos.x, box.minX, box.maxX);
            double y = Mth.clamp(eyePos.y, box.minY, box.maxY);
            double z = Mth.clamp(eyePos.z, box.minZ, box.maxZ);

            Vec3 hitPosition = new Vec3(x, y, z);
            EntityHitResult supplyHitResult = new EntityHitResult(closestSupplyHitbox, hitPosition);
            log.info("interacting with data: LocalPlayer: {}, entity: {}, hitResult: {}, hand: {}", mc.player, closestSupplyHitbox, supplyHitResult, InteractionHand.MAIN_HAND);
            mc.gameMode.interact(mc.player, closestSupplyHitbox, supplyHitResult, InteractionHand.MAIN_HAND);
        }
    }

    private @Nullable Zombie findClosestSupplyHitbox(@NotNull LocalPlayer player) {
        List<SupplyPosition> supplies = supplyState.getActiveSupplies();
        if (supplies.isEmpty()) return null;
        List<Zombie> zombies = EntityDetectorUtil.getEntitiesOfType(Zombie.class);
        if (zombies.isEmpty()) return null;

        Zombie overallClosest = null;
        double overallClosestDistanceSq = Double.MAX_VALUE;

        for (SupplyPosition supply : supplies) {
            Vec3 realPos = getInterpolatedSupplyPosition(supply);
            for (Zombie zombie : zombies) {
                if (zombie.distanceToSqr(realPos) > 9) continue;
                if (Math.abs(zombie.getBbHeight() - 0.975) < 0.1) continue;

                double distSq = zombie.distanceToSqr(player.position());
                if (distSq < overallClosestDistanceSq) {
                    overallClosestDistanceSq = distSq;
                    overallClosest = zombie;
                }
            }
        }
        return overallClosest;
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