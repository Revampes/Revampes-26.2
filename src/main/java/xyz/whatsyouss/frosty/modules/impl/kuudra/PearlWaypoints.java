package xyz.whatsyouss.frosty.modules.impl.kuudra;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.SupplyDropEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.SupplyPickupEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.SupplyPlaceEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.SupplyProgressEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.StringUtils;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraRenderUtil;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraTier;
import xyz.whatsyouss.frosty.utility.kuudra.SupplyState;
import xyz.whatsyouss.frosty.utility.pearl.AreaDetectionUtil;
import xyz.whatsyouss.frosty.utility.pearl.BoundingBox2D;
import xyz.whatsyouss.frosty.utility.pearl.BuiltInPearlWaypoints;
import xyz.whatsyouss.frosty.utility.pearl.PearlAimSolution;
import xyz.whatsyouss.frosty.utility.pearl.PearlTalismanTier;
import xyz.whatsyouss.frosty.utility.pearl.PearlTrajectorySolver;
import xyz.whatsyouss.frosty.utility.pearl.PearlTrajectoryType;
import xyz.whatsyouss.frosty.utility.pearl.PearlWaypoint;
import xyz.whatsyouss.frosty.utility.pearl.WaypointArea;

/**
 * Ported from IQAddons (features/kuudra/waypoints/PearlWaypointFeature).
 *
 * <p>Renders the JSON-configured pearl throw waypoints by solving the real projectile
 * arc with {@link PearlTrajectorySolver}, so the marker sits on the actual pearl path and
 * the per-waypoint timer is derived from the solved flight time plus the supply window of
 * the current Kuudra tier / talisman tier. Waypoints are matched to the area the player
 * stands in and can be filtered by the missing pre.
 */
public class PearlWaypoints extends Module {

    private static final int AREA_CHECK_INTERVAL = 2;
    private static final int TIMER_UPDATE_STEP_MS = 5;
    private static final int CIRCLE_SEGMENTS = 48;
    private static final float CIRCLE_THICKNESS = 0.035f;
    private static final float MIN_WAYPOINT_SIZE = 0.05f;
    private static final double PEARL_LAUNCH_EYE_Y_OFFSET = -0.10000000149011612;
    private static final double FLAT_SERVER_LAUNCH_Y_OFFSET = 1.5;
    private static final double TIMER_TEXT_GAP = 0.18;
    private static final double TIMER_TEXT_ABOVE_EXTRA_GAP = 0.22;
    private static final Vec3 WORLD_UP = new Vec3(0.0, 1.0, 0.0);
    private static final double PILE_BEACON_CENTER_OFFSET = 0.0;
    private static final double PILE_BEACON_TARGET_Y_OFFSET = 0.0;
    private static final double FLAT_AREA_Y_OFFSET_SCALE = 0.01;
    private static final double TEXT_REFERENCE_DISTANCE = 24.0;
    private static final double MIN_TEXT_DISTANCE_SCALE = 0.75;
    private static final double MAX_TEXT_DISTANCE_SCALE = 4.0;
    private static final double AREA_DEBUG_FLOOR_Y = 76.05;
    private static final double AREA_DEBUG_FLOOR_HEIGHT = 0.12;
    private static final double AREA_DEBUG_WALL_MIN_Y = 76.0;
    private static final double AREA_DEBUG_WALL_MAX_Y = 82.0;
    private static final float AREA_DEBUG_TEXT_SCALE = 0.08f;
    private static final long PICKUP_PROGRESS_TIMEOUT_MS = 750L;
    private static final long READY_FALLBACK_TIMEOUT_MS = 5_000L;
    private static final List<Integer> SUPPLY_TICK_PERCENTAGES = List.of(
            5, 11, 17, 23, 29, 35, 41,
            47, 53, 59, 65, 71, 77, 83,
            89, 95, 100
    );

    private final AreaDetectionUtil areaDetection = new AreaDetectionUtil();
    private final SupplyState supplyState = SupplyState.get();
    private final Set<String> alertedWaypoints = new HashSet<>();
    private List<WaypointArea> loadedAreas = List.of();

