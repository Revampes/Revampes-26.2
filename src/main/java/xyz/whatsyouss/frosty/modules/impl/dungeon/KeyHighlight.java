package xyz.whatsyouss.frosty.modules.impl.dungeon;

import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.DungeonUtils;
import xyz.whatsyouss.frosty.utility.ItemUtils;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;
import xyz.whatsyouss.frosty.utility.skyblock.HeadTextures;
import meteordevelopment.orbit.EventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.List;

public class KeyHighlight extends Module {

    private final ButtonSetting announceKeySpawn = new ButtonSetting("Announce Key Spawn", true);
    private final ButtonSetting drawLine = new ButtonSetting("Draw Line", true);
    private final SliderSetting lineWidth = new SliderSetting("Line Width", 2, 1, 10, 0.5);
    private final SelectSetting currentHighlight = new SelectSetting("Highlight Type", 0, new String[]{"Both", "Filled", "Outline"});
    private final SliderSetting outlineWidth = new SliderSetting("Outline Width", 2, 1, 10, 0.5);
    private final ColorSetting witherColor = new ColorSetting("Wither Color", new Color(0, 0, 0, 204));
    private final ColorSetting witherLineColor = new ColorSetting("Wither Line Color", new Color(0, 0, 0, 204));
    private final ColorSetting bloodColor = new ColorSetting("Blood Color", new Color(255, 85, 85, 204));
    private final ColorSetting bloodLineColor = new ColorSetting("Blood Line Color", new Color(255, 85, 85, 204));
    private final ButtonSetting enableDepthCheck = new ButtonSetting("Depth Check (See through wall)", false);

    private ArmorStand currentWitherKey = null;
    private ArmorStand currentBloodKey = null;

    public KeyHighlight() {
        super("KeyHighlight", category.Dungeon);
        this.registerSetting(announceKeySpawn);
        this.registerSetting(drawLine);
        this.registerSetting(lineWidth);
        this.registerSetting(currentHighlight);
        this.registerSetting(outlineWidth);
        this.registerSetting(witherColor);
        this.registerSetting(witherLineColor);
        this.registerSetting(bloodColor);
        this.registerSetting(bloodLineColor);
        this.registerSetting(enableDepthCheck);
    }

    @Override
    public void guiUpdate() {
        this.lineWidth.setVisibilityCondition(() -> drawLine.isToggled());
        this.witherLineColor.setVisibilityCondition(() -> drawLine.isToggled());
        this.bloodLineColor.setVisibilityCondition(() -> drawLine.isToggled());
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!DungeonUtils.isInDungeon()) {
            currentWitherKey = null;
            currentBloodKey = null;
            return;
        }

        PoseStack stack = event.getMatrix();

        if (currentWitherKey != null && (!currentWitherKey.isAlive() || mc.level.getEntity(currentWitherKey.getId()) == null)) {
            currentWitherKey = null;
        }
        if (currentBloodKey != null && (!currentBloodKey.isAlive() || mc.level.getEntity(currentBloodKey.getId()) == null)) {
            currentBloodKey = null;
        }

        // Find key-carrying armor stands in the level
        AABB searchBox = new AABB(mc.player.getX() - 128, mc.player.getY() - 128, mc.player.getZ() - 128,
                                   mc.player.getX() + 128, mc.player.getY() + 128, mc.player.getZ() + 128);
        List<Entity> stands = mc.level.getEntities(mc.player, searchBox, e -> e instanceof ArmorStand);
        for (Entity entity : stands) {
            if (entity instanceof ArmorStand stand) {
                ItemStack headStack = stand.getItemBySlot(EquipmentSlot.HEAD);
                if (headStack != null && !headStack.isEmpty() && headStack.is(Items.PLAYER_HEAD)) {
                    String texture = ItemUtils.getHeadTexture(headStack);
                    if (texture != null) {
                        if (texture.equals(HeadTextures.WITHER_KEY)) {
                            if (currentWitherKey != stand) {
                                currentWitherKey = stand;
                                if (announceKeySpawn.isToggled()) {
                                    Utils.addChatMessage("§8Wither Key§7 spawned!");
                                }
                            }
                        } else if (texture.equals(HeadTextures.BLOOD_KEY)) {
                            if (currentBloodKey != stand) {
                                currentBloodKey = stand;
                                if (announceKeySpawn.isToggled()) {
                                    Utils.addChatMessage("§cBlood Key§7 spawned!");
                                }
                            }
                        }
                    }
                }
            }
        }

        String mode = currentHighlight.getOption();
        boolean doFill = mode == null || mode.equals("Filled") || mode.equals("Both");
        boolean doOutline = mode == null || mode.equals("Outline") || mode.equals("Both");
        boolean depthTest = enableDepthCheck.isToggled();

        if (currentWitherKey != null) {
            double x = currentWitherKey.getX();
            double y = currentWitherKey.getY();
            double z = currentWitherKey.getZ();
            AABB box = new AABB(x - 0.5, y + 1.0, z - 0.5, x + 0.5, y + 2.0, z + 0.5);
            Color colored = witherColor.getColor();

            if (doFill) {
                RenderUtils.drawBoxFilled(stack, box, colored, depthTest);
            }
            if (doOutline) {
                RenderUtils.drawBox(stack, box, colored, (float) outlineWidth.getInput(), depthTest);
            }
            if (drawLine.isToggled()) {
                Vec3 crosshair = mc.player.getEyePosition(event.getDelta()).add(mc.player.getViewVector(event.getDelta()).scale(2.0));
                Vec3 center = new Vec3(x, y + 1.5, z);
                RenderUtils.drawLine3D(stack, crosshair, center, witherLineColor.getColor(), (float) lineWidth.getInput(), depthTest);
            }
        }

        if (currentBloodKey != null) {
            double x = currentBloodKey.getX();
            double y = currentBloodKey.getY();
            double z = currentBloodKey.getZ();
            AABB box = new AABB(x - 0.5, y + 1.0, z - 0.5, x + 0.5, y + 2.0, z + 0.5);
            Color colored = bloodColor.getColor();
            if (doFill) {
                RenderUtils.drawBoxFilled(stack, box, colored, depthTest);
            }
            if (doOutline) {
                RenderUtils.drawBox(stack, box, colored, (float) outlineWidth.getInput(), depthTest);
            }
            if (drawLine.isToggled()) {
                Vec3 crosshair = mc.player.getEyePosition(event.getDelta()).add(mc.player.getViewVector(event.getDelta()).scale(2.0));
                Vec3 center = new Vec3(x, y + 1.5, z);
                RenderUtils.drawLine3D(stack, crosshair, center, bloodLineColor.getColor(), (float) lineWidth.getInput(), depthTest);
            }
        }
    }
}
