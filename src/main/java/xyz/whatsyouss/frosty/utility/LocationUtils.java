package xyz.whatsyouss.frosty.utility;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.events.impl.ReceivePacketEvent;

import java.util.Collection;

public class LocationUtils {

    private static final Minecraft mc = Minecraft.getInstance();
    private static String cachedTabArea = "Unknown";
    private static boolean registered = false;

    public static void init() {
        if (!registered) {
            Frosty.EVENT_BUS.subscribe(LocationUtils.class);
            registered = true;
        }
    }

    @EventHandler
    private static void onReceivePacket(ReceivePacketEvent event) {
        if (event.getPacket() instanceof ClientboundTabListPacket packet) {
            String header = packet.header().getString().replaceAll("(?i)\\\\u00A7[0-9A-FK-OR]", "");
            String footer = packet.footer().getString().replaceAll("(?i)\\\\u00A7[0-9A-FK-OR]", "");

            if (header.contains("Area: ")) {
                try { cachedTabArea = header.split("Area: ")[1].split("\n")[0].trim(); } catch(Exception ignored) {}
            }
            if (footer.contains("Area: ")) {
                try { cachedTabArea = footer.split("Area: ")[1].split("\n")[0].trim(); } catch(Exception ignored) {}
            }
        }
    }

    /**
     * Gets the current area the player is in from the Tab List.
     * Looks for a specific string pattern: "Area: <area_name>"
     */
    public static String getCurrentArea() {
        if (mc.getConnection() == null) return "Unknown";

        try {
            // Check fake players in tab list FIRST
            Collection<PlayerInfo> playerList = mc.getConnection().getOnlinePlayers();

            for (PlayerInfo entry : playerList) {
                String tabName = "";

                if (entry.getTabListDisplayName() != null) {
                    tabName = entry.getTabListDisplayName().getString();
                } else if (entry.getProfile() != null && entry.getProfile().name() != null) {
                    tabName = entry.getProfile().name();
                }

                if (tabName != null) {
                    tabName = tabName.replaceAll("(?i)\\\\u00A7[0-9A-FK-OR]", "");

                    if (tabName.contains("Area: ")) {
                        try {
                            return tabName.split("Area: ")[1].split("\n")[0].trim();
                        } catch (Exception e) {
                            return "Unknown";
                        }
                    }
                }
            }
        } catch (java.util.ConcurrentModificationException ignored) {
            // Failsafe for thread-safety issues when requested by async thread event calls
        }

        // Fallback to cached header/footer
        if (!cachedTabArea.equals("Unknown")) {
            return cachedTabArea;
        }

        return "Unknown";
    }

    /**
     * Checks if the player is currently in a Dungeon (Catacombs).
     */
    public static boolean isInDungeon() {
        if (mc.getConnection() == null) return false;

        Collection<PlayerInfo> playerList = mc.getConnection().getOnlinePlayers();

        for (PlayerInfo entry : playerList) {
            String tabName = "";

            if (entry.getTabListDisplayName() != null) {
                tabName = entry.getTabListDisplayName().getString();
            } else if (entry.getProfile() != null && entry.getProfile().name() != null) {
                tabName = entry.getProfile().name();
            }

            if (tabName != null) {
                tabName = tabName.replaceAll("(?i)\\\\u00A7[0-9A-FK-OR]", "");

                if (tabName.contains("Dungeon: Catacombs") || tabName.contains("Dungeon") || tabName.contains("Catacombs")) {
                    return true;
                }
            }
        }

        if (cachedTabArea != null && (cachedTabArea.contains("Dungeon") || cachedTabArea.contains("Catacombs"))) {
            return true;
        }

        return false;
    }
}