    private final ColorSetting color = new ColorSetting("Color", new Color(0, 255, 255, 255));
    private final SliderSetting sizeAdjust = new SliderSetting("Size Adjustment", 0, -5, 5, 1);
    private final SliderSetting textScale = new SliderSetting("Text Scale", 0.08, 0.02, 0.12, 0.01);
    private final SliderSetting ping = new SliderSetting("Average Ping (ms)", 100, 0, 500, 1);
    private final SelectSetting talismanTier = new SelectSetting("Talisman Tier", 3, new String[]{"None", "Tier 1", "Tier 2", "Tier 3"});
    private final SelectSetting times = new SelectSetting("Timer Type", 1, new String[]{"Timer Ms", "Timer Seconds", "Timer Ticks"});
    private final SelectSetting textPosition = new SelectSetting("Timer Position", 1, new String[]{"Above", "Below"});
    private final SelectSetting renderStyleSetting = new SelectSetting("Render Style", 3, new String[]{"Full Block", "Filled Outline", "Block Outline", "Square", "Circle"});
    private final ButtonSetting blockOutlines = new ButtonSetting("Stand Block Outlines", true);
    private final ButtonSetting throwAlert = new ButtonSetting("Throw Alert", true);
    private final ButtonSetting areaDebug = new ButtonSetting("Area Debug", false);
    private final SliderSetting flatXAreaYOffset = new SliderSetting("Flat X Y Offset", 0, -100, 100, 1);
    private final SliderSetting flatEqualsAreaYOffset = new SliderSetting("Flat Equals Y Offset", 0, -100, 100, 1);
    private final SliderSetting flatSlashAreaYOffset = new SliderSetting("Flat Slash Y Offset", 0, -100, 100, 1);
    private final SliderSetting flatTriangleAreaYOffset = new SliderSetting("Flat Triangle Y Offset", 0, -100, 100, 1);
    private final SliderSetting flatSquareAreaYOffset = new SliderSetting("Flat Square Y Offset", 0, -100, 100, 1);
    private final SliderSetting flatShopAreaYOffset = new SliderSetting("Flat Shop Y Offset", 0, -100, 100, 1);

    private int lastSupplyProgress = 0;
    private int lastSupplyProgressIndex = -1;
    private long supplyProgressStartMs = -1L;
    private long lastPickupProgressMs = -1L;
    private long readyStartedMs = -1L;
    private int tickCounter = 0;

    public PearlWaypoints() {
        super("Pearl Waypoints", category.Kuudra);
        this.registerSetting(color);
        this.registerSetting(sizeAdjust);
        this.registerSetting(textScale);
        this.registerSetting(ping);
        this.registerSetting(talismanTier);
        this.registerSetting(times);
        this.registerSetting(textPosition);
        this.registerSetting(renderStyleSetting);
        this.registerSetting(blockOutlines);
        this.registerSetting(throwAlert);
        this.registerSetting(areaDebug);
        this.registerSetting(flatXAreaYOffset);
        this.registerSetting(flatEqualsAreaYOffset);
        this.registerSetting(flatSlashAreaYOffset);
        this.registerSetting(flatTriangleAreaYOffset);
        this.registerSetting(flatSquareAreaYOffset);
        this.registerSetting(flatShopAreaYOffset);
    }

    @Override
    public void onEnable() {
        List<WaypointArea> areas = BuiltInPearlWaypoints.getAreas();
        loadedAreas = areas;
        areaDetection.setAreas(areas);
        resetState();
    }

    @Override
    public void onDisable() {
        areaDetection.reset();
        loadedAreas = List.of();
        resetState();
    }

    private boolean inSuppliesPhase() {
        return KuudraState.get().phase() == KuudraPhase.SUPPLIES;
    }

    @EventHandler
    public void onTick(PreUpdateEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (!inSuppliesPhase()) return;

        tickCounter++;
        if (tickCounter % AREA_CHECK_INTERVAL == 0) {
            areaDetection.update();
        }

        long now = System.currentTimeMillis();
        if (lastPickupProgressMs > 0L && now - lastPickupProgressMs > PICKUP_PROGRESS_TIMEOUT_MS) {
            clearPickupState();
        }
        if (readyStartedMs > 0L && now - readyStartedMs > READY_FALLBACK_TIMEOUT_MS) {
            clearPickupState();
        }
    }

