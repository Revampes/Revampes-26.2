package xyz.whatsyouss.frosty.utility.kuudra;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.events.impl.ScreenClickEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.KuudraChestOpenEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.KuudraChestRerollEvent;
import xyz.whatsyouss.frosty.utility.StringUtils;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ChestProfitUtil;
import xyz.whatsyouss.frosty.utility.kuudra.profit.ChestType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.dispatcher.detector.ChestInteractionDetector).
 * Turns a buy/reroll slot click plus the resulting "PAID CHEST REWARDS" chat line into
 * {@link KuudraChestOpenEvent} / {@link KuudraChestRerollEvent}.
 */
public final class ChestInteractionDetector {

    private static final int BUY_SLOT = 31;
    private static final int REROLL_SLOT = 50;
    private static final int SHARD_REROLL_SLOT = 51;
    private static final long WINDOW_STATE_TTL_TICKS = 20L * 60L * 5L;

    private static final String PAID_CHEST_REWARDS_MESSAGE = "PAID CHEST REWARDS";
    private static final String FREE_CHEST_REWARDS_MESSAGE = "FREE CHEST REWARDS";

    private final Map<Integer, ChestWindowState> windowStates = new ConcurrentHashMap<>();

    public void detect(ScreenClickEvent event, long tickCount) {
        if (!(event.screen() instanceof AbstractContainerScreen<?> screen)) return;
        if (!(screen instanceof ContainerScreen containerScreen)) return;

        String title = containerScreen.getTitle().getString();
        ChestType chestType = ChestType.fromString(title);
        if (chestType == ChestType.UNKNOWN) return;

        Slot slot = event.slot();
        if (slot == null || isOpened(slot.getItem())) return;

        int windowId = screen.getMenu().containerId;
        ChestWindowState state = windowStates.computeIfAbsent(windowId, key -> new ChestWindowState());
        state.lastInteractionTick = tickCount;

        if (state.pendingOpens.isEmpty()) {
            state.rerolled = false;
            state.shardRerolled = false;
        }

        if (slot.index == REROLL_SLOT && !state.rerolled && ChestProfitUtil.canUseReroll(slot.getItem(), "rerolled this chest")) {
            state.rerolled = true;
            Frosty.EVENT_BUS.post(new KuudraChestRerollEvent(windowId, KuudraChestRerollEvent.RerollType.ITEMS));
            return;
        }

        if (slot.index == SHARD_REROLL_SLOT && !state.shardRerolled && ChestProfitUtil.canUseReroll(slot.getItem(), "rerolled this shard")) {
            state.shardRerolled = true;
            Frosty.EVENT_BUS.post(new KuudraChestRerollEvent(windowId, KuudraChestRerollEvent.RerollType.SHARD));
            return;
        }

        if (slot.index != BUY_SLOT) return;
        if (!slot.getItem().isEmpty() && !isBuyAction(slot.getItem())) return;

        state.pendingOpens.addLast(new PendingChestOpen(
                windowId,
                title,
                new ArrayList<>(screen.getMenu().slots),
                chestType
        ));
    }

    public void detect(ReceiveMessageEvent event, long tickCount) {
        String message = StringUtils.stripFormatting(event.getMessage().getString());
        if (!message.contains(PAID_CHEST_REWARDS_MESSAGE) && !message.contains(FREE_CHEST_REWARDS_MESSAGE)) return;

        windowStates.values().stream()
                .filter(state -> !state.pendingOpens.isEmpty())
                .max(Comparator.comparingLong(left -> left.lastInteractionTick))
                .ifPresent(state -> {
                    state.lastInteractionTick = tickCount;
                    PendingChestOpen pendingOpen = state.pendingOpens.pollFirst();
                    if (pendingOpen == null) return;

                    Frosty.EVENT_BUS.post(new KuudraChestOpenEvent(
                            pendingOpen.windowId(),
                            pendingOpen.title(),
                            pendingOpen.slots(),
                            pendingOpen.chestType()
                    ));
                });
    }

    public void evictExpired(long tickCount) {
        windowStates.entrySet().removeIf(entry -> tickCount - entry.getValue().lastInteractionTick > WINDOW_STATE_TTL_TICKS);
    }

    private boolean isBuyAction(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        return ChestProfitUtil.getLoreLines(stack).stream()
                .map(StringUtils::stripFormatting)
                .anyMatch(line -> line.contains("Click to open!"));
    }

    private boolean isOpened(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        return ChestProfitUtil.getLoreLines(stack).stream()
                .map(StringUtils::stripFormatting)
                .anyMatch(line -> line.contains("Already opened!")
                        || line.contains("Chest already opened!")
                        || line.contains("You have already opened a chest!"));
    }

    private static final class ChestWindowState {
        private volatile boolean rerolled;
        private volatile boolean shardRerolled;
        private final Deque<PendingChestOpen> pendingOpens = new ConcurrentLinkedDeque<>();
        private volatile long lastInteractionTick;
    }

    private record PendingChestOpen(int windowId, String title, java.util.List<Slot> slots, ChestType chestType) {
    }
}
