package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.ItemUseEvent;
import xyz.whatsyouss.frosty.events.impl.ParticleEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.hud.HudManager;
import xyz.whatsyouss.frosty.hud.impl.FireVeilOverlayWidget;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.StringUtils;
import xyz.whatsyouss.frosty.utility.kuudra.FireVeilOverlayManager;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraRenderUtil;

import java.awt.Color;
import java.util.Locale;

/**
 * Ported from IQAddons (features/kuudra/miscellaneous/FireVeilOverlayFeature) plus its
 * countdown HUD widget. Shows the Fire Veil radius as a ring or a wireframe wall from
 * the moment the wand is used, hides the vanilla flame particles while it is active and
 * can chime when the ability is ready again.
 */
public class FireVeilOverlay extends Module {

    private static final String FIRE_VEIL_ID = "FIRE_VEIL_WAND";
    private static final String FIRE_VEIL_NAME = "fire veil";
    private static final float FIRE_VEIL_RADIUS = 3.5f;
    private static final float FIRE_VEIL_CIRCLE_THICKNESS = 0.08f;
    private static final int FIRE_VEIL_CIRCLE_SEGMENTS = 60;
    private static final float FIRE_VEIL_WALL_HEIGHT = 1.0f;

    private final FireVeilOverlayManager manager = FireVeilOverlayManager.get();

    private final ColorSetting color = new ColorSetting("Color", new Color(255, 140, 0, 200));
    private final SelectSetting render = new SelectSetting("Render", 1, new String[]{"Default", "Circle", "Wall"});
    private final ButtonSetting renderThroughWalls = new ButtonSetting("Through Walls", true);
    private final ButtonSetting abilityCountdown = new ButtonSetting("Ability Countdown", true);
    private final ButtonSetting soundWhenRecast = new ButtonSetting("Ready Sound", false);

    public FireVeilOverlay() {
        super("Fire Veil Overlay", category.Kuudra);
        this.defaultEnabled = true;
        this.registerSetting(color);
        this.registerSetting(render);
        this.registerSetting(renderThroughWalls);
        this.registerSetting(abilityCountdown);
        this.registerSetting(soundWhenRecast);

        HudManager.register(new FireVeilOverlayWidget(this));
    }

    @Override
    public void onEnable() {
        manager.reset();
    }

    @Override
    public void onDisable() {
        manager.reset();
    }

    public FireVeilOverlayManager getManager() {
        return manager;
    }

    public boolean isCountdownEnabled() {
        return abilityCountdown.isToggled();
    }

    private boolean customRender() {
        return render.getValue() != 0;
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;
        if (!soundWhenRecast.isToggled()) return;

        if (manager.consumeReadySound(System.currentTimeMillis())) {
            mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 2.0f, 1.6f);
        }
    }

    @EventHandler
    public void onItemUse(ItemUseEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!isFireVeil(event.getItemStack())) return;

        manager.recordCast(System.currentTimeMillis());
    }

    @EventHandler
    public void onParticle(ParticleEvent event) {
        if (!customRender()) return;
        if (!manager.shouldHideDefaultParticles(System.currentTimeMillis())) return;
        if (event.getType() != ParticleTypes.FLAME) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (mc.player == null) return;
        if (!customRender()) return;
        if (!manager.isAbilityActive(System.currentTimeMillis())) return;

        Vec3 center = getInterpolatedPlayerPos(event);
        boolean throughWalls = renderThroughWalls.isToggled();
        Color renderColor = color.getColor();

        if (render.getValue() == 2) {
            KuudraRenderUtil.drawCircleWireframeWall(
                    event.getMatrix(), center, FIRE_VEIL_RADIUS, FIRE_VEIL_WALL_HEIGHT,
                    FIRE_VEIL_CIRCLE_SEGMENTS, throughWalls, renderColor
            );
            return;
        }

        KuudraRenderUtil.drawThickCircleOutline(
                event.getMatrix(), center, FIRE_VEIL_RADIUS, FIRE_VEIL_CIRCLE_THICKNESS,
                FIRE_VEIL_CIRCLE_SEGMENTS, throughWalls, renderColor
        );
    }

    private Vec3 getInterpolatedPlayerPos(Render3DEvent event) {
        float partialTicks = event.getDelta();
        double x = mc.player.xo + (mc.player.getX() - mc.player.xo) * partialTicks;
        double y = mc.player.yo + (mc.player.getY() - mc.player.yo) * partialTicks + 0.02;
        double z = mc.player.zo + (mc.player.getZ() - mc.player.zo) * partialTicks;
        return new Vec3(x, y, z);
    }

    private boolean isFireVeil(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var tag = customData.copyTag();

            var itemId = tag.getString("id");
            if (itemId.isPresent() && itemId.get().equals(FIRE_VEIL_ID)) return true;

            var extraAttributes = tag.getCompound("ExtraAttributes");
            if (extraAttributes.isPresent()) {
                var extraItemId = extraAttributes.get().getString("id");
                if (extraItemId.isPresent() && extraItemId.get().equals(FIRE_VEIL_ID)) return true;
            }
        }

        String name = StringUtils.stripFormatting(stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
        return name.contains(FIRE_VEIL_NAME);
    }
}