    @EventHandler
    public void onSupplyProgress(SupplyProgressEvent event) {
        lastSupplyProgress = event.getCurrentProgress();
        int previousIndex = lastSupplyProgressIndex;
        lastSupplyProgressIndex = getProgressIndex(lastSupplyProgress);

        long now = System.currentTimeMillis();
        lastPickupProgressMs = now;
        if (lastSupplyProgress <= 0 || supplyProgressStartMs < 0L) {
            readyStartedMs = -1L;
            alertedWaypoints.clear();
        }

        if (lastSupplyProgress <= 0) {
            supplyProgressStartMs = now;
        } else if (lastSupplyProgressIndex >= 0
                && (supplyProgressStartMs < 0 || previousIndex != lastSupplyProgressIndex)
        ) {
            supplyProgressStartMs = now - getProgressElapsedMs(lastSupplyProgressIndex);
        }
    }

    @EventHandler
    public void onSupplyDrop(SupplyDropEvent event) {
        if (isLocalPlayer(event.playerName())) resetState();
    }

    @EventHandler
    public void onSupplyPlace(SupplyPlaceEvent event) {
        if (isLocalPlayer(event.playerName())) resetState();
    }

    @EventHandler
    public void onSupplyPickup(SupplyPickupEvent event) {
        resetState();
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (!inSuppliesPhase()) return;
        if (mc.player == null) return;

        WaypointArea area = areaDetection.getCurrentArea();

        if (areaDebug.isToggled()) {
            renderAreaDebug(event, area);
        }

        if (area == null) return;

        Vec3 commonStandBlockCenter = getCommonStandBlockCenter(area);
        int missingPre = supplyState.getMissingPre();

        for (PearlWaypoint waypoint : area.waypoints()) {
            if (!waypoint.shouldShow(missingPre)) continue;

            RenderData renderData = buildRenderData(area, waypoint, commonStandBlockCenter);
            if (renderData == null) continue;

            renderWaypoint(event, area, waypoint, renderData);
        }

        renderAreaStandBlockOutlines(event, area);
    }

    private void renderAreaDebug(Render3DEvent event, WaypointArea currentArea) {
        for (int i = 0; i < loadedAreas.size(); i++) {
            WaypointArea area = loadedAreas.get(i);
            Color color = getAreaDebugColor(i, area == currentArea);
            BoundingBox2D bounds = area.bounds();

            AABB floor = new AABB(
                    bounds.minX(),
                    AREA_DEBUG_FLOOR_Y,
                    bounds.minZ(),
                    bounds.maxX() + 1.0,
                    AREA_DEBUG_FLOOR_Y + AREA_DEBUG_FLOOR_HEIGHT,
                    bounds.maxZ() + 1.0
            );
            AABB wallOutline = new AABB(
                    bounds.minX(),
                    AREA_DEBUG_WALL_MIN_Y,
                    bounds.minZ(),
                    bounds.maxX() + 1.0,
                    AREA_DEBUG_WALL_MAX_Y,
                    bounds.maxZ() + 1.0
            );

            KuudraRenderUtil.drawFilled(event.getMatrix(), floor, true, withOpacity(color, area == currentArea ? 0.28f : 0.16f));
            KuudraRenderUtil.drawOutline(event.getMatrix(), wallOutline, true, color, area == currentArea ? 3.0f : 1.5f);
            KuudraRenderUtil.drawText(
                    event.getMatrix(),
                    floor.getCenter().add(0.0, AREA_DEBUG_WALL_MAX_Y - AREA_DEBUG_FLOOR_Y + 0.35, 0.0),
                    area.name(),
                    AREA_DEBUG_TEXT_SCALE,
                    true,
                    withOpacity(color, 1.0f)
            );
        }
    }

    private Color getAreaDebugColor(int index, boolean currentArea) {
        Color color = switch (index % 7) {
            case 0 -> new Color(255, 85, 85, 255);
            case 1 -> new Color(85, 170, 255, 255);
            case 2 -> new Color(85, 255, 135, 255);
            case 3 -> new Color(255, 205, 85, 255);
            case 4 -> new Color(255, 85, 255, 255);
            case 5 -> new Color(85, 255, 255, 255);
            default -> new Color(255, 255, 255, 255);
        };
        return currentArea ? color : withOpacity(color, 0.78f);
    }

