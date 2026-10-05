package xyz.whatsyouss.frosty.modules.impl.farming;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.Render3DEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.RenderUtils;
import xyz.whatsyouss.frosty.utility.Utils;

public class FarmingMacro extends Module {

    private static final double ARRIVAL_XZ = 0.45;
    private static final float YAW_SNAP_DEG = 2.0f;
    private static final int TURN_MAX_TICKS = 40;
    private static final int PRE_WARP_TICKS = 20;
    private static final int WARP_COOLDOWN_TICKS = 20;
    private static final int WARP_TIMEOUT_TICKS = 200;
    private static final double WARP_DETECT_DIST = 8.0;
    private static final int PENDING_RESUME_TIMEOUT = 200;
    private final List<double[]> waypoints = new ArrayList<>();
    private SelectSetting face;
    private String[] FACES = new String[]{"North", "South", "East", "West"};
    private String[] CNFACES = new String[]{"北", "南", "东", "西"};
    private ButtonSetting caneMode; // diagonal movement for sugar cane / sunflower / rose etc.
    private SelectSetting diagonalFace;
    private String[] DIAG_FACES = new String[]{"NE", "NW", "SE", "SW"};
    private String[] CN_DIAG_FACES = new String[]{"东北", "西北", "东南", "西南"};
    private SliderSetting pitch, stopTime, triggerAmount;
    private ButtonSetting rotateOnFinish, pestCleaner, rewarpOnly, enableLoop, enableUngrab;
    private ButtonSetting axeMode; // use an axe (melon/pumpkin) instead of a hoe
    private State state = State.IDLE;
    private int targetIndex = 1;
    private int lapCount = 0;
    private int dwellTicks = 0;
    private int turnTicks = 0;
    private int preWarpTicks = 0;
    private int warpCooldown = 0;
    private int warpTimeout = 0;

    private boolean awaitingGrab = false;
    private boolean pestPaused = false;
    private int pestResumePt = 1;
    private boolean pestCleanCompletedLap = false;

    private final List<KeyMapping> activeKeys = new ArrayList<>();
    private int hoeSlot = -1;
    public boolean running = false;

    private Vec3 preWarpPos = null;

    private boolean pendingResume = false;
    private int pendingResumeTick = 0;

    // UngrabMouse deferred enable: only toggle it on after the macro has been running a short while.
    private static final int UNGRAB_CONFIRM_TICKS = 20;
    private boolean ungrabScheduled = false;
    private int ungrabEnableTicks = 0;
    public FarmingMacro() {
        super("FarmingMacro", "农业宏", category.Farming);

        this.registerSetting(caneMode = new ButtonSetting("Cane Mode", "对角线模式", false));
        this.registerSetting(face = new SelectSetting("Face", "朝向", 0, FACES, CNFACES));
        this.registerSetting(diagonalFace = new SelectSetting("Diagonal Face", "对角线朝向", 0, DIAG_FACES, CN_DIAG_FACES));
        this.registerSetting(pitch = new SliderSetting("Pitch", 0, -90, 90, 1, "俯仰角"));
        this.registerSetting(stopTime = new SliderSetting("Stop time", 500, 100, 6000, 50, "刹车时长"));
        this.registerSetting(rotateOnFinish = new ButtonSetting("Rotate on finish", "结束后反向", false));
        this.registerSetting(pestCleaner = new ButtonSetting("Pest cleaner", "害虫清理", true));
        this.registerSetting(triggerAmount = new SliderSetting("Trigger amount", 4, 1, 8, 1, "触发数量"));
        this.registerSetting(rewarpOnly = new ButtonSetting("Rewarp only", "只在本轮结束清理", true));
        this.registerSetting(enableLoop = new ButtonSetting("Enable Loop", false));
        this.registerSetting(enableUngrab = new ButtonSetting("Enable Ungrab Mouse Automatically", true));
        this.registerSetting(axeMode = new ButtonSetting("Pumpkin/Melon (Axe)", "切瓜/南瓜(斧)", false));
    }

