package xyz.whatsyouss.frosty.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.util.debugchart.LocalSampleLogger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from IQAddons (net.iqaddons.mod.utils.ServerUtils).
 * TPS is updated from {@code ClientboundSetTimePacket} (see KuudraListener), ping from
 * the vanilla debug ping logger.
 */
public final class ServerUtils {

    private static volatile long previousUpdateMillis;
    private static volatile float averageTps = 20.0f;

    private ServerUtils() {
    }

    public static float getAverageTps() {
        return averageTps;
    }

    public static Duration getAveragePing() {
        List<Long> previousPings = getPreviousPings();
        if (previousPings.isEmpty()) {
            return Duration.ZERO;
        }

        double average = previousPings.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        return Duration.ofMillis((long) average);
    }

    public static List<Long> getPreviousPings() {
        LocalSampleLogger pingLogger = Minecraft.getInstance().getDebugOverlay().getPingLogger();
        if (pingLogger == null) {
            return List.of();
        }

        List<Long> list = new ArrayList<>();
        for (int i = 0; i < pingLogger.size(); i++) {
            list.add(pingLogger.get(i));
        }
        return list;
    }

    public static void onWorldTimeUpdate() {
        long now = System.currentTimeMillis();
        long previous = previousUpdateMillis;

        if (previous != 0L) {
            float tps = 20_000f / (now - previous + 1L);
            averageTps = Math.clamp(tps, 0.0f, 20.0f);
        }

        previousUpdateMillis = now;
    }
}