    private RenderData buildRenderData(WaypointArea area, PearlWaypoint waypoint, Vec3 commonStandBlockCenter) {
        if (waypoint.usesTrajectoryProjection()) {
            Vec3 origin = getPearlSpawnPosition(waypoint.trajectoryType());
            Vec3 columnTarget = getPileBeaconColumnTarget(waypoint);
            PearlAimSolution solution = solveWaypointTrajectory(area, waypoint, origin, columnTarget);
            if (solution == null) {
                return null;
            }

            Vec3 renderTarget = getProjectedRenderTarget(waypoint, solution.direction().normalize());
            return new RenderData(renderTarget, solution.flightTicks(), true, solution.direction().normalize());
        }

        return new RenderData(getStaticRenderTarget(area, waypoint, commonStandBlockCenter), -1.0, false, null);
    }

        private PearlAimSolution solveWaypointTrajectory(
            WaypointArea area,
            PearlWaypoint waypoint,
            Vec3 origin,
            Vec3 target
        ) {
        if (waypoint.trajectoryType() == PearlTrajectoryType.FLAT) {
            return PearlTrajectorySolver.solveFlatForDisplay(
                origin,
                target,
                mc.level,
                mc.player,
                isSquareLongFlatWaypoint(area, waypoint)
            );
        }

        return PearlTrajectorySolver.solve(
            origin,
            target,
            waypoint.trajectoryType(),
            mc.level,
            mc.player
        );
        }

    private Vec3 applyFlatAreaYOffset(WaypointArea area, PearlWaypoint waypoint, Vec3 target) {
        // Projected FLAT markers must stay on the solved ray; legacy visual offsets change the throw.
        if (waypoint.trajectoryType() != PearlTrajectoryType.FLAT || waypoint.usesTrajectoryProjection()) {
            return target;
        }

        int offset = getFlatAreaYOffset(area.name());

        if (offset == 0) {
            return target;
        }
        return target.add(0.0, offset * FLAT_AREA_Y_OFFSET_SCALE, 0.0);
    }

    private int getFlatAreaYOffset(String areaName) {
        return switch (areaName.toLowerCase(Locale.ROOT)) {
            case "x" -> (int) flatXAreaYOffset.getInput();
            case "equals" -> (int) flatEqualsAreaYOffset.getInput();
            case "slash" -> (int) flatSlashAreaYOffset.getInput();
            case "triangle" -> (int) flatTriangleAreaYOffset.getInput();
            case "square" -> (int) flatSquareAreaYOffset.getInput();
            case "shop" -> (int) flatShopAreaYOffset.getInput();
            default -> 0;
        };
    }

    private Vec3 getProjectedRenderTarget(PearlWaypoint waypoint, Vec3 direction) {
        Vec3 eyePosition = getPlayerEyePosition();
        double renderDistance = waypoint.projectionDistance() > 0.0
                ? waypoint.projectionDistance()
                : 13.0;
        return eyePosition.add(direction.scale(renderDistance));
    }

    private boolean isSquareLongFlatWaypoint(WaypointArea area, PearlWaypoint waypoint) {
        String label = waypoint.label().toLowerCase(Locale.ROOT);
        return area.name().equalsIgnoreCase("square")
                && waypoint.trajectoryType() == PearlTrajectoryType.FLAT
                && (label.equals("square - triangle") || label.equals("square - shop"));
    }

    private Vec3 getPileBeaconColumnTarget(PearlWaypoint waypoint) {
        return waypoint.target().add(PILE_BEACON_CENTER_OFFSET, PILE_BEACON_TARGET_Y_OFFSET, PILE_BEACON_CENTER_OFFSET);
    }

    private Vec3 getPlayerEyePosition() {
        return mc.player.position().add(0.0, mc.player.getEyeHeight(), 0.0);
    }

    private Vec3 getPearlSpawnPosition(PearlTrajectoryType type) {
        if (type == PearlTrajectoryType.FLAT) {
            return mc.player.position().add(0.0, FLAT_SERVER_LAUNCH_Y_OFFSET, 0.0);
        }
        return mc.player.getEyePosition().add(0.0, PEARL_LAUNCH_EYE_Y_OFFSET, 0.0);
    }

