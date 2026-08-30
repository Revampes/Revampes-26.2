package xyz.whatsyouss.frosty.modules.impl.dungeon;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.DungeonUtils;
import xyz.whatsyouss.frosty.utility.RenderUtils;

import meteordevelopment.orbit.EventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

public class MobHighlight extends Module {

    public enum MobType {
        STAR, TANK, MINI, FEL, ASSASSIN, MIMIC
    }

    private final ButtonSetting dontShowInvisibleMobs = new ButtonSetting("Hide Invisible", true);
    private final ButtonSetting highlightFel = new ButtonSetting("Highlight Fel", true);
    private final ButtonSetting highlightMimic = new ButtonSetting("Highlight Mimic", true);
    private final ButtonSetting highlightMidas = new ButtonSetting("Highlight King Midas", true);
    private final ButtonSetting highlightMiniBoss = new ButtonSetting("Highlight Miniboss", true);
    private final ButtonSetting highlightTankMob = new ButtonSetting("Highlight TankMob", true);
    private final ButtonSetting highlightStarMob = new ButtonSetting("Highlight StarMob", true);
    private final SelectSetting currentHighlight = new SelectSetting("Highlight Type", 0, new String[]{"Both", "Filled", "Outline"});
    private final SliderSetting outlineWidth = new SliderSetting("Outline Width", 4, 1, 10, 0.5);

    private final ColorSetting starColor = new ColorSetting("Star Color", new Color(0, 255, 0, 255));
    private final ColorSetting tankColor = new ColorSetting("Tank Color", new Color(255, 0, 0, 255));
    private final ColorSetting miniColor = new ColorSetting("Mini Color", new Color(255, 255, 0, 255));
    private final ColorSetting felColor = new ColorSetting("Fel Color", new Color(0, 255, 255, 255));
    private final ColorSetting assassinColor = new ColorSetting("Assassin Color", new Color(128, 0, 128, 255));
    private final ColorSetting mimicColor = new ColorSetting("Mimic Color", new Color(255, 255, 255, 255));

    public MobHighlight() {
        super("MobHighlight", category.Dungeon);
        this.registerSetting(dontShowInvisibleMobs);
        this.registerSetting(highlightFel);
        this.registerSetting(highlightMimic);
        this.registerSetting(highlightMidas);
        this.registerSetting(highlightMiniBoss);
        this.registerSetting(highlightTankMob);
        this.registerSetting(highlightStarMob);
        this.registerSetting(currentHighlight);
        this.registerSetting(outlineWidth);
        this.registerSetting(starColor);
        this.registerSetting(tankColor);
        this.registerSetting(miniColor);
        this.registerSetting(felColor);
        this.registerSetting(assassinColor);
        this.registerSetting(mimicColor);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!DungeonUtils.isInDungeon()) return;
        if (mc.player != null && mc.player.hasEffect(MobEffects.BLINDNESS)) return;

        PoseStack stack = event.getMatrix();

        List<Entity> targetEntities = new ArrayList<>();
        List<MobType> targetTypes = new ArrayList<>();

        AABB searchBox = new AABB(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        for (Entity entity : mc.level.getEntities(mc.player, searchBox, e -> true)) {
            if (entity instanceof ArmorStand armorStand) {
                MobType type = getType(armorStand);
                if (type != null) {
                    int idOffset = getIdOffset(armorStand);
                    if (idOffset >= 0) {
                        int id = armorStand.getId() - idOffset;
                        Entity target = mc.level.getEntity(id);
                        if (target != null && target.isAlive() && !(target instanceof ArmorStand)) {
                            targetEntities.add(target);
                            targetTypes.add(type);
                        }
                    }
                }
            } else if (entity instanceof Player player && isShadowAssassin(player)) {
                targetEntities.add(player);
                targetTypes.add(MobType.ASSASSIN);
            }
        }

        for (int i = 0; i < targetEntities.size(); i++) {
            Entity target = targetEntities.get(i);
            MobType type = targetTypes.get(i);

            if (dontShowInvisibleMobs.isToggled() && target.isInvisible() && target instanceof Player) continue;

            AABB box = getAABB(target);
            Color color = new Color(getFilledColor(type), true);

            String mode = currentHighlight.getOption();
            boolean doFill = mode == null || mode.equals("Filled") || mode.equals("Both");
            boolean doOutline = mode == null || mode.equals("Outline") || mode.equals("Both");

            if (doFill) {
                RenderUtils.drawBoxFilled(stack, box, color, true);
            }
            if (doOutline) {
                RenderUtils.drawBox(stack, box, color, (float) outlineWidth.getInput(), true);
            }
        }
    }

    private MobType getType(ArmorStand armorStand) {
        Component text = armorStand.getCustomName();
        if (text == null) return null;
        String name = text.getString();

        if (name.contains("King Midas") && highlightMidas.isToggled()) return MobType.MINI;
        if (name.contains("Mimic") && highlightMimic.isToggled()) return MobType.MIMIC;
        if (name.contains("Fel") && highlightFel.isToggled()) return MobType.FEL;
        if (isMiniBoss(name) && highlightMiniBoss.isToggled()) return MobType.MINI;
        if (isTankMob(name) && highlightTankMob.isToggled()) return MobType.TANK;

        if (DungeonUtils.isStarMob(armorStand) && highlightStarMob.isToggled()) return MobType.STAR;

        return null;
    }

    private int getIdOffset(ArmorStand armorStand) {
        Component text = armorStand.getCustomName();
        if (text == null) return -1;
        String name = text.getString();
        if (name.toLowerCase().contains("withermancer")) return 3;
        return 1;
    }

    private boolean isTankMob(String name) {
        return name.contains("Zombie Commander") || name.contains("Zombie Lord") ||
               name.contains("Skeleton Lord") || name.contains("Withermancer") ||
               name.contains("Super Archer");
    }

    private boolean isMiniBoss(String name) {
        return name.contains("Lost Adventurer") || name.contains("Angry Archaeologist") ||
               name.contains("Frozen Adventurer");
    }

    private boolean isShadowAssassin(Player player) {
        if (player == mc.player) return false;

        ItemStack heldItem = player.getMainHandItem();
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);

        Component text = heldItem.getCustomName();
        if (text == null) return false;
        if (!text.getString().contains("Silent Death")) return false;

        return boots != null && boots.getItem() == Items.LEATHER_BOOTS;
    }

    private int getFilledColor(MobType mob) {
        return switch (mob) {
            case STAR -> starColor.getRGB();
            case TANK -> tankColor.getRGB();
            case MINI -> miniColor.getRGB();
            case FEL -> felColor.getRGB();
            case ASSASSIN -> assassinColor.getRGB();
            case MIMIC -> mimicColor.getRGB();
        };
    }

    private AABB getAABB(Entity entity) {
        AABB box = entity.getBoundingBox();

        if (entity instanceof EnderMan && entity.isInvisible() && dontShowInvisibleMobs.isToggled()) {
            box = box.inflate(0, -1.8, 0).move(0, -1.2, 0);
        }

        if (entity instanceof Zombie zombie) {
            if (zombie.isBaby()) {
                box = box.inflate(0.15, 0.2, 0.15);
            }
        }
        return box;
    }
}
