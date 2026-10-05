package xyz.whatsyouss.frosty.utility.kuudra;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.ReceiveMessageEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.SupplyDropEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.SupplyPickupEvent;
import xyz.whatsyouss.frosty.events.impl.kuudra.SupplyPlaceEvent;
import xyz.whatsyouss.frosty.utility.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.dispatcher.detector.SupplyDetector).
 * Posts the same supply events the modules consume and keeps the supply progress in
 * {@link SupplyState}.
 */
public final class SupplyDetector {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final String SUPPLY_PICKUP_MESSAGE = "You retrieved some of Elle's supplies from the Lava!";
    private static final Pattern SUPPLY_PLACE_PATTERN = Pattern.compile("(.+) recovered one of Elle's supplies! \\((\\d)/6\\)");
    private static final Pattern SUPPLY_DROPPED_PATTERN = Pattern.compile("(.+) dropped Elle's supplies back into the lava! Oops!");

    private final SupplyState supplyState;

    public SupplyDetector(SupplyState supplyState) {
        this.supplyState = supplyState;
    }

    public void detect(ReceiveMessageEvent event, String strippedMessage) {
        if (strippedMessage.contains(SUPPLY_PICKUP_MESSAGE)) {
            LocalPlayer player = mc.player;
            if (player == null) return;

            Vec3 playerPos = player.position();
            Frosty.EVENT_BUS.post(new SupplyPickupEvent(
                    supplyState.findSupplyNear(playerPos, 3),
                    System.currentTimeMillis()
            ));
            return;
        }

        Matcher supplyPlacedMatcher = SUPPLY_PLACE_PATTERN.matcher(strippedMessage);
        if (supplyPlacedMatcher.find()) {
            String formattedMessage = event.getMessage().getString();
            double timeSeconds = supplyState.getElapsedTimeMillis() / 1000.0;

            Frosty.EVENT_BUS.post(new SupplyPlaceEvent(
                    formattedMessage,
                    StringUtils.extractFormattedPlayerName(formattedMessage),
                    Integer.parseInt(supplyPlacedMatcher.group(2)),
                    timeSeconds
            ));
            return;
        }

        Matcher supplyDroppedMatcher = SUPPLY_DROPPED_PATTERN.matcher(strippedMessage);
        if (supplyDroppedMatcher.find()) {
            String formattedMessage = event.getMessage().getString();
            Frosty.EVENT_BUS.post(new SupplyDropEvent(StringUtils.extractFormattedPlayerName(formattedMessage)));
            return;
        }

        if (strippedMessage.contains("You moved and the Chest slipped out of your hands!")) {
            LocalPlayer player = mc.player;
            if (player == null) return;
            supplyState.setSupplyProgress(0);
            Frosty.EVENT_BUS.post(new SupplyDropEvent(player.getName().getString()));
        }
    }
}
