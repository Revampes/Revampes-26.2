package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.StringUtils;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraRenderUtil;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;

import java.awt.Color;

/**
 * Ported from IQAddons (features/kuudra/waypoints/StunWaypointsFeature).
 * Shows a stun waypoint while a Human Cannonball is active, then a post-eaten
 * waypoint once the player drops below Y 50.
 */
public class StunWaypoints extends Module {

    private static final Vec3 ENTER_POS = new Vec3(-161, 49, -186);
    private static final double EATEN_Y_THRESHOLD = 50.0;

    private volatile boolean stunPhase = false;
    private volatile boolean eaten = false;

    private final ColorSetting color = new ColorSetting("Color", new Color(0, 245, 255, 200));
    private final SelectSetting style = new SelectSetting("Style", 1, new String[]{"Solid", "Outline", "Both", "None"});
    private final SelectSetting waypoint = new SelectSetting("Waypoint", 1, new String[]{"Right Pod", "Left Pod", "Back Pod"});

    public StunWaypoints() {
        super("Stun Waypoints", category.Kuudra);
        this.registerSetting(color);
        this.registerSetting(style);
        this.registerSetting(waypoint);
    }

    @Override
    public void onDisable() {
        this.stunPhase = false;
        this.eaten = false;
    }

    private boolean inPhase() {
        KuudraPhase phase = KuudraState.get().phase();
        return phase == KuudraPhase.BUILD || phase == KuudraPhase.STUN
                || phase == KuudraPhase.EATEN || phase == KuudraPhase.DPS;
    }

    @EventHandler
    public void onChat(ReceiveMessageEvent event) {
        if (!inPhase()) return;

        String msg = StringUtils.stripFormatting(event.getMessage().getString());
        if (msg.contains("You purchased Human Cannonball!")) {
            this.stunPhase = true;
            return;
        }

        if (msg.contains("destroyed one of Kuudra's pods!")) {
            this.stunPhase = false;
            this.eaten = false;
        }
    }

    @EventHandler
    public void onTick(PreUpdateEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (!inPhase()) return;

        if (mc.player.getY() < EATEN_Y_THRESHOLD && this.stunPhase) {
            this.stunPhase = false;
            this.eaten = true;
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (!this.eaten && !this.stunPhase) return;
        if (mc.player == null) return;
        if (!inPhase()) return;

        Vec3 selected = getSelectedWaypoint();
        Vec3 renderPos = this.stunPhase
                ? selected.add(getInterpolatedPlayerPos(event).subtract(ENTER_POS))
                : selected;

        float half = 0.5f;
        AABB waypointBox = new AABB(
                renderPos.x() - half, renderPos.y(), renderPos.z() - half,
                renderPos.x() + half, renderPos.y() + 1.0, renderPos.z() + half
        );

        KuudraRenderUtil.drawStyledBox(event.getMatrix(), waypointBox, true,
                color.getColor(), KuudraRenderUtil.parseStyle(style.getOption()));
    }

    private Vec3 getSelectedWaypoint() {
        return switch (waypoint.getOption()) {
            case "Right Pod" -> new Vec3(-168, 28, -168);
            case "Back Pod" -> new Vec3(-156, 28, -157);
            default -> new Vec3(-152.5, 27, -172.5);
        };
    }

    private Vec3 getInterpolatedPlayerPos(Render3DEvent event) {
        if (mc.player == null) return Vec3.ZERO;

        float partialTicks = event.getDelta();
        double x = mc.player.xo + (mc.player.getX() - mc.player.xo) * partialTicks;
        double y = mc.player.yo + (mc.player.getY() - mc.player.yo) * partialTicks;
        double z = mc.player.zo + (mc.player.getZ() - mc.player.zo) * partialTicks;
        return new Vec3(x, y, z);
    }
}