    private void renderWaypoint(
            Render3DEvent event,
            WaypointArea area,
            PearlWaypoint waypoint,
            RenderData renderData
    ) {
        float size = getAdjustedSize(waypoint);
        Vec3 target = applyFlatAreaYOffset(area, waypoint, getMarkerRenderCenter(renderData, size));
        if (waypoint.trajectoryType() == PearlTrajectoryType.FLAT && renderData.direction() != null
                && mc.options.getCameraType().isFirstPerson()) {
            // Anchor to the rendered camera, including interpolation and crouch transitions.
            double distance = waypoint.projectionDistance() > 0.0 ? waypoint.projectionDistance() : 13.0;
            target = new Vec3(event.getOffsetX(), event.getOffsetY(), event.getOffsetZ())
                    .add(renderData.direction().scale(distance));
        }
        if (target.x == 0 && target.y == 0 && target.z == 0) return;

        AABB box = makeWaypointBox(target, size);
        Color color = getWaypointColor(waypoint);

        TimerState timer = renderData.trajectory()
                ? getTrajectoryTimer(waypoint, renderData.flightTicks())
                : getProgressTimer(waypoint);

        if (timer.ready() && throwAlert.isToggled() && waypoint.alert()) {
            color = new Color(0, 255, 0, 0xff);
            if (readyStartedMs < 0L) {
                readyStartedMs = System.currentTimeMillis();
            }
            playAlertOnce(area.name() + ":" + waypoint.label() + ":" + waypoint.target());
        }

        renderShape(event, box, target, size, color);

        if (!timer.text().isBlank()) {
            Vec3 textPosition = getTimerTextPosition(target, size);
            KuudraRenderUtil.drawText(
                    event.getMatrix(),
                    textPosition,
                    timer.text(),
                    getDistanceCompensatedTextScale(textPosition),
                    true,
                    timer.timerColor()
            );
        }
    }

    private Vec3 getMarkerRenderCenter(RenderData renderData, float size) {
        if (!renderData.trajectory()) {
            return renderData.position();
        }
        return renderData.position();
    }

    private void renderShape(
            Render3DEvent event,
            AABB box,
            Vec3 center,
            float size,
            Color color
    ) {
        RenderStyle style = getRenderStyle();
        Color fill = withOpacity(color, Math.min(color.getAlpha() / 255f, 0.8f));
        Color softFill = withOpacity(color, Math.min(color.getAlpha() / 255f, 0.35f));
        float radius = Math.max(MIN_WAYPOINT_SIZE, size * 0.75f);

        switch (style) {
            case FULL_BLOCK -> KuudraRenderUtil.drawFilled(event.getMatrix(), box, true, fill);
            case FILLED_OUTLINE -> {
                KuudraRenderUtil.drawFilled(event.getMatrix(), box, true, softFill);
                KuudraRenderUtil.drawOutline(event.getMatrix(), box, true, color);
            }
            case BLOCK_OUTLINE -> KuudraRenderUtil.drawOutline(event.getMatrix(), box, true, color);
            case SQUARE -> KuudraRenderUtil.drawBillboardSquareOutline(event.getMatrix(), center, radius * 2.0f, true, color);
            case CIRCLE -> KuudraRenderUtil.drawThickBillboardCircleOutline(event.getMatrix(), center, radius, CIRCLE_THICKNESS, CIRCLE_SEGMENTS, true, color);
        }
    }

    private void renderAreaStandBlockOutlines(Render3DEvent event, WaypointArea area) {
        if (!blockOutlines.isToggled()) {
            return;
        }

        Set<String> renderedBlocks = new HashSet<>();
        for (PearlWaypoint waypoint : area.waypoints()) {
            if (!waypoint.hasStandBlock()) continue;

            Vec3 block = waypoint.standBlock();
            String blockKey = block.x() + ":" + block.y() + ":" + block.z();
            if (!renderedBlocks.add(blockKey)) continue;

            AABB blockBox = new AABB(
                    block.x(), block.y(), block.z(),
                    block.x() + 1, block.y() + 1, block.z() + 1
            );
            KuudraRenderUtil.drawOutline(event.getMatrix(), blockBox, true, getWaypointColor(waypoint));
        }
    }

    private TimerState getTrajectoryTimer(PearlWaypoint waypoint, double flightTicks) {
        if (!isPickingSupply()) {
            return TimerState.hidden();
        }

        double elapsedTicks = getServerSyncedElapsedMs() / 50.0;
        double landingTick = waypoint.landingTick() != null
                ? waypoint.landingTick()
                : getSupplyWindowTicks(KuudraState.get().tier(), getTalismanTier())
                + getTrajectoryLandingOffset(waypoint);
        double pingTicks = ping.getInput() / 50.0;
        double throwTick = landingTick - flightTicks - pingTicks;
        double remainingTicks = throwTick - elapsedTicks;

        if (remainingTicks <= 0.0) {
            return TimerState.ready("READY");
        }

        long remainingMs = Math.round(remainingTicks * 50.0);
        return TimerState.countdown(formatTimerText(remainingMs), getPearlTimerColor(remainingMs, Math.round((float) landingTick * 50.0)));
    }

