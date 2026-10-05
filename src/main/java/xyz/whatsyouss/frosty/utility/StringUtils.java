package xyz.whatsyouss.frosty.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ported from IQAddons (net.iqaddons.mod.utils.StringUtils), trimmed to the two
 * helpers the Kuudra supply detector/modules need. The extraction result is
 * identical after {@link #stripFormatting(String)}.
 */
public final class StringUtils {

    private static final Pattern MINECRAFT_NAME_PATTERN = Pattern.compile("([A-Za-z0-9_]{3,16})(?!.*[A-Za-z0-9_]{3,16})");

    private StringUtils() {
    }

    /**
     * Extracts the player name from a supply chat message. The original returned the
     * name prefixed with the player's rank colour; callers immediately strip the
     * formatting, so returning the plain name preserves behaviour.
     */
    public static String extractFormattedPlayerName(String message) {
        int endIndex = findMessageSeparator(message);
        String playerSection = endIndex > 0
                ? message.substring(0, endIndex).trim()
                : message;

        return formatPlayerNick(playerSection);
    }

    public static String formatPlayerNick(String rawPlayerText) {
        String normalizedText = removeChatPrefix(rawPlayerText.trim());
        String plainText = normalizedText.replaceAll("§.", "");

        Matcher nameMatcher = MINECRAFT_NAME_PATTERN.matcher(plainText);
        if (!nameMatcher.find()) {
            return normalizedText;
        }

        return nameMatcher.group(1);
    }

    public static String stripFormatting(String text) {
        return text.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }

    private static int findMessageSeparator(String formattedMessage) {
        int recoveredIndex = formattedMessage.indexOf("recovered");
        if (recoveredIndex > 0) {
            return recoveredIndex;
        }

        int droppedIndex = formattedMessage.indexOf("dropped");
        if (droppedIndex > 0) {
            return droppedIndex;
        }

        return formattedMessage.indexOf(':');
    }

    private static String removeChatPrefix(String rawPlayerText) {
        String strippedPrefix = rawPlayerText.replaceFirst("(?i)^(§.)*party\\s*>\\s*", "");
        int separator = strippedPrefix.indexOf(':');
        return separator > 0 ? strippedPrefix.substring(0, separator).trim() : strippedPrefix;
    }
}
