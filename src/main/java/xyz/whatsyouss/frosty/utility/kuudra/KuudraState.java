package xyz.whatsyouss.frosty.utility.kuudra;

import net.minecraft.client.Minecraft;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.kuudra.KuudraPhaseChangeEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.KuudraRunEndEvent;
import xyz.whatsyouss.frosty.utility.LocationUtils;
import xyz.whatsyouss.frosty.utility.Utils;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ported from IQAddons (net.iqaddons.mod.manager.KuudraStateManager /
 * net.iqaddons.mod.model.kuudra.KuudraContext).
 *
 * Keeps the run-detection behaviour the modules depend on:
 *  - phase is driven by the same chat triggers as IQAddons,
 *  - a run only counts while the player is on SkyBlock inside the Kuudra area,
 *  - BOSS is detected from the player dropping below Y 10 during SKIP/DPS,
 *  - leaving the Kuudra area (or SkyBlock) resets the run,
 *  - phase transitions are broadcast as {@link KuudraPhaseChangeEvent},
 *  - a finished run is broadcast as {@link KuudraRunEndEvent} with the reason, so the
 *    profit tracker can tell a completed run from a failed or abandoned one.
 */
public final class KuudraState {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final KuudraState INSTANCE = new KuudraState();

    /** Matches the "(T5)" tier suffix of the scoreboard area line. */
    private static final Pattern KUUDRA_TIER_PATTERN = Pattern.compile("\\(T([1-5])\\)");

    private static final String DEFEAT_MESSAGE = "DEFEAT";

    private volatile KuudraPhase phase = KuudraPhase.NONE;
    private volatile KuudraTier tier = KuudraTier.UNKNOWN;
    private volatile boolean inKuudraArea = false;
    private volatile boolean onSkyBlock = false;
    private volatile KuudraBossInfo bossInfo = KuudraBossInfo.empty();

    private final Map<KuudraPhase, Duration> phaseDurations = new EnumMap<>(KuudraPhase.class);
    private volatile long phaseStartedAtMs = 0L;

    private KuudraState() {
    }

    public static KuudraState get() {
        return INSTANCE;
    }

    public KuudraPhase phase() {
        return phase;
    }

    public KuudraTier tier() {
        return tier;
    }

    public KuudraBossInfo bossInfo() {
        return bossInfo;
    }

    public boolean isInKuudra() {
        return phase != KuudraPhase.NONE;
    }

    public boolean isInRun() {
        return phase.isInRun() && inKuudraArea && onSkyBlock;
    }

    public Optional<Duration> currentPhaseDuration() {
        if (phase == KuudraPhase.NONE || phaseStartedAtMs <= 0L) return Optional.empty();
        return Optional.of(Duration.ofMillis(Math.max(0L, System.currentTimeMillis() - phaseStartedAtMs)));
    }

    public void onChat(String strippedMessage) {
        if (isInKuudra() && DEFEAT_MESSAGE.equals(strippedMessage.trim())
                && (phase == KuudraPhase.BOSS || phase == KuudraPhase.COMPLETED)) {
            endRun(KuudraRunEndEvent.EndReason.DEFEATED);
            return;
        }

        if (!onSkyBlock && phase == KuudraPhase.NONE) return;

        KuudraPhase detected = KuudraPhase.fromMessage(strippedMessage);
        if (detected == null) return;

        if (detected == KuudraPhase.NONE) {
            if (isInKuudra()) {
                endRun(isInstanceTransferMessage(strippedMessage)
                        ? KuudraRunEndEvent.EndReason.DISCONNECTED
                        : KuudraRunEndEvent.EndReason.OTHER);
            }
            return;
        }

        setPhase(detected);
    }

    public boolean setPhase(KuudraPhase newPhase) {
        if (phase == newPhase) return false;
        if (phase == KuudraPhase.NONE && newPhase != KuudraPhase.SUPPLIES) return false;

        if (newPhase == KuudraPhase.NONE) {
            endRun(KuudraRunEndEvent.EndReason.OTHER);
            return true;
        }

        KuudraPhase previous = phase;

        if ((previous == KuudraPhase.NONE || previous == KuudraPhase.COMPLETED) && newPhase == KuudraPhase.SUPPLIES) {
            startRun();
        } else if (newPhase.getOrder() < previous.getOrder()) {
            return false;
        } else {
            recordPhaseDuration(previous);
            phase = newPhase;
            phaseStartedAtMs = System.currentTimeMillis();
        }

        Frosty.EVENT_BUS.post(new KuudraPhaseChangeEvent(previous, phase));

        if (phase == KuudraPhase.COMPLETED) {
            endRun(KuudraRunEndEvent.EndReason.COMPLETED);
        }

        return true;
    }

