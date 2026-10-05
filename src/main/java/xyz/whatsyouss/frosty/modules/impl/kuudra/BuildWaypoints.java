package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.ArmorStandRenderEvent;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.EntityDetectorUtil;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraRenderUtil;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;

import java.awt.Color;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ported from IQAddons (features/kuudra/waypoints/BuildWaypointsFeature).
 * Replaces Kuudra's default pile progress holograms with colour-coded beams during
 * the BUILD phase.
 */
public class BuildWaypoints extends Module {

    private static final Pattern PROGRESS_PATTERN = Pattern.compile("PROGRESS:\\s*(?:§.)?(\\d+)%");
    private static final int UPDATE_INTERVAL_TICKS = 2;
    private static final int BEAM_HEIGHT = 25;

    private static final Color COLOR_0_20 = new Color(168, 0, 0, 255);
    private static final Color COLOR_21_40 = new Color(255, 0, 0, 255);
    private static final Color COLOR_41_60 = new Color(255, 135, 0, 255);
    private static final Color COLOR_61_80 = new Color(46, 130, 0, 255);
    private static final Color COLOR_81_100 = new Color(125, 218, 88, 255);

    private final SliderSetting opacity = new SliderSetting("Opacity", 0.5, 0.0, 1.0, 0.05);
    private final ButtonSetting hideDefaultText = new ButtonSetting("Hide Default Pile Text", true);

    private final List<BuildPile> buildPiles = new CopyOnWriteArrayList<>();

    private int tickCounter = 0;

    public BuildWaypoints() {
        super("Build Overlay", category.Kuudra);
        this.registerSetting(opacity);
        this.registerSetting(hideDefaultText);
    }

    @Override
    public void onDisable() {
        this.buildPiles.clear();
    }

    @EventHandler
    public void onTick(PreUpdateEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (KuudraState.get().phase() != KuudraPhase.BUILD) return;

        tickCounter++;
        if (tickCounter % UPDATE_INTERVAL_TICKS != 0) return;

        List<BuildPile> newPiles = EntityDetectorUtil.getAllArmorStands().stream()
                .filter(this::isProgressStand)
                .map(this::createBuildPile)
                .filter(Objects::nonNull)
                .toList();

        this.buildPiles.clear();
        this.buildPiles.addAll(newPiles);
    }

    @EventHandler
    public void onArmorStandRender(ArmorStandRenderEvent event) {
        if (!hideDefaultText.isToggled()) return;

        var state = event.getRenderState();
        if (state == null || state.nameTag == null) return;

        String stripped = state.nameTag.getString().replaceAll("§.", "");
        if (stripped.contains("PROGRESS:") && stripped.contains("%")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (KuudraState.get().phase() != KuudraPhase.BUILD) return;

        for (BuildPile pile : buildPiles) {
            Color progressColor = getColorForProgress(pile.progress());

            Vec3 beaconPos = new Vec3(pile.position().x - 0.5, pile.position().y, pile.position().z - 0.5);
            KuudraRenderUtil.drawStyledWithBeam(
                    event.getMatrix(),
                    AABB.unitCubeFromLowerCorner(beaconPos),
                    BEAM_HEIGHT, false,
                    KuudraRenderUtil.withOpacity(progressColor, (float) opacity.getInput()),
                    KuudraRenderUtil.RenderStyle.BOTH
            );

            Vec3 textPos = new Vec3(pile.position().x, pile.position().y + 2, pile.position().z);
            KuudraRenderUtil.drawText(event.getMatrix(), textPos, pile.displayName(), 0.05f, true, progressColor);
        }
    }

    private boolean isProgressStand(ArmorStand stand) {
        if (!stand.hasCustomName() || stand.getCustomName() == null) {
            return false;
        }

        String name = stand.getCustomName().getString();
        return name.contains("PROGRESS:") && name.contains("%");
    }

    private BuildPile createBuildPile(ArmorStand stand) {
        String name = Objects.requireNonNull(stand.getCustomName()).getString();
        int progress = extractProgress(name);
        if (progress < 0) return null;

        return new BuildPile(
                new Vec3(stand.getX(), stand.getY(), stand.getZ()),
                name,
                progress
        );
    }

    private int extractProgress(String name) {
        String stripped = name.replaceAll("§.", "");
        Matcher matcher = PROGRESS_PATTERN.matcher(stripped);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                String numbers = stripped.replaceAll("[^0-9]", "");
                if (!numbers.isEmpty()) {
                    try {
                        return Integer.parseInt(numbers);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return -1;
    }

    private Color getColorForProgress(int progress) {
        if (progress <= 20) return COLOR_0_20;
        if (progress <= 40) return COLOR_21_40;
        if (progress <= 60) return COLOR_41_60;
        if (progress <= 80) return COLOR_61_80;
        return COLOR_81_100;
    }

    private record BuildPile(Vec3 position, String displayName, int progress) {
    }
}
