package xyz.whatsyouss.frosty.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
import xyz.whatsyouss.frosty.utility.RenderUtils;

public final class LiquidGlassStyle {
    private static final float PANEL_RADIUS = 10.0f;
    private static final float CONTROL_RADIUS = 5.0f;

    private LiquidGlassStyle() {
    }

    public static boolean isEnabled() {
        return UI.liquidGlass.isToggled();
    }

    public static int textColor() {
        return isLight() ? 0xFF172033 : 0xFFF2F7FF;
    }

    public static int mutedTextColor() {
        return isLight() ? 0xFF526176 : 0xFFB8C5D8;
    }

    public static int accentColor() {
        return isLight() ? 0xFF5D81FF : 0xFF88A8FF;
    }

    public static void drawPanel(GuiGraphicsExtractor context, float x, float y,
                                 float width, float height) {
        GlassRenderer.recordPanel(context, x, y, width, height, PANEL_RADIUS, isLight());
    }

    public static void drawHeader(GuiGraphicsExtractor context, float x, float y,
                                  float width, float height) {
        drawGlass(context, x, y, width, height, PANEL_RADIUS,
                isLight() ? 0x78678CFF : 0x8A2A4A95);
    }

    public static void drawControl(GuiGraphicsExtractor context, float x, float y,
                                   float width, float height, boolean active,
                                   boolean hovered) {
        int color = active
                ? (isLight() ? 0x745D82F5 : 0x804D72D2)
                : (isLight() ? 0x28FFFFFF : 0x3024334D);
        if (hovered) {
            color = active
                    ? (isLight() ? 0x9A799BFF : 0xA06D94F5)
                    : (isLight() ? 0x4AE4EDFF : 0x50617AA8);
        }
        drawGlass(context, x, y, width, height, CONTROL_RADIUS, color);
    }

    public static void drawGlass(GuiGraphicsExtractor context, float x, float y,
                                 float width, float height, float radius, int fillColor) {
        RenderUtils.drawRoundedRect(context, x + 1, y + 2, width, height, radius,
                0x18000000);
        RenderUtils.drawRoundedRect(context, x, y, width, height, radius, fillColor);
        RenderUtils.drawRoundedBorder(context, x, y, width, height, radius,
                isLight() ? 0x70FFFFFF : 0x60CFE1FF);
        RenderUtils.drawRoundedRect(context, x + 2, y + 2, width - 4,
                Math.min(3.0f, height - 4), Math.min(2.0f, radius - 2),
                0x1AFFFFFF);
    }

    private static boolean isLight() {
        return UI.clickGuiColor.getValue() == 0;
    }
}
