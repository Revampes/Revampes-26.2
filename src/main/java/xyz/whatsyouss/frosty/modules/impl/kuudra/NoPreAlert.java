package xyz.whatsyouss.frosty.modules.impl.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.KuudraPhaseChangeEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.utility.EntityDetectorUtil;
import xyz.whatsyouss.frosty.utility.MessageUtil;
import xyz.whatsyouss.frosty.utility.ServerUtils;
import xyz.whatsyouss.frosty.utility.StringUtils;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraPhase;
import xyz.whatsyouss.frosty.utility.kuudra.KuudraState;
import xyz.whatsyouss.frosty.utility.kuudra.NoPreMessageParser;
import xyz.whatsyouss.frosty.utility.kuudra.PreSpot;
import xyz.whatsyouss.frosty.utility.kuudra.SupplyPosition;
import xyz.whatsyouss.frosty.utility.kuudra.SupplyState;

import java.util.List;

/**
 * Ported from IQAddons (features/kuudra/alerts/NoPreAlertFeature).
 * Detects the player's pre spot and announces to party chat when the matching pre /
 * secondary supply is missing at the start of the supplies phase.
 */
public class NoPreAlert extends Module {

    private static final int SUPPLY_SCAN_INTERVAL_TICKS = 2;
    private static final long EARLY_CHECK_DELAY_MS = 10_000L;
    private static final long MAX_LAG_BUDGET_MS = 4_000L;
    private static final long PING_BASELINE_MS = 120L;
    private static final long FALLBACK_CONFIRM_DELAY_MS = 350L;
    private static final int MIN_EMPTY_SCANS_FOR_FALLBACK = 3;
    private static final long SUPPLY_SPAWN_DESYNC_GRACE_MS = 250L;
    private static final int PARTIAL_SPAWN_MAX_SUPPLIES = 2;

    private static final String ELLE_HEAD_OVER_MESSAGE = "[NPC] Elle: Head over to the main platform";
    private static final String ELLE_NOT_AGAIN_MESSAGE = "[NPC] Elle: Not again!";

    private final SupplyState supplyState = SupplyState.get();
    private final ButtonSetting showCratePriority = new ButtonSetting("Show Crate Priority", true);
    private final ButtonSetting advancedPriority = new ButtonSetting("Advanced Crate Priority", false);

    private boolean supplyCheckCompleted = false;
    private boolean carrierCheckAttempted = false;
    private boolean timedCheckAttempted = false;
    private boolean fallbackConfirmationPending = false;
    private long fallbackConfirmationStartMs = 0L;
    private long firstSupplySeenAtMs = 0L;
    private boolean graceRecheckAttempted = false;
    private int consecutiveEmptyScans = 0;
    private boolean preSpotDetectionAnnounced = false;

    private int tickCounter = 0;

    public NoPreAlert() {
        super("No Pre Alert", category.Kuudra);
        registerSetting(showCratePriority);
        registerSetting(advancedPriority);
    }

    @Override
    public void onEnable() {
        resetForSuppliesPhase();
    }

