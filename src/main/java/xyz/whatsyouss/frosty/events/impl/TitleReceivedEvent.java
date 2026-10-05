package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.network.chat.Component;
import xyz.whatsyouss.frosty.utility.StringUtils;

/**
 * Ported from IQAddons (net.iqaddons.mod.events.impl.TitleReceivedEvent).
 * Posted from {@code Hud#extractTitle} for the currently displayed title/subtitle,
 * deduplicated so it fires when the title text changes (used to track Kuudra supply
 * progress, which is rendered as a title).
 */
public class TitleReceivedEvent {

    private final Component title;
    private final Component subtitle;

    private final String message;
    private final String strippedMessage;

    public TitleReceivedEvent(Component title, Component subtitle) {
        this.title = title == null ? Component.empty() : title;
        this.subtitle = subtitle == null ? Component.empty() : subtitle;

        Component primaryText = this.title.getString().isEmpty()
                ? this.subtitle
                : this.title;

        this.message = primaryText.getString();
        this.strippedMessage = StringUtils.stripFormatting(message);
    }

    public Component getTitle() {
        return title;
    }

    public Component getSubtitle() {
        return subtitle;
    }

    public String getMessage() {
        return message;
    }

    public String getStrippedMessage() {
        return strippedMessage;
    }
}
