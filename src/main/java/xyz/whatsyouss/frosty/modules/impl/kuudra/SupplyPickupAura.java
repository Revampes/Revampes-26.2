package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.SupplyDropEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.SupplyPickupEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.SupplyPlaceEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.EntityDetectorUtil;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.StringUtils;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;
import xyz.whatsyouss.frosty.utility.kuudra.SupplyPosition;
import xyz.whatsyouss.frosty.utility.kuudra.SupplyState;

import java.awt.Color;
import java.util.List;

public class SupplyPickupAura extends Module {

//    private final SliderSetting maxInteract = new SliderSetting("Max Interact /s", 1, 1, 5, 1);
    private final SliderSetting maxInteractDistance = new SliderSetting("Max Interact Distance (block)", 5, 1, 5, 1);

    private static final Color HITBOX_COLOR = new Color(0, 160, 255, 255);
//    private static final double REACH_SQ = 25.0;
    private int interactCooldown = 0;

    private final double finalReachSQ = (double) maxInteractDistance.getInput() * (double) maxInteractDistance.getInput();

    private final SupplyState supplyState = SupplyState.get();

    private boolean isPickingUpSupply = false;;

    public SupplyPickupAura() {
        super("Supply Pickup Aura", category.Kuudra);
//        this.registerSetting(maxInteract);
        this.registerSetting(maxInteractDistance);
    }

    @Override
    public void onDisable() {
        this.isPickingUpSupply = false;
        this.interactCooldown = 0;
    }

    private boolean inSuppliesPhase() {
        return KuudraState.get().phase() == KuudraPhase.SUPPLIES;
    }

    @EventHandler
    public void onChat(ReceiveMessageEvent event) {
        if (!inSuppliesPhase()) return;

        String stripped = StringUtils.stripFormatting(event.getMessage().getString());
        if (stripped.equals("Someone else is currently trying to pick up these supplies!")) {
            this.isPickingUpSupply = false;
        }
        if (stripped.equals("You are already currently picking up some supplies!")) {
            this.isPickingUpSupply = true;
        }
    }

    @EventHandler
    public void onDrop(SupplyDropEvent event) {
        if (!inSuppliesPhase()) return;
        if (mc.player == null) return;
        if (!StringUtils.stripFormatting(event.playerName()).equals(mc.player.getName().getString())) return;
        this.isPickingUpSupply = false;
    }

    @EventHandler
    public void onPlace(SupplyPlaceEvent event) {
        if (!inSuppliesPhase()) return;
        if (mc.player == null) return;
        if (!StringUtils.stripFormatting(event.playerName()).equals(mc.player.getName().getString())) return;
        this.isPickingUpSupply = false;
    }

    @EventHandler
    public void onPickup(SupplyPickupEvent event) {
        if (!inSuppliesPhase()) return;
        this.isPickingUpSupply = false;
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (!inSuppliesPhase()) return;
        if (mc.player == null) return;

        Zombie closestHitbox = findClosestSupplyHitbox(mc.player);
        if (closestHitbox == null || closestHitbox.distanceToSqr(mc.player) > finalReachSQ) return;

        AABB box = closestHitbox.getBoundingBox();
        RenderUtils.drawBoxFilled(event.getMatrix(), box, HITBOX_COLOR, false);
        RenderUtils.drawBox(event.getMatrix(), box, HITBOX_COLOR, 2f, false);
    }

    @EventHandler
    public void onTick(PreUpdateEvent event) {
        if (!inSuppliesPhase()) return;
        if (mc.player == null || mc.gameMode == null || mc.level == null) return;
        if (this.isPickingUpSupply) return;

        if (this.interactCooldown > 0) {
            this.interactCooldown--;
            return;
        }
        Zombie closestSupplyHitbox = findClosestSupplyHitbox(mc.player);
        if (closestSupplyHitbox != null && closestSupplyHitbox.distanceToSqr(mc.player) < finalReachSQ) {
            AABB box = closestSupplyHitbox.getBoundingBox();
            Vec3 eyePos = mc.player.getEyePosition();

            double x = Mth.clamp(eyePos.x, box.minX, box.maxX);
            double y = Mth.clamp(eyePos.y, box.minY, box.maxY);
            double z = Mth.clamp(eyePos.z, box.minZ, box.maxZ);

            Vec3 hitPosition = new Vec3(x, y, z);
            EntityHitResult supplyHitResult = new EntityHitResult(closestSupplyHitbox, hitPosition);
            mc.gameMode.interact(mc.player, closestSupplyHitbox, supplyHitResult, InteractionHand.MAIN_HAND);

            this.interactCooldown = 20;
        }
    }

    private Zombie findClosestSupplyHitbox(LocalPlayer player) {
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