    @EventHandler
    public void onPhaseChange(KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            resetForSuppliesPhase();
        }
    }

    private boolean inSuppliesPhase() {
        return KuudraState.get().phase() == KuudraPhase.SUPPLIES;
    }

    @EventHandler
    public void onTick(PreUpdateEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (!inSuppliesPhase()) return;

        tickCounter++;
        if (tickCounter % SUPPLY_SCAN_INTERVAL_TICKS != 0) return;
        if (supplyCheckCompleted) return;

        List<SupplyPosition> supplies = updateSupplyPositions();
        if (!supplies.isEmpty()) {
            long now = System.currentTimeMillis();
            if (firstSupplySeenAtMs == 0L) {
                firstSupplySeenAtMs = now;
            }

            consecutiveEmptyScans = 0;
            fallbackConfirmationPending = false;
            if (!carrierCheckAttempted) {
                carrierCheckAttempted = true;
                queueSupplyCheck(false);
            } else if (!graceRecheckAttempted
                    && now - firstSupplySeenAtMs >= SUPPLY_SPAWN_DESYNC_GRACE_MS) {
                graceRecheckAttempted = true;
                queueSupplyCheck(false);
            }
            return;
        }

        firstSupplySeenAtMs = 0L;
        graceRecheckAttempted = false;
        consecutiveEmptyScans++;

        if (!timedCheckAttempted) {
            long elapsedMs = supplyState.getElapsedTimeMillis();
            long adaptiveFallbackDelayMs = getAdaptiveFallbackDelayMs();

            if (elapsedMs >= adaptiveFallbackDelayMs) {
                if (!fallbackConfirmationPending) {
                    fallbackConfirmationPending = true;
                    fallbackConfirmationStartMs = System.currentTimeMillis();
                    return;
                }

                long confirmationElapsed = System.currentTimeMillis() - fallbackConfirmationStartMs;
                if (confirmationElapsed >= FALLBACK_CONFIRM_DELAY_MS
                        && consecutiveEmptyScans >= MIN_EMPTY_SCANS_FOR_FALLBACK) {
                    timedCheckAttempted = true;
                    fallbackConfirmationPending = false;
                    queueSupplyCheck(false);
                }
            }
        }
    }

    @EventHandler
    public void onChat(ReceiveMessageEvent event) {
        if (!inSuppliesPhase()) return;

        String message = StringUtils.stripFormatting(event.getMessage().getString());
        if (message.contains(ELLE_HEAD_OVER_MESSAGE)) {
            detectPreSpotFromPlayerPosition();
            return;
        }

        if (message.contains(ELLE_NOT_AGAIN_MESSAGE)) {
            queueSupplyCheck(true);
            return;
        }

        NoPreMessageParser.ParsedNoPreCall parsed = NoPreMessageParser.parse(message);
        if (parsed != null) {
            updateMissingPre(parsed.missingPreValue());
            announceCratePriority(parsed.missingPreValue());
        }
    }

    private void resetForSuppliesPhase() {
        supplyState.reset();
        supplyState.startSuppliesPhase();
        supplyCheckCompleted = false;
        carrierCheckAttempted = false;
        timedCheckAttempted = false;
        fallbackConfirmationPending = false;
        fallbackConfirmationStartMs = 0L;
        firstSupplySeenAtMs = 0L;
        graceRecheckAttempted = false;
        consecutiveEmptyScans = 0;
        preSpotDetectionAnnounced = false;
    }

    private void queueSupplyCheck(boolean notifyDetectionFailure) {
        if (supplyCheckCompleted || !inSuppliesPhase()) return;

        mc.execute(() -> {
            if (supplyCheckCompleted || !inSuppliesPhase()) return;

            boolean checked = performSupplyCheck(notifyDetectionFailure);
            if (checked) {
                supplyState.markNoPreCheckCompleted();
                supplyCheckCompleted = true;
            }
        });
    }

    private void updateMissingPre(int missingPreValue) {
        int currentMissingPre = supplyState.getMissingPre();
        if (currentMissingPre != missingPreValue) {
            supplyState.setMissingPre(missingPreValue);
        }
    }

    private void announceCratePriority(int missingPreValue) {
        if (!showCratePriority.isToggled() || mc.player == null) return;
        PreSpot preSpot = supplyState.getDetectedPreSpot();
        if (preSpot == null) return;

        String priority = getCratePriority(missingPreValue, preSpot);
        if (priority.isEmpty()) return;

        mc.gui.hud.setTitle(Component.literal("§eCrate Priority"));
        mc.gui.hud.setSubtitle(Component.literal("§f" + priority));
        mc.gui.hud.setTimes(5, 45, 10);
    }

    private void announceDetectedPreSpot(PreSpot preSpot) {
        if (preSpotDetectionAnnounced) return;

        preSpotDetectionAnnounced = true;
        MessageUtil.INFO.sendMessage("Pre Spot detected: §e" + preSpot.getDisplayName() + "§7!");
    }

    private String getCratePriority(int missing, PreSpot preSpot) {
        return switch (missing) {
            case 7 -> switch (preSpot) {
                case TRIANGLE, X -> "Go X Cannon";
                case EQUALS, SLASH -> "Go Square, place on Shop";
            };
            case 6 -> switch (preSpot) {
                case TRIANGLE -> advancedPriority.isToggled() ? "Pull Square and X Cannon. Next: collect Shop" : "Pull Square. Next: collect Shop";
                case X -> "Go X Cannon";
                case EQUALS -> advancedPriority.isToggled() ? "Go Shop" : "Go X Cannon";
                case SLASH -> "Go Square, place on Triangle";
            };
            case 5 -> switch (preSpot) {
                case TRIANGLE -> advancedPriority.isToggled() ? "Go Shop" : "Go X Cannon";
                case X -> "Go X Cannon";
                case EQUALS -> advancedPriority.isToggled() ? "Pull Square and X Cannon. Next: collect Shop" : "Pull Square. Next: collect Shop";
                case SLASH -> "Go Square, place on Equals";
            };
            case 4 -> switch (preSpot) {
                case TRIANGLE -> "Go Square, place on Slash";
                case X -> "Go X Cannon";
                case EQUALS -> advancedPriority.isToggled() ? "Go Shop" : "Go X Cannon";
                case SLASH -> advancedPriority.isToggled() ? "Pull Square and X Cannon. Next: collect Shop" : "Pull Square. Next: collect Shop";
            };
            case 3 -> switch (preSpot) {
                case TRIANGLE, EQUALS -> "Go Shop";
                case X, SLASH -> "Go X Cannon";
            };
            case 2 -> switch (preSpot) {
                case TRIANGLE, EQUALS -> "Go Shop";
                case X, SLASH -> "Go Square, place on X Cannon";
            };
            case 1 -> switch (preSpot) {
                case TRIANGLE -> "Go X Cannon";
                case X -> advancedPriority.isToggled() ? "Pull Square and X Cannon. Next: collect Shop" : "Pull Square. Next: collect Shop";
                case EQUALS -> advancedPriority.isToggled() ? "Go Shop" : "Go X Cannon";
                case SLASH -> "Go Square, place on X";
            };
            default -> "";
        };
    }

    private void detectPreSpotFromPlayerPosition() {
        if (mc.player == null) return;

        if (supplyState.tryDetectPreSpot(mc.player.position())) {
            PreSpot preSpot = supplyState.getDetectedPreSpot();
            if (preSpot != null) {
                announceDetectedPreSpot(preSpot);
            }
        }
    }

    private List<SupplyPosition> updateSupplyPositions() {
        List<SupplyPosition> supplies = EntityDetectorUtil.getSupplyCarriers().stream()
                .map(giant -> SupplyPosition.fromGiant(
                        giant.getX(),
                        giant.getZ(),
                        giant.getYRot(),
                        giant.getId()
                ))
                .toList();

        supplyState.updateSupplyPositions(supplies);
        return supplies;
    }

    private boolean performSupplyCheck(boolean notifyDetectionFailure) {
        if (mc.player == null) return false;

        PreSpot preSpot = supplyState.getDetectedPreSpot();
        if (preSpot == null) {
            Vec3 playerPos = mc.player.position();
            boolean detected = supplyState.tryDetectPreSpot(playerPos);
            if (!detected) {
                if (notifyDetectionFailure) {
                    MessageUtil.ERROR.sendMessage("Could not determine your pre spot (too far away?)");
                }
                return false;
            }

            preSpot = supplyState.getDetectedPreSpot();
            if (preSpot == null) {
                return false;
            }

            announceDetectedPreSpot(preSpot);
        }

        List<SupplyPosition> supplies = updateSupplyPositions();

        boolean hasPre = supplyState.hasPreSupply();
        if (!hasPre) {
            if (shouldWaitForSpawnDesync(supplies)) {
                return false;
            }

            supplyState.setMissingPre(preSpot.getMissingPreValue());
            MessageUtil.PARTY.sendMessage("No " + preSpot.getDisplayName() + "!");
            announceCratePriority(preSpot.getMissingPreValue());
        }

        if (preSpot.hasSecondaryLocation()) {
            Boolean hasSecondary = supplyState.hasSecondarySupply();
            if (hasSecondary != null && !hasSecondary) {
                if (shouldWaitForSpawnDesync(supplies)) {
                    return false;
                }

                MessageUtil.PARTY.sendMessage("No " + preSpot.getSecondaryName() + "!");
            }
        }

        return true;
    }

    private boolean shouldWaitForSpawnDesync(List<SupplyPosition> supplies) {
        if (supplies.isEmpty() || supplies.size() > PARTIAL_SPAWN_MAX_SUPPLIES) {
            return false;
        }

        if (firstSupplySeenAtMs == 0L) {
            return false;
        }

        long elapsedSinceFirstSupplyMs = System.currentTimeMillis() - firstSupplySeenAtMs;
        return elapsedSinceFirstSupplyMs < SUPPLY_SPAWN_DESYNC_GRACE_MS;
    }

    private long getAdaptiveFallbackDelayMs() {
        float averageTps = ServerUtils.getAverageTps();
        long averagePingMs = ServerUtils.getAveragePing().toMillis();

        long tpsBudgetMs = Math.max(0L, Math.round((20.0f - averageTps) * 220.0f));
        long pingBudgetMs = averagePingMs > PING_BASELINE_MS
                ? (averagePingMs - PING_BASELINE_MS) * 2L
                : 0L;

        long lagBudgetMs = Math.min(MAX_LAG_BUDGET_MS, tpsBudgetMs + pingBudgetMs);
        return EARLY_CHECK_DELAY_MS + lagBudgetMs;
    }
}
