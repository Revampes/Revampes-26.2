package xyz.whatsyouss.frosty.utility.kuudra;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.events.impl.ReceivePacketEvent;
import xyz.whatsyouss.frosty.events.impl.ScreenClickEvent;
import xyz.whatsyouss.frosty.events.impl.TitleReceivedEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.SupplyProgressEvent;
import xyz.whatsyouss.frosty.utility.EntityDetectorUtil;
import xyz.whatsyouss.frosty.utility.LocationUtils;
import xyz.whatsyouss.frosty.utility.ServerUtils;
import xyz.whatsyouss.frosty.utility.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Global Kuudra tracker, equivalent to IQAddons' KuudraEventsDispatcher +
 * SupplyWaypointsFeature tick, subscribed once at mod init. It feeds the shared
 * {@link KuudraState} / {@link SupplyState} singletons so the modules can stay thin.
 */
public final class KuudraListener {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final int ENVIRONMENT_CHECK_INTERVAL_TICKS = 10;
    private static final int DEBUG_LOG_INTERVAL_TICKS = 100;

    private static final Pattern SUPPLY_PROGRESS_PATTERN = Pattern.compile("^\\[[| ]+]\\s*(\\d+)%$");

    private final KuudraState kuudraState = KuudraState.get();
    private final SupplyState supplyState = SupplyState.get();
    private final SupplyDetector supplyDetector = new SupplyDetector(supplyState);
    private final ChestInteractionDetector chestInteractionDetector = new ChestInteractionDetector();

    private int tickCounter = 0;

    @EventHandler
    public void onTick(PreUpdateEvent event) {
        if (mc.player == null || mc.level == null) return;

        tickCounter++;
        if (tickCounter % ENVIRONMENT_CHECK_INTERVAL_TICKS == 0) {
            kuudraState.refreshEnvironment();
            chestInteractionDetector.evictExpired(mc.level.getGameTime());
        }
        kuudraState.tickBossCheck();

        if (kuudraState.phase() == KuudraPhase.SUPPLIES) {
            updateSupplyPositions();
        }

        if (tickCounter % DEBUG_LOG_INTERVAL_TICKS == 0) {
            logDebugState();
        }
    }

    @EventHandler
    public void onChat(ReceiveMessageEvent event) {
        if (mc.player == null) return;

        String strippedMessage = StringUtils.stripFormatting(event.getMessage().getString());
        kuudraState.refreshEnvironment();
        kuudraState.onChat(strippedMessage);

        if (kuudraState.isInKuudra()) {
            supplyDetector.detect(event, strippedMessage);
        }

        chestInteractionDetector.detect(event, mc.level == null ? 0L : mc.level.getGameTime());
    }

    @EventHandler
    public void onScreenClick(ScreenClickEvent event) {
        if (mc.level == null) return;
        chestInteractionDetector.detect(event, mc.level.getGameTime());
    }

    @EventHandler
    public void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof ClientboundSetTimePacket) {
            ServerUtils.onWorldTimeUpdate();
        }
    }

    /**
     * The Kuudra supply progress is rendered as a title (e.g. {@code [|||||   ] 35%});
     * reading it from the displayed title matches IQAddons' TitleReceivedEvent path.
     */
    @EventHandler
    public void onTitle(TitleReceivedEvent event) {
        if (!kuudraState.isInKuudra()) return;

        Matcher matcher = SUPPLY_PROGRESS_PATTERN.matcher(event.getStrippedMessage());
        if (!matcher.matches()) return;

        int progress = Integer.parseInt(matcher.group(1));
        supplyState.setSupplyProgress(progress);

        if (mc.player == null) return;

        Vec3 playerPos = mc.player.position();
        Frosty.EVENT_BUS.post(new SupplyProgressEvent(
                supplyState.findSupplyNear(playerPos, 3),
                PreSpot.fromPlayerPosition(playerPos),
                event.getMessage(),
                progress
        ));
    }

    private void updateSupplyPositions() {
        List<Giant> carriers = EntityDetectorUtil.getSupplyCarriers();
        List<SupplyPosition> positions = carriers.stream()
                .map(giant -> SupplyPosition.fromGiant(
                        giant.getX(),
                        giant.getZ(),
                        giant.getYRot(),
                        giant.getId()
                ))
                .toList();

        supplyState.updateSupplyPositions(positions);
    }

    private void logDebugState() {
        if (mc.player == null) return;

        List<Giant> giants = EntityDetectorUtil.getAllGiants();
        long belowY = giants.stream().filter(g -> g.getY() < 67.0).count();
        long skulls = giants.stream().filter(EntityDetectorUtil::isHoldingSkull).count();

        Frosty.LOGGER.info(
                "[Kuudra/debug] phase={} area='{}' giants={} (y<67:{} skull:{}) carriers={} supplies={} missingPre={} progress={} preSpot={}",
                kuudraState.phase(),
                LocationUtils.getCurrentArea(),
                giants.size(), belowY, skulls,
                EntityDetectorUtil.getSupplyCarriers().size(),
                supplyState.getActiveSupplies().size(),
                supplyState.getMissingPre(),
                supplyState.getCurrentSupplyProgress(),
                supplyState.getDetectedPreSpot()
        );
    }
}
