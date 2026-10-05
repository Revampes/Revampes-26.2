package xyz.whatsyouss.frosty.utility.kuudra;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Ported from IQAddons (net.iqaddons.mod.model.kuudra.KuudraPhase).
 * Kept behaviour-identical so the Kuudra modules gate on the same phases.
 */
public enum KuudraPhase {

    NONE("None", -1, msg -> msg.contains("Sending to server") || msg.contains("Starting in 4 seconds...")),
    SUPPLIES("Supplies", 0, msg -> msg.contains("[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!")),
    BUILD("Build", 1, msg -> msg.contains("[NPC] Elle: OMG! Great work collecting my supplies!")),
    EATEN("Eaten", 2, msg -> msg.contains("[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!")),
    STUN("Stun", 3, msg -> (msg.contains("has been eaten by Kuudra!") && !msg.contains("Elle"))),
    DPS("DPS", 4, msg -> msg.contains("destroyed one of Kuudra's pods!")),
    SKIP("Skip", 5, msg -> msg.contains("[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!")),
    BOSS("Boss", 6, msg -> false), // The boss phase is detected by player Y position
    COMPLETED("Completed", 7, msg -> msg.contains("KUUDRA DOWN!"));

    public static final KuudraPhase[] RUN_PHASES = {SUPPLIES, BUILD, EATEN, STUN, DPS, SKIP, BOSS};
    public static final Set<KuudraPhase> COMBAT_PHASES = EnumSet.of(STUN, DPS, SKIP, BOSS);
    public static final Set<KuudraPhase> PRE_COMBAT_PHASES = EnumSet.of(SUPPLIES, BUILD, EATEN);

    private final String displayName;
    private final int order;
    private final Predicate<String> trigger;

    KuudraPhase(String displayName, int order, Predicate<String> trigger) {
        this.displayName = displayName;
        this.order = order;
        this.trigger = trigger;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getOrder() {
        return order;
    }

    public boolean isActive() {
        return this != NONE && this != COMPLETED;
    }

    public boolean isInRun() {
        return order >= 0 && order <= BOSS.order;
    }

    public boolean isCombatPhase() {
        return COMBAT_PHASES.contains(this);
    }

    public boolean isPreCombat() {
        return PRE_COMBAT_PHASES.contains(this);
    }

    public KuudraPhase next() {
        return switch (this) {
            case NONE -> SUPPLIES;
            case SUPPLIES -> BUILD;
            case BUILD -> EATEN;
            case EATEN -> STUN;
            case STUN -> DPS;
            case DPS -> SKIP;
            case SKIP -> BOSS;
            case BOSS, COMPLETED -> COMPLETED;
        };
    }

    public KuudraPhase previous() {
        return switch (this) {
            case NONE, SUPPLIES -> NONE;
            case BUILD -> SUPPLIES;
            case EATEN -> BUILD;
            case STUN -> EATEN;
            case DPS -> STUN;
            case SKIP -> DPS;
            case BOSS -> SKIP;
            case COMPLETED -> BOSS;
        };
    }

    public static KuudraPhase fromMessage(String message) {
        for (KuudraPhase phase : values()) {
            if (phase.trigger.test(message)) {
                return phase;
            }
        }
        return null;
    }

    public static Predicate<KuudraPhase> isOneOf(KuudraPhase... phases) {
        var phaseSet = EnumSet.noneOf(KuudraPhase.class);
        phaseSet.addAll(Set.of(phases));
        return phaseSet::contains;
    }
}