    @Override
    public void guiUpdate() {
        this.triggerAmount.setVisibilityCondition(() -> pestCleaner.isToggled());
        this.rewarpOnly.setVisibilityCondition(() -> pestCleaner.isToggled());
        this.face.setVisibilityCondition(() -> !caneMode.isToggled());
        this.diagonalFace.setVisibilityCondition(() -> caneMode.isToggled());
    }

    public List<double[]> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<double[]> loaded) {
        waypoints.clear();
        waypoints.addAll(loaded);
    }

    @Override
    public void onEnable() {
        running = false;
        state = State.IDLE;
        releaseAll();
    }

    @Override
    public void onDisable() {
        if (ModuleManager.pestCleaner.isEnabled()) {
            ModuleManager.pestCleaner.disable();
        }
        pestPaused = false;
        stopMacro();

        if (enableUngrab.isToggled() && ModuleManager.ungrabMouse != null && ModuleManager.ungrabMouse.isEnabled()) {
            ModuleManager.ungrabMouse.disable();
        }
    }

    public void startMacro(int startIndex) {
        if (!Utils.nullCheck()) return;
        if (waypoints.size() < 2) {
            Utils.addModuleMessage(this.getName(), "§cNeed at least 2 waypoints");
            return;
        }

        hoeSlot = findToolSlot();
        if (hoeSlot == -1) {
            Utils.addModuleMessage(this.getName(), "§cNo " + (axeMode.isToggled() ? "axe" : "hoe") + " found in hotbar");
            return;
        }
        mc.player.getInventory().setSelectedSlot(hoeSlot);

        FarmingProtector.stopped = false;
        lapCount = 0;
        targetIndex = Math.min(startIndex + 1, waypoints.size() - 1);
        dwellTicks = 0;
        turnTicks = 0;
        preWarpTicks = 0;
        warpCooldown = 0;
        warpTimeout = 0;
        preWarpPos = null;
        activeKeys.clear();
        awaitingGrab = true;
        pestCleanCompletedLap = false;
        ungrabScheduled = enableUngrab.isToggled() && ModuleManager.ungrabMouse != null;
        ungrabEnableTicks = 0;

        releaseAll();
        prepareMouseForMacroStart();

        if (pestCleaner.isToggled() && rewarpOnly.isToggled() && checkAliveThreshold() && startIndex == 0) {
            pauseForPestClean();
            return;
        }
        // UngrabMouse is now enabled only after the macro is confirmed to be running (timed ticks below),
        // so the player can keep fishing/farming without the cursor being freed too early.
    }

    public void stopMacro() {
        running = false;
        state = State.IDLE;
        activeKeys.clear();
        dwellTicks = 0;
        preWarpTicks = 0;
        warpCooldown = 0;
        preWarpPos = null;
        ungrabScheduled = false;
        ungrabEnableTicks = 0;
        releaseAll();

        if (enableUngrab.isToggled() && ModuleManager.ungrabMouse != null && ModuleManager.ungrabMouse.isEnabled()) {
            ModuleManager.ungrabMouse.disable();
            Utils.addModuleMessage(this.getName(), "§aUngrabMouse disabled");
        }
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) return;

        if (awaitingGrab) {
            // If UngrabMouse is enabled, we should skip the grab check
            if (enableUngrab.isToggled() && ModuleManager.ungrabMouse != null && ModuleManager.ungrabMouse.isEnabled()) {
                awaitingGrab = false;
                running = true;
                beginTurning();
                return;
            }

            if (mc.mouseHandler.isMouseGrabbed()) {
                awaitingGrab = false;
                running = true;
                beginTurning();
                // Delay enabling UngrabMouse until the macro is confirmed running, then free the cursor.
                if (ungrabScheduled) {
                    ungrabEnableTicks = UNGRAB_CONFIRM_TICKS;
                }
                return;
            }
            return;
        }
        if (!running) {
            return;
        }
        // Only auto-(re)grab the cursor when UngrabMouse is off. When UngrabMouse frees the cursor we
        // keep farming via the synthetic keyAttack hold below instead of fighting the freed pointer.
        if (!enableUngrab.isToggled() && !mc.mouseHandler.isMouseGrabbed()) {
            if (mc.gui.screen() == null) {
                prepareMouseForMacroStart();
                setKeyPressed(mc.options.keyAttack, true);
            } else {
                setKeyPressed(mc.options.keyAttack, false);
            }
        }
        if (pestPaused) {
            releaseAll();
            return;
        }
        if (pendingResume) {
            releaseAll();
            pendingResumeTick++;
            if (!mc.player.onGround()) {
                if (mc.player.getAbilities().flying) {
                    mc.player.getAbilities().flying = false;
                    mc.player.onUpdateAbilities();
                }
                mc.options.keyShift.setDown(true);
            } else {
                mc.options.keyShift.setDown(false);
            }
            boolean grounded = mc.player.onGround();
            boolean timedOut = pendingResumeTick >= PENDING_RESUME_TIMEOUT;
            if (grounded || timedOut) {
                pendingResume = false;
                pendingResumeTick = 0;
                beginTurning();
            }
            return;
        }

        // Once the macro has been running for a short while, toggle UngrabMouse on (once per start).
        if (ungrabScheduled) {
            ungrabEnableTicks--;
            if (ungrabEnableTicks <= 0) {
                ungrabScheduled = false;
                if (enableUngrab.isToggled() && ModuleManager.ungrabMouse != null && !ModuleManager.ungrabMouse.isEnabled()) {
                    ModuleManager.ungrabMouse.enable();
                    Utils.addModuleMessage(this.getName(), "§aUngrabMouse enabled");
                }
            }
        }

        if (!mc.options.keyAttack.isDown()) {
            setKeyPressed(mc.options.keyAttack, true);
        }

        int slot = findToolSlot();
        if (slot != -1) {
            hoeSlot = slot;
            mc.player.getInventory().setSelectedSlot(hoeSlot);
        }

        switch (state) {
            case TURNING -> tickTurning();
            case MOVING -> tickMoving();
            case DWELLING -> tickDwelling();
            case PRE_WARP -> tickPreWarp();
            case WARPING -> tickWarping();
            default -> {
            }
        }

        if (pestCleaner.isToggled() && running && !pestPaused && !rewarpOnly.isToggled()) {
            checkPestTrigger();
        }
    }

    private void beginTurning() {
        releaseKeys();
        turnTicks = 0;
        state = State.TURNING;
    }

    private void tickTurning() {
        float tYaw = faceYaw();
        float tPitch = (float) pitch.getInput();
        snapYaw(tYaw, tPitch);
        turnTicks++;

        float delta = Math.abs(Mth.wrapDegrees(tYaw - mc.player.getYRot()));
        if (delta < YAW_SNAP_DEG || turnTicks >= TURN_MAX_TICKS) {
            beginMovingToTarget();
        }
    }

    private void beginMovingToTarget() {
        if (targetIndex >= waypoints.size()) {
            beginPreWarp();
            return;
        }

        double[] from = waypoints.get(targetIndex - 1);
        double[] to = waypoints.get(targetIndex);

        List<KeyMapping> keys = resolveKeys(from, to);
        if (keys.isEmpty()) {
            Utils.addModuleMessage(this.getName(), "§eWaypoints #" + targetIndex
                    + " identical; skipping.");
            targetIndex++;
            beginMovingToTarget();
            return;
        }

        activeKeys.clear();
        activeKeys.addAll(keys);
        state = State.MOVING;
    }

    private void tickMoving() {
        if (targetIndex >= waypoints.size()) {
            beginPreWarp();
            return;
        }

        lockYaw();

        double[] target = waypoints.get(targetIndex);
        Vec3 pos = mc.player.position();

        boolean arrivedX = Math.abs(pos.x - target[0]) < ARRIVAL_XZ;
        boolean arrivedZ = Math.abs(pos.z - target[2]) < ARRIVAL_XZ;

        if (arrivedX && arrivedZ) {
            releaseKeys();
            dwellTicks = 0;
            state = State.DWELLING;
            return;
        }

        double[] from = waypoints.get(targetIndex - 1);
        List<KeyMapping> keys = resolveKeys(from, target);
        if (keys.isEmpty()) {
            releaseKeys();
        } else {
            pressKeys(keys);
        }
    }

    private void tickDwelling() {
        if (targetIndex - 1 >= 0 && targetIndex < waypoints.size()) {
            double[] from = waypoints.get(targetIndex - 1);
            double[] to = waypoints.get(targetIndex);
            pressKeys(resolveKeys(from, to));
        } else if (!activeKeys.isEmpty()) {
            pressKeys(activeKeys);
        }

        dwellTicks++;
        if (dwellTicks >= msToTicks((int) stopTime.getInput())) {
            releaseKeys();
            targetIndex++;
            dwellTicks = 0;
            if (targetIndex >= waypoints.size()) {
                beginPreWarp();
            } else {
                beginMovingToTarget();
            }
        }
    }

    private void beginPreWarp() {
        releaseAll();
        preWarpTicks = 0;

        if (pestCleaner.isToggled() && rewarpOnly.isToggled() && checkAliveThreshold()) {
            state = State.IDLE;
            pauseForPestClean();
            return;
        }

        state = State.PRE_WARP;
    }

    private void checkPestTrigger() {
        if (rewarpOnly.isToggled()) return;
        if (mc.getConnection() == null) return;
        for (var entry : mc.getConnection().getOnlinePlayers()) {
            if (entry.getTabListDisplayName() == null) continue;
            String line = entry.getTabListDisplayName().getString().trim();
            if (!line.toLowerCase().contains("alive:")) continue;
            String after = line.substring(line.toLowerCase().indexOf("alive:") + 6).trim();
            String num = after.split("[^0-9]")[0];
            try {
                int alive = Integer.parseInt(num);
                if (alive >= (int) triggerAmount.getInput()) pauseForPestClean();
            } catch (NumberFormatException ignored) {
            }
            break;
        }
    }

    private boolean checkAliveThreshold() {
        if (mc.getConnection() == null) return false;
        for (var entry : mc.getConnection().getOnlinePlayers()) {
            if (entry.getTabListDisplayName() == null) continue;
            String line = entry.getTabListDisplayName().getString().trim();
            if (!line.toLowerCase().contains("alive:")) continue;
            String after = line.substring(line.toLowerCase().indexOf("alive:") + 6).trim();
            String num = after.split("[^0-9]")[0];
            try {
                return Integer.parseInt(num) >= (int) triggerAmount.getInput();
            } catch (NumberFormatException ignored) {
            }
            break;
        }
        return false;
    }

    public void pauseForPestClean() {
        if (pestPaused) return;
        pestPaused = true;
        pestResumePt = targetIndex;
        pestCleanCompletedLap = rewarpOnly.isToggled()
                && pestResumePt >= waypoints.size();
        releaseAll();

        boolean useRewarp = rewarpOnly.isToggled();

        Vec3 resumeVec = null;
        if (!useRewarp && pestResumePt < waypoints.size()) {
            double[] wp = waypoints.get(pestResumePt);
            resumeVec = new Vec3(wp[0], wp[1], wp[2]);
        }

        PestCleaner pc = ModuleManager.pestCleaner;
        if (pc != null) pc.requestPestClean(resumeVec, useRewarp);
    }

    public void resumeFromPestClean() {
        if (!pestPaused) return;
        pestPaused = false;

        if (rewarpOnly.isToggled()) {
            if (pestCleanCompletedLap) {
                lapCount++;
                if (!enableLoop.isToggled()) {
                    stopMacro();
                    Utils.addModuleMessage(this.getName(), "§aFarming completed after pest clean (loop disabled)");
                    return;
                }
            }
            targetIndex = 1;
        } else {
            targetIndex = Math.max(1, Math.min(pestResumePt, waypoints.size() - 1));
        }
        pestCleanCompletedLap = false;

        hoeSlot = findToolSlot();
        if (hoeSlot != -1) mc.player.getInventory().setSelectedSlot(hoeSlot);

        pendingResume = true;
        pendingResumeTick = 0;
    }

    private void tickPreWarp() {
        preWarpTicks++;
        if (preWarpTicks >= PRE_WARP_TICKS) {
            preWarpPos = mc.player.position();
            warpCooldown = WARP_COOLDOWN_TICKS;
            warpTimeout = 0;
            mc.player.connection.sendCommand("warp garden");
            state = State.WARPING;
        }
    }

    private void tickWarping() {
        releaseKeys();
        warpCooldown = Math.max(0, warpCooldown - 1);
        warpTimeout++;

        if (warpTimeout >= WARP_TIMEOUT_TICKS) {
            Utils.addModuleMessage(this.getName(), "§eWarp timed out, resuming...");
            resumeAfterWarp();
            return;
        }

        if (warpCooldown > 0) return;

        if (preWarpPos != null) {
            Vec3 now = mc.player.position();
            double dist = Math.sqrt(
                    Math.pow(now.x - preWarpPos.x, 2) +
                            Math.pow(now.z - preWarpPos.z, 2));
            if (dist >= WARP_DETECT_DIST) {
                resumeAfterWarp();
            }
        }
    }

    private void resumeAfterWarp() {
        if (!enableLoop.isToggled()) {
            stopMacro();
            Utils.addModuleMessage(this.getName(), "§aFarming completed");
            return;
        }
        lapCount++;
        targetIndex = 1;
        dwellTicks = 0;
        preWarpTicks = 0;
        warpCooldown = 0;
        warpTimeout = 0;
        preWarpPos = null;
        activeKeys.clear();
        beginTurning();
    }

    private void snapYaw(float targetYaw, float targetPitch) {
        if (mc.player == null) return;
        float cy = mc.player.getYRot();
        mc.player.setYRot(cy + Mth.wrapDegrees(targetYaw - cy) * 0.35f);
        float cp = mc.player.getXRot();
        mc.player.setXRot(cp + (targetPitch - cp) * 0.35f);
    }

    private void lockYaw() {
        if (mc.player == null) return;
        float targetYaw = faceYaw();
        float targetPitch = (float) pitch.getInput();
        float cy = mc.player.getYRot();
        float cp = mc.player.getXRot();
        mc.player.setYRot(cy + Mth.wrapDegrees(targetYaw - cy) * 0.15f);
        mc.player.setXRot(cp + (targetPitch - cp) * 0.15f);
    }

    // ---- movement resolution: two independent modes ----
    // normal (axial) and cane(diagonal) each use their own key logic and never
    // bleed into the other. faceYaw() already selects the world direction; here
    // we just route to the correct mode's key calculation.

    private List<KeyMapping> resolveKeys(double[] from, double[] to) {
        float yaw = faceYaw();
        if (caneMode != null && caneMode.isToggled()) {
            return computeKeys(from, to, yaw); // cane/diagonal logic below
        }
        return normalKeys(from, to, yaw);
    }

    /**
     * Original axial movement: the travel is treated purely against the current
     * cardinal facing (N/E/S/W). The dominant axis of the leg decides the needed
     * direction; the correct single key is derived from where that direction sits
     * relative to where the player looks.
     */
    private List<KeyMapping> normalKeys(double[] from, double[] to, float yaw) {
        double dx = to[0] - from[0];
        double dz = to[2] - from[2];

        boolean horizontal = Math.abs(dx) >= Math.abs(dz);
        int need;
        if (horizontal) {
            if (Math.abs(dx) < 0.1) return List.of();
            need = dx > 0 ? CARD_EAST : CARD_WEST;
        } else {
            if (Math.abs(dz) < 0.1) return List.of();
            need = dz > 0 ? CARD_SOUTH : CARD_NORTH;
        }

        int view = axialView(yaw);
        KeyMapping k = cardinalKey(view, need);
        return k == null ? List.of() : List.of(k);
    }

    // yaw -> the cardinal index that matches the original Face selector:
    // Face idx 0=North(yaw180), 1=South(yaw0), 2=East(yaw-90), 3=West(yaw90).
    private int axialView(float yaw) {
        float y = Mth.wrapDegrees(yaw);
        if (Math.abs(y) < 45f) return CARD_SOUTH;      // yaw ~0
        if (y >= 45f && y < 135f) return CARD_WEST;    // yaw ~90
        if (y >= 135f || y < -135f) return CARD_NORTH; // yaw ~180
        return CARD_EAST;                              // yaw ~-90
    }

    private KeyMapping cardinalKey(int view, int need) {
        if (view == need) return mc.options.keyUp;     // forward
        if (need == opposite(view)) return mc.options.keyDown;   // back
        if (need == clockwiseFrom(view)) return mc.options.keyRight; // right
        if (need == counterClockwiseFrom(view)) return mc.options.keyLeft; // left
        return null;
    }

    private float faceYaw() {
        float yaw;
        if (caneMode != null && caneMode.isToggled()) {
            yaw = switch ((int) diagonalFace.getValue()) {
                case 0 -> -135f; // NE
                case 1 -> 135f;  // NW
                case 2 -> -45f;  // SE
                case 3 -> 45f;   // SW
                default -> -135f;
            };
        } else {
            yaw = switch ((int) face.getValue()) {
                case 0 -> 180f;
                case 1 -> 0f;
                case 2 -> -90f;
                case 3 -> 90f;
                default -> 0f;
            };
        }
        if (rotateOnFinish.isToggled() && (lapCount % 2 == 1)) {
            yaw = Mth.wrapDegrees(yaw + 180f);
        }
        return yaw;
    }

    private List<KeyMapping> computeKeys(double[] from, double[] to, float yaw) {
        double dx = to[0] - from[0];
        double dz = to[2] - from[2];
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-4) {
            return List.of();
        }
        double ndx = dx / len;
        double ndz = dz / len;

        // Diagonal (canes) mode: straight N-S / E-W legs get a single strafe key
        // matched to the wall layout. Axial mode must keep the original cardinal
        // N/E/S/W movement, so this special-casing is only turned on for caneMode.
        boolean nsRow = Math.abs(dx) < 0.6;
        boolean ewRow = Math.abs(dz) < 0.6;
        boolean cane = caneMode != null && caneMode.isToggled();
        if (cane && (nsRow || ewRow)) {
            if (nsRow) {
                // N-S rows: going South is always "S". Going North is strafed —
                // toward the player's right for west-facing looks (NW/SW -> D),
                // toward the left for east-facing looks (NE/SE -> A).
                if (dz > 0) return List.of(mc.options.keyDown);
                return List.of(Mth.wrapDegrees(yaw) >= 0
                        ? mc.options.keyRight
                        : mc.options.keyLeft);
            }
            int row = dx > 0 ? CARD_EAST : CARD_WEST;
            KeyMapping single = singleAxisKey(row, yaw);
            return single == null ? List.of() : List.of(single);
        }

        // True diagonal row: geometric two-key combination.
        double yawRad = Math.toRadians(Mth.wrapDegrees(yaw));
        // Minecraft: forward (-sin, 0, cos), right (cos, 0, sin)
        double fwX = -Math.sin(yawRad);
        double fwZ = Math.cos(yawRad);
        double rtX = Math.cos(yawRad);
        double rtZ = Math.sin(yawRad);

        double fwdDot = ndx * fwX + ndz * fwZ;
        double rgtDot = ndx * rtX + ndz * rtZ;

        List<KeyMapping> keys = new ArrayList<>();
        double threshold = 0.1;
        if (fwdDot > threshold) keys.add(mc.options.keyUp);
        else if (fwdDot < -threshold) keys.add(mc.options.keyDown);
        if (rgtDot > threshold) keys.add(mc.options.keyRight);
        else if (rgtDot < -threshold) keys.add(mc.options.keyLeft);
        return keys;
    }

    // Cardinal axis codes used by singleAxisKey / relativeAxisKey.
    private static final int CARD_NORTH = 0;
    private static final int CARD_EAST = 1;
    private static final int CARD_SOUTH = 2;
    private static final int CARD_WEST = 3;

    private static final float[] AXIS_YAW = {180f, -90f, 0f, 90f}; // N, E, S, W

    /**
     * Reduce a straight N-S / E-W segment to a single cardinal key.
     * The chosen diagonal faces sit exactly on 45° bisectors, i.e. always an
     * even split between two neighbouring cardinal axes. We pick the axis whose
     * pressed "right" strafe key drives along {@code row}, so the macro moves
     * straight down the row with one key (D in the described cane setup) instead
     * of producing a W+A combo.
     */
    private KeyMapping singleAxisKey(int row, float yaw) {
        float best = Float.MAX_VALUE;
        int bestA = -1;
        int bestB = -1;
        for (int i = 0; i < AXIS_YAW.length; i++) {
            float d = Math.abs(Mth.wrapDegrees(yaw - AXIS_YAW[i]));
            if (d < best - 1e-6f) {
                best = d;
                bestA = i;
                bestB = -1;
            } else if (Math.abs(d - best) < 1e-6f && i != bestA) {
                bestB = i;
            }
        }
        int view = bestA;
        if (bestB != -1) {
            // On a 45° tie prefer the neighbour that puts the row on our right
            // (pressing D), then the neighbour that walks the row straight-ahead.
            view = clockwiseFrom(bestA) == row ? bestA
                    : clockwiseFrom(bestB) == row ? bestB
                    : bestA;
        }
        return relativeAxisKey(view, row);
    }

    private KeyMapping relativeAxisKey(int view, int row) {
        if (view == row) return mc.options.keyUp;             // forward
        if (row == opposite(view)) return mc.options.keyDown; // backward
        if (row == clockwiseFrom(view)) return mc.options.keyRight; // D
        if (row == counterClockwiseFrom(view)) return mc.options.keyLeft;  // A
        return null;
    }

    private int opposite(int axis) {
        return (axis + 2) & 3;
    }

    private int clockwiseFrom(int axis) {
        return (axis + 1) & 3; // N->E, E->S, S->W, W->N
    }

    private int counterClockwiseFrom(int axis) {
        return (axis + 3) & 3; // N->W, E->N, S->E, W->S
    }

    private void pressKeys(List<KeyMapping> keys) {
        releaseKeys();
        for (KeyMapping key : keys) {
            setKeyPressed(key, true);
        }
    }

    private void releaseKeys() {
        setKeyPressed(mc.options.keyUp, false);
        setKeyPressed(mc.options.keyDown, false);
        setKeyPressed(mc.options.keyLeft, false);
        setKeyPressed(mc.options.keyRight, false);
    }

    private void releaseAll() {
        releaseKeys();
        setKeyPressed(mc.options.keyAttack, false);
        mc.options.keyShift.setDown(false);
    }

    private void setKeyPressed(KeyMapping key, boolean pressed) {
        key.setDown(pressed);
    }

    private void prepareMouseForMacroStart() {
        if (mc.gui.screen() != null) return;

        boolean restoreUngrab = ModuleManager.ungrabMouse != null && ModuleManager.ungrabMouse.isEnabled();
        if (restoreUngrab) {
            ModuleManager.ungrabMouse.disable();
        }

        if (!mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.grabMouse();
        }

        if (restoreUngrab) {
            ModuleManager.ungrabMouse.enable();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (waypoints.isEmpty()) return;

        for (int i = 0; i < waypoints.size(); i++) {
            double[] wp = waypoints.get(i);
            BlockPos bp = new BlockPos(
                    (int) Math.floor(wp[0]),
                    (int) Math.floor(wp[1]),
                    (int) Math.floor(wp[2]));

            Color color;
            if (!running) {
                color = Color.CYAN;
            } else if (i < targetIndex) {
                color = Color.GREEN;
            } else if (i == targetIndex) {
                color = Color.PINK;
            } else {
                color = Color.RED;
            }

            RenderUtils.drawBox(event.getMatrix(), bp, color, 2f, false);

            String label = "#" + (i + 1);
            if (running && i == targetIndex) {
                label = "➔ #" + (i + 1);
            }

            double textX = wp[0];
            double textY = wp[1] + 3;
            double textZ = wp[2];

            RenderUtils.drawText3D(event.getMatrix(), label, textX, textY, textZ, color, false);
        }
    }

    private int findToolSlot() {
        Inventory inv = mc.player.getInventory();
        boolean axe = axeMode != null && axeMode.isToggled();
        for (int i = 0; i < 9; i++) {
            if (axe) {
                if (inv.getItem(i).getItem() instanceof AxeItem) return i;
            } else {
                if (inv.getItem(i).getItem() instanceof HoeItem) return i;
            }
        }
        return -1;
    }

    private int msToTicks(int ms) {
        return Math.max(1, ms / 50);
    }

    private enum State {
        IDLE, TURNING, MOVING, DWELLING, PRE_WARP, WARPING
    }
}