    private double getTrajectoryLandingOffset(PearlWaypoint waypoint) {
        if (waypoint.trajectoryType() == PearlTrajectoryType.DOUBLE_HIGH) {
            return waypoint.landingOffsetTicks();
        }
        return -waypoint.landingOffsetTicks();
    }

    private TimerState getProgressTimer(PearlWaypoint waypoint) {
        if (!isPickingSupply()) {
            return TimerState.staticOnly();
        }

        int targetIndex = getTargetIndex(waypoint.label());
        if (targetIndex < 0) {
            return TimerState.staticOnly();
        }

        if (lastSupplyProgressIndex >= targetIndex && lastSupplyProgressIndex >= 0) {
            return TimerState.ready("READY");
        }

        long remainingMs = getRemainingTimerMs(targetIndex);
        if (remainingMs <= 0L) {
            return TimerState.staticOnly();
        }

        return TimerState.countdown(formatTimerText(remainingMs), getPearlTimerColor(remainingMs, getProgressElapsedMs(targetIndex)));
    }

    private String formatTimerText(long remainingMs) {
        long roundedMs = Math.max(TIMER_UPDATE_STEP_MS, (remainingMs / TIMER_UPDATE_STEP_MS) * TIMER_UPDATE_STEP_MS);
        if (getTimerType() == TimerType.TIMER_SECONDS) {
            return String.format(Locale.ROOT, "%.2fs", roundedMs / 1000.0);
        }
        if (getTimerType() == TimerType.TIMER_TICKS) {
            return Math.max(1L, Math.round(roundedMs / 50.0)) + "t";
        }
        return roundedMs + "ms";
    }

    private Vec3 getTimerTextPosition(Vec3 target, float size) {
        double offset = getTimerTextOffset(size);
        Vec3 textAxis = getCameraRelativeTextAxis(target);
        if (getTextPosition() == TextPosition.ABOVE) {
            return target.add(textAxis.scale(offset + TIMER_TEXT_ABOVE_EXTRA_GAP));
        }
        return target.add(textAxis.scale(-offset));
    }

    private double getTimerTextOffset(float size) {
        return switch (getRenderStyle()) {
            case FULL_BLOCK, FILLED_OUTLINE, BLOCK_OUTLINE -> (size / 2.0) + TIMER_TEXT_GAP;
            case SQUARE, CIRCLE -> Math.max(MIN_WAYPOINT_SIZE, size * 0.75f) + TIMER_TEXT_GAP;
        };
    }

    private Vec3 getCameraRelativeTextAxis(Vec3 target) {
        if (mc.player == null) {
            return WORLD_UP;
        }

        Vec3 toTarget = target.subtract(getPlayerEyePosition());
        if (toTarget.lengthSqr() < 1.0E-6) {
            return WORLD_UP;
        }

        Vec3 viewDirection = toTarget.normalize();
        Vec3 right = viewDirection.cross(WORLD_UP);
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        }

