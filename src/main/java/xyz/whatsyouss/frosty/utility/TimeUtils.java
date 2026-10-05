package xyz.whatsyouss.frosty.utility;

import java.util.Locale;

/**
 * Ported from IQAddons (net.iqaddons.mod.utils.TimeUtils), reduced to the formatters
 * the profit tracker widget uses.
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    public static String formatTime(long time) {
        if (time <= 0) return "n/a";

        long days = time / (24L * 60L * 60L * 1000L);
        long hours = (time / (60L * 60L * 1000L)) % 24L;
        long minutes = (time / (60L * 1000L)) % 60L;
        long seconds = (time / 1000L) % 60L;

        StringBuilder builder = new StringBuilder();
        if (days > 0L) builder.append(days).append("d ");
        if (hours > 0L) builder.append(hours).append("h ");
        if (minutes > 0L) builder.append(minutes).append("m ");
        if (seconds > 0L) builder.append(seconds).append("s");

        String formatted = builder.toString();
        return formatted.isEmpty() ? "0s" : formatted;
    }

    public static String formatTime(double seconds) {
        if (seconds <= 0) return "0s";

        long totalSeconds = (long) seconds;
        long minutes = totalSeconds / 60;
        long remainingSeconds = totalSeconds % 60;

        if (minutes > 0) {
            return remainingSeconds > 0 ? minutes + "m" + remainingSeconds + "s" : minutes + "m";
        }

        return String.format(Locale.ROOT, "%.2fs", seconds);
    }
}
