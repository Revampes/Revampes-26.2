package xyz.whatsyouss.frosty.utility;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.ReceivePacketEvent;

import java.util.regex.Pattern;

public class KuudraUtils {

    private static final Minecraft mc = Minecraft.getInstance();

//    private static final Pattern OWN_FRESH_REGEX = Pattern.compile("^Your Fresh Tools Perk bonus doubles your building speed for the next 10 seconds!$");

    public static boolean inKuudra = false;
    private static int tickCounter = 0;

    public static boolean isInKuudra() {
        return inKuudra;
    }

    @EventHandler
    private static void onPreUpdate(PreUpdateEvent event) {
        String area = LocationUtils.getCurrentArea();
        inKuudra = area != null && area.contains("kuudra");
    }

    @EventHandler
    private static void onReceivePacket(ReceivePacketEvent event) {
        if (!isInKuudra()) return;


    }
}