        Vec3 screenUp = right.normalize().cross(viewDirection);
        if (screenUp.lengthSqr() < 1.0E-6) {
            return WORLD_UP;
        }
        return screenUp.normalize();
    }

    private float getDistanceCompensatedTextScale(Vec3 textPosition) {
        if (mc.player == null) {
            return (float) textScale.getInput();
        }

        double distance = getPlayerEyePosition().distanceTo(textPosition);
        double multiplier = Math.clamp(distance / TEXT_REFERENCE_DISTANCE, MIN_TEXT_DISTANCE_SCALE, MAX_TEXT_DISTANCE_SCALE);
        return (float) (textScale.getInput() * multiplier);
    }

    private Vec3 getStaticRenderTarget(WaypointArea area, PearlWaypoint waypoint, Vec3 commonStandBlockCenter) {
        Vec3 target = waypoint.target();
        if (commonStandBlockCenter == null || mc.player == null) {
            return target;
        }

        return target;
    }

    private float getAdjustedSize(PearlWaypoint waypoint) {
        int steps = Math.clamp((int) sizeAdjust.getInput(), -5, 5);
        double multiplier = 1.0 + (steps * 0.1);
        return (float) Math.max(MIN_WAYPOINT_SIZE, waypoint.size() * multiplier);
    }

    private AABB makeWaypointBox(Vec3 center, float size) {
        float half = size / 2f;
        return new AABB(
                center.x - half, center.y - half, center.z - half,
                center.x + half, center.y + half, center.z + half
        );
    }

    private Color getWaypointColor(PearlWaypoint waypoint) {
        Color waypointColor = waypoint.color();
        if (waypointColor == null || waypointColor.getAlpha() == 0) {
            return color.getColor();
        }
        return waypointColor;
    }

    private Vec3 getCommonStandBlockCenter(WaypointArea area) {
        PearlWaypoint waypoint = getStandBlockWaypoint(area);
        return waypoint != null ? waypoint.standBlock().add(0.5, 0.5, 0.5) : null;
    }

    private PearlWaypoint getStandBlockWaypoint(WaypointArea area) {
        for (PearlWaypoint waypoint : area.waypoints()) {
            if (waypoint.hasStandBlock()) {
                return waypoint;
            }
        }
        return null;
    }

    private int getTargetIndex(String label) {
        if (label.isBlank()) return -1;

        try {
            int value = Integer.parseInt(label.replace("%", "").trim());
            return SUPPLY_TICK_PERCENTAGES.indexOf(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private long getRemainingTimerMs(int targetIndex) {
        if (targetIndex < 0 || lastSupplyProgressIndex < 0 || supplyProgressStartMs < 0) {
            return -1L;
        }

        long targetTimeMs = getProgressElapsedMs(targetIndex);
        long remainingMs = targetTimeMs - getServerSyncedElapsedMs();
        return remainingMs > 0 ? remainingMs : -1L;
    }

    private long getServerSyncedElapsedMs() {
        if (supplyProgressStartMs < 0L || lastPickupProgressMs < 0L) {
            return 0L;
        }

        return Math.max(0L, System.currentTimeMillis() - supplyProgressStartMs);
    }

    private Color getPearlTimerColor(long remainingMs, long totalMs) {
        if (remainingMs <= 0 || totalMs <= 0) return Color.WHITE;

        double remainingRatio = Math.clamp((double) remainingMs / (double) totalMs, 0.0, 1.0);

        if (remainingRatio >= 0.75) return new Color(85, 255, 85, 0xff);
        if (remainingRatio >= 0.50) return new Color(255, 255, 0, 0xff);
        if (remainingRatio >= 0.25) return new Color(255, 165, 0, 0xff);
        return new Color(255, 85, 85, 0xff);
    }

    private int getProgressIndex(int progress) {
        if (progress <= 0) return -1;
        for (int i = 0; i < SUPPLY_TICK_PERCENTAGES.size(); i++) {
            if (SUPPLY_TICK_PERCENTAGES.get(i) >= progress) {
                return i;
            }
        }
        return SUPPLY_TICK_PERCENTAGES.size() - 1;
    }

    private long getProgressElapsedMs(int targetIndex) {
        double totalPickupMs = getSupplyWindowTicks(
                KuudraState.get().tier(),
                getTalismanTier()
        ) * 50.0;
        double progressStep = (targetIndex + 1) / (double) SUPPLY_TICK_PERCENTAGES.size();
        return Math.round(totalPickupMs * progressStep);
    }

    private double getSupplyWindowTicks(KuudraTier tier, PearlTalismanTier talismanTier) {
        return switch (talismanTier) {
            case NONE -> switch (tier) {
                case BASIC -> 60;
                case HOT -> 80;
                case BURNING -> 100;
                case FIERY, INFERNAL, UNKNOWN -> 120;
            };
            case TIER_1 -> switch (tier) {
                case BASIC -> 55;
                case HOT -> 75;
                case BURNING -> 90;
                case FIERY, INFERNAL, UNKNOWN -> 110;
            };
            case TIER_2 -> switch (tier) {
                case BASIC -> 50;
                case HOT -> 65;
                case BURNING -> 80;
                case FIERY, INFERNAL, UNKNOWN -> 100;
            };
            case TIER_3 -> switch (tier) {
                case BASIC -> 45;
                case HOT -> 60;
                case BURNING -> 70;
                case FIERY, INFERNAL, UNKNOWN -> 85;
            };
        };
    }

    private void playAlertOnce(String key) {
        if (mc.player == null || mc.level == null || !alertedWaypoints.add(key)) return;

        mc.level.playSound(
                mc.player,
                mc.player.blockPosition(),
                SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.PLAYERS,
                1.3f,
                1.6f
        );
    }

    private boolean isLocalPlayer(String playerName) {
        if (mc.player == null || playerName == null || playerName.isBlank()) {
            return false;
        }

        return StringUtils.stripFormatting(playerName).equalsIgnoreCase(mc.player.getName().getString());
    }

    private void resetState() {
        lastSupplyProgress = 0;
        lastSupplyProgressIndex = -1;
        supplyProgressStartMs = -1L;
        lastPickupProgressMs = -1L;
        readyStartedMs = -1L;
        alertedWaypoints.clear();
    }

    private boolean isPickingSupply() {
        return lastPickupProgressMs > 0L
                && System.currentTimeMillis() - lastPickupProgressMs <= PICKUP_PROGRESS_TIMEOUT_MS;
    }

    private void clearPickupState() {
        lastSupplyProgress = 0;
        lastSupplyProgressIndex = -1;
        supplyProgressStartMs = -1L;
        lastPickupProgressMs = -1L;
        readyStartedMs = -1L;
        alertedWaypoints.clear();
    }

    /** Re-reads the editable waypoint config (used by the waypoint editor / reload command). */
    public void reloadConfig() {
        List<WaypointArea> areas = BuiltInPearlWaypoints.getAreas();
        areaDetection.setAreas(areas);
        areaDetection.update();
    }

    private TimerType getTimerType() {
        String option = times.getOption();
        if (option == null) return TimerType.TIMER_SECONDS;
        return switch (option) {
            case "Timer Ms" -> TimerType.TIMER_MS;
            case "Timer Ticks" -> TimerType.TIMER_TICKS;
            default -> TimerType.TIMER_SECONDS;
        };
    }

    private TextPosition getTextPosition() {
        String option = textPosition.getOption();
        if (option == null) return TextPosition.BELOW;
        return option.equals("Above") ? TextPosition.ABOVE : TextPosition.BELOW;
    }

    private RenderStyle getRenderStyle() {
        String option = renderStyleSetting.getOption();
        if (option == null) return RenderStyle.SQUARE;
        return switch (option) {
            case "Full Block" -> RenderStyle.FULL_BLOCK;
            case "Filled Outline" -> RenderStyle.FILLED_OUTLINE;
            case "Block Outline" -> RenderStyle.BLOCK_OUTLINE;
            case "Circle" -> RenderStyle.CIRCLE;
            default -> RenderStyle.SQUARE;
        };
    }

    private PearlTalismanTier getTalismanTier() {
        String option = talismanTier.getOption();
        if (option == null) return PearlTalismanTier.TIER_3;
        return switch (option) {
            case "None" -> PearlTalismanTier.NONE;
            case "Tier 1" -> PearlTalismanTier.TIER_1;
            case "Tier 2" -> PearlTalismanTier.TIER_2;
            default -> PearlTalismanTier.TIER_3;
        };
    }

    private static Color withOpacity(Color color, float opacity) {
        int alpha = Math.round(Math.clamp(opacity, 0.0f, 1.0f) * 255.0f);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private enum TimerType {
        TIMER_MS,
        TIMER_SECONDS,
        TIMER_TICKS
    }

    private enum TextPosition {
        ABOVE,
        BELOW
    }

    private enum RenderStyle {
        FULL_BLOCK,
        FILLED_OUTLINE,
        BLOCK_OUTLINE,
        SQUARE,
        CIRCLE
    }

    private record RenderData(
            Vec3 position,
            double flightTicks,
            boolean trajectory,
            Vec3 direction
    ) {
    }

    private record TimerState(
            String text,
            boolean ready,
            Color timerColor
    ) {
        static TimerState hidden() {
            return new TimerState("", false, Color.WHITE);
        }

        static TimerState ready(String text) {
            return new TimerState(text, true, new Color(0, 255, 0, 0xff));
        }

        static TimerState countdown(String text, Color color) {
            return new TimerState(text, false, color);
        }

        static TimerState staticOnly() {
            return new TimerState("", false, Color.WHITE);
        }
    }
}
