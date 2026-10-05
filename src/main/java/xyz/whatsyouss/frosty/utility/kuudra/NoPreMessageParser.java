package xyz.whatsyouss.frosty.utility.kuudra;

import xyz.whatsyouss.frosty.utility.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ported from IQAddons (net.iqaddons.mod.utils.NoPreMessageParser).
 * Robust "no &lt;pile&gt;" detection that tolerates punctuation, formatting and unicode.
 */
public final class NoPreMessageParser {

    private static final Pattern NON_ALIAS_CHAR_PATTERN = Pattern.compile("[^a-z0-9 ]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final List<AliasRule> ALIAS_RULES;

    static {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("triangle", "TRIANGLE");
        aliases.put("tri", "TRIANGLE");
        aliases.put("x", "X");
        aliases.put("xc", "X CANNON");
        aliases.put("xcannon", "X CANNON");
        aliases.put("x cannon", "X CANNON");
        aliases.put("equals", "EQUALS");
        aliases.put("eq", "EQUALS");
        aliases.put("slash", "SLASH");
        aliases.put("shop", "SHOP");
        aliases.put("square", "SQUARE");

        List<AliasRule> rules = new ArrayList<>();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            Pattern pattern = Pattern.compile(
                    "\\b(?:no|missing)\\s+" + Pattern.quote(entry.getKey()) + "\\b",
                    Pattern.CASE_INSENSITIVE
            );
            rules.add(new AliasRule(entry.getKey(), entry.getValue(), pattern));
        }

        // Always prefer specific aliases first (e.g. "x cannon" before "x").
        rules.sort(Comparator.comparingInt((AliasRule rule) -> rule.alias().length()).reversed());
        ALIAS_RULES = List.copyOf(rules);
    }

    private NoPreMessageParser() {
    }

    public static ParsedNoPreCall parse(String message) {
        if (message == null || message.isBlank()) return null;

        String normalized = WHITESPACE_PATTERN.matcher(
                NON_ALIAS_CHAR_PATTERN.matcher(StringUtils.stripFormatting(message).toLowerCase(Locale.ROOT))
                        .replaceAll(" ")
        ).replaceAll(" ").trim();

        for (AliasRule rule : ALIAS_RULES) {
            Matcher matcher = rule.pattern().matcher(normalized);
            if (!matcher.find()) continue;

            int missingPreValue = PreSpot.getMissingPreValueFromPileName(rule.canonical());
            if (missingPreValue > 0) {
                return new ParsedNoPreCall(missingPreValue, rule.canonical());
            }
        }

        return null;
    }

    public record ParsedNoPreCall(int missingPreValue, String canonicalPileName) {
    }

    private record AliasRule(String alias, String canonical, Pattern pattern) {
    }
}