    /**
     * Gets the current area from the tab list and resets the run when the player
     * leaves Kuudra / SkyBlock. Transient blank/"Unknown" area reads are ignored so
     * a scoreboard refresh mid-run does not drop the run (matches IQAddons).
     */
    public void refreshEnvironment() {
        String area = LocationUtils.getCurrentArea();
        if (area == null || area.isBlank() || "Unknown".equalsIgnoreCase(area)) {
            return;
        }

        boolean inKuudra = area.toLowerCase().contains("kuudra");

        this.onSkyBlock = true;
        this.inKuudraArea = inKuudra;

        if (inKuudra && tier == KuudraTier.UNKNOWN) {
            detectTier();
        }

        if (phase != KuudraPhase.NONE && !inKuudra) {
            endRun(KuudraRunEndEvent.EndReason.DISCONNECTED);
        }
    }

    /**
     * Reads the Kuudra tier from the scoreboard sidebar area line (e.g. "Kuudra's Hollow (T5)"),
     * falling back to the tab-list area, and stores it for the pearl timers.
     */
    public void detectTier() {
        KuudraTier detected = findTier(mc.level != null ? Utils.getScoreboardSidebarLines() : null);
        if (detected == null) {
            detected = findTier(java.util.List.of(LocationUtils.getCurrentArea()));
        }
        if (detected != null) {
            tier = detected;
        }
    }

    /** BOSS is entered once the player drops into the pit during SKIP/DPS. */
    public void tickBossCheck() {
        if (mc.player == null) return;

        if (phase == KuudraPhase.NONE) {
            if (bossInfo.isAlive()) {
                bossInfo = KuudraBossInfo.empty();
            }
            return;
        }

        bossInfo = KuudraLocationUtil.findKuudra()
                .map(KuudraBossInfo::tracked)
                .orElseGet(KuudraBossInfo::empty);

        if (!isInRun()) return;
        if (phase == KuudraPhase.SKIP || phase == KuudraPhase.DPS) {
            if (mc.player.getY() < 10) {
                setPhase(KuudraPhase.BOSS);
            }
        }
    }

    public void reset() {
        if (phase == KuudraPhase.NONE) return;
        endRun(KuudraRunEndEvent.EndReason.OTHER);
    }

    private void startRun() {
        phase = KuudraPhase.SUPPLIES;
        inKuudraArea = true;
        onSkyBlock = true;
        detectTier();

        phaseDurations.clear();
        phaseStartedAtMs = System.currentTimeMillis();
        bossInfo = KuudraBossInfo.empty();

        SupplyState supplyState = SupplyState.get();
        supplyState.reset();
        supplyState.startSuppliesPhase();
    }

    private void recordPhaseDuration(KuudraPhase finishedPhase) {
        if (!finishedPhase.isInRun() || phaseStartedAtMs <= 0L) return;

        phaseDurations.put(finishedPhase, Duration.ofMillis(Math.max(0L, System.currentTimeMillis() - phaseStartedAtMs)));
    }

    private void endRun(KuudraRunEndEvent.EndReason reason) {
        KuudraPhase previous = phase;
        KuudraTier runTier = tier;

        recordPhaseDuration(previous);

        Duration totalDuration = phaseDurations.values().stream().reduce(Duration.ZERO, Duration::plus);

        phase = KuudraPhase.NONE;
        tier = KuudraTier.UNKNOWN;
        inKuudraArea = false;
        onSkyBlock = false;
        bossInfo = KuudraBossInfo.empty();
        phaseStartedAtMs = 0L;

        KuudraLocationUtil.invalidateCache();
        SupplyState.get().reset();

        Frosty.EVENT_BUS.post(new KuudraPhaseChangeEvent(previous, KuudraPhase.NONE));
        Frosty.EVENT_BUS.post(new KuudraRunEndEvent(reason, runTier, totalDuration, Map.copyOf(phaseDurations)));

        phaseDurations.clear();
    }

    private boolean isInstanceTransferMessage(String message) {
        return message.contains("Sending to server") || message.contains("Starting in 4 seconds...");
    }

    private KuudraTier findTier(java.util.List<String> lines) {
        if (lines == null) return null;

        for (String line : lines) {
            if (line == null) continue;
            Matcher matcher = KUUDRA_TIER_PATTERN.matcher(line);
            if (matcher.find()) {
                return KuudraTier.fromLevel(Integer.parseInt(matcher.group(1))).orElse(null);
            }
        }
        return null;
    }
}
