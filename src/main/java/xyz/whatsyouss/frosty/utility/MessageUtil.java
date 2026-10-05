package xyz.whatsyouss.frosty.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.HudNotificationEvent;

/**
 * Ported from IQAddons (net.iqaddons.mod.utils.MessageUtil), trimmed to the chat
 * helpers the Kuudra alerts use. PARTY routes through the vanilla "pc" command.
 */
public enum MessageUtil {

    SUCCESS("§a"),
    INFO("§7"),
    WARNING("§e"),
    ERROR("§c"),
    PARTY();

    private static final Minecraft mc = Minecraft.getInstance();
    private static final String PREFIX = "§d§l[Frosty] §r";

    private final String color;

    MessageUtil() {
        this.color = "";
    }

    MessageUtil(String color) {
        this.color = color;
    }

    public void sendMessage(String message) {
        mc.execute(() -> {
            LocalPlayer player = mc.player;
            if (player == null) return;

            if (this == PARTY) {
                player.connection.sendCommand("pc " + message);
                return;
            }

            player.sendSystemMessage(Component.literal(PREFIX + color + message));
        });
    }

    public static void sendFormattedMessage(String message) {
        mc.execute(() -> {
            LocalPlayer player = mc.player;
            if (player == null) return;

            player.sendSystemMessage(Component.literal(PREFIX + message.replace('&', '§')));
        });
    }

    /** Posts the big centred HUD notification IQAddons shows for Kuudra alerts. */
    public static void showAlert(String message, int durationTicks) {
        mc.execute(() -> Frosty.EVENT_BUS.post(new HudNotificationEvent(message, durationTicks)));
    }
}
