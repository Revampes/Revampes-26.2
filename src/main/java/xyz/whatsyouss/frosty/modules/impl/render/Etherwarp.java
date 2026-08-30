package xyz.whatsyouss.frosty.modules.impl.render;

import xyz.whatsyouss.frosty.events.impl.ReceivePacketEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.events.impl.SendPacketEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.LocationUtils;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.ClipContext;

import java.awt.Color;

public class Etherwarp extends Module {

    private final ButtonSetting render = new ButtonSetting("Show Guess", true);
    private final ColorSetting color = new ColorSetting("Color", new Color(255, 170, 0, 128));
    private final ButtonSetting renderFail = new ButtonSetting("Show when failed", true);
    private final ColorSetting failColor = new ColorSetting("Fail Color", new Color(255, 85, 85, 128));
    private final SelectSetting renderStyle = new SelectSetting("Render Style", 1, new String[]{"Filled", "Outline", "Filled Outline"});
    private final ButtonSetting useServerPosition = new ButtonSetting("Use Server Position", false);
    private final ButtonSetting fullBlock = new ButtonSetting("Full Block", false);
    private final ButtonSetting customSounds = new ButtonSetting("Custom Sounds", false);

    private EtherPos etherPos = null;

    public Etherwarp() {
        super("Etherwarp", category.Render);
        this.registerSetting(render);
        this.registerSetting(color);
        this.registerSetting(renderFail);
        this.registerSetting(failColor);
        this.registerSetting(renderStyle);
        this.registerSetting(useServerPosition);
        this.registerSetting(fullBlock);
        this.registerSetting(customSounds);
    }

    @Override
    public void guiUpdate() {
        this.color.setVisibilityCondition(() -> render.isToggled());
        this.renderFail.setVisibilityCondition(() -> render.isToggled());
        this.failColor.setVisibilityCondition(() -> render.isToggled() && renderFail.isToggled());
        this.renderStyle.setVisibilityCondition(() -> render.isToggled());
        this.useServerPosition.setVisibilityCondition(() -> render.isToggled());
        this.fullBlock.setVisibilityCondition(() -> render.isToggled());
    }

    @EventHandler
    public void onReceive(ReceivePacketEvent event) {
        if (!customSounds.isToggled()) return;
        if (event.getPacket() instanceof ClientboundSoundPacket packet) {
            if (packet.getSound().value() == SoundEvents.ENDER_DRAGON_HURT && packet.getPitch() >= 0.53f && packet.getPitch() <= 0.54f) {
                mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, packet.getVolume(), 1.0f);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.gui.screen() != null || !render.isToggled() || mc.player == null || mc.level == null) return;

        ItemStack mainHandItem = mc.player.getMainHandItem();
        if (mainHandItem.isEmpty()) return;

        String id = Utils.getCustomDataIId(mainHandItem.getComponents().toString());
        if (id == null || id.isEmpty()) return;

        boolean isEtherwarp = id.equals("ETHERWARP_CONDUIT") || id.equals("ASPECT_OF_THE_VOID");
        boolean isAote = id.equals("ASPECT_OF_THE_END");

        if (!isEtherwarp && !(isAote && mc.player.isShiftKeyDown())) {
            return;
        }

        if (!mc.player.isShiftKeyDown() && !id.equals("ETHERWARP_CONDUIT")) return;

        double distance = 57.0;

        Vec3 position = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        etherPos = getEtherPos(position, distance, true);

        if (etherPos.succeeded == false && !renderFail.isToggled()) return;

        Color renderColor = etherPos.succeeded ? color.getColor() : failColor.getColor();

        if (etherPos.pos != null) {
            AABB box;
            if (fullBlock.isToggled()) {
                box = new AABB(etherPos.pos);
            } else {
                BlockState state = mc.level.getBlockState(etherPos.pos);
                if (!state.isAir()) {
                    VoxelShape outlineShape = state.getShape(mc.level, etherPos.pos);
                    if (outlineShape.isEmpty()) {
                        box = new AABB(etherPos.pos);
                    } else {
                        box = outlineShape.bounds().move(etherPos.pos.getX(), etherPos.pos.getY(), etherPos.pos.getZ());
                    }
                } else {
                    box = new AABB(etherPos.pos);
                }
            }

            PoseStack stack = event.getMatrix();
            String style = renderStyle.getOption();

            if (style.equals("Filled") || style.equals("Filled Outline")) {
                RenderUtils.drawBoxFilled(stack, box, renderColor, true);
            }
            if (style.equals("Outline") || style.equals("Filled Outline")) {
                RenderUtils.drawBox(stack, box, renderColor, 2.0f, true);
            }
        }
    }

    @EventHandler
    public void onSend(SendPacketEvent event) {
        if (!LocationUtils.getCurrentArea().equals("SinglePlayer")) return;

        if (event.getPacket() instanceof ServerboundUseItemPacket packet) {
            ItemStack mainHandItem = mc.player.getMainHandItem();
            if (mainHandItem.isEmpty()) return;

            String id = Utils.getCustomDataIId(mainHandItem.getComponents().toString());
            boolean isEtherwarp = id.equals("ETHERWARP_CONDUIT") || id.equals("ASPECT_OF_THE_VOID");

            if (isEtherwarp || (id.equals("ASPECT_OF_THE_END") && mc.player.isShiftKeyDown())) {
                if (!mc.player.isShiftKeyDown() && !id.equals("ETHERWARP_CONDUIT")) return;

                if (etherPos != null && etherPos.pos != null && etherPos.succeeded) {
                    BlockPos p = etherPos.pos;
                    mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                        new Vec3(p.getX() + 0.5, p.getY() + 1.05, p.getZ() + 0.5),
                        mc.player.getYRot(), mc.player.getXRot(),
                        false, mc.player.horizontalCollision
                    ));
                    mc.player.setPos(p.getX() + 0.5, p.getY() + 1.05, p.getZ() + 0.5);
                    mc.player.setDeltaMovement(0, 0, 0);

                    if (customSounds.isToggled()) {
                        mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    } else {
                        mc.player.playSound(SoundEvents.ENDER_DRAGON_HURT, 1.0f, 0.53968257f);
                    }
                }
            }
        }
    }

    public static class EtherPos {
        public boolean succeeded;
        public BlockPos pos;

        public EtherPos(boolean succeeded, BlockPos pos) {
            this.succeeded = succeeded;
            this.pos = pos;
        }

        public static final EtherPos NONE = new EtherPos(false, null);
    }

    private EtherPos getEtherPos(Vec3 position, Double distance, boolean etherWarp) {
        if (mc.player == null || mc.level == null) return EtherPos.NONE;

        double eyeHeight = mc.player.isShiftKeyDown() ? 1.54 : 1.62;
        Vec3 startPos = position.add(0, eyeHeight, 0);
        Vec3 endPos = startPos.add(mc.player.getViewVector(1.0f).scale(distance));

        BlockHitResult result = mc.level.clip(new ClipContext(
                startPos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player
        ));

        if (result.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = result;
            BlockPos hitPos = blockHit.getBlockPos();

            // basic check to see if we can stand on it
            BlockPos head = hitPos.above(2);
            BlockPos feet = hitPos.above(1);

            if (mc.level.getBlockState(head).getCollisionShape(mc.level, head).isEmpty()
                && mc.level.getBlockState(feet).getCollisionShape(mc.level, head).isEmpty()) {
                return new EtherPos(true, hitPos);
            } else {
                return new EtherPos(false, hitPos);
            }
        }

        return new EtherPos(false, BlockPos.containing(endPos));
    }
}
