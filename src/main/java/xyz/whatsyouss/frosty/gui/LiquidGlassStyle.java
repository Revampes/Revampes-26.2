package xyz.whatsyouss.frosty.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
import xyz.whatsyouss.frosty.utility.RenderUtils;

public final class LiquidGlassStyle {
    private static final float PANEL_RADIUS = 14.0f;
    private static final float CONTROL_RADIUS = 6.0f;

    private LiquidGlassStyle() {
    }

    public static boolean isEnabled() {
        return UI.liquidGlass.isToggled();
    }

    /** Primary (high-contrast) text color. */
    public static int textColor() {
        return isLight() ? 0xFF171D26 : 0xFFECF0F6;
    }

    /** Secondary / muted text color for labels & hints. */
    public static int mutedTextColor() {
        return isLight() ? 0xFF5D6B7C : 0xFFA9B4C2;
    }

    /** Neutral-slate accent used for highlights & the active left edge. */
    public static int accentColor() {
        return isLight() ? 0xFF5C7288 : 0xFF9DB1C4;
    }

    public static void drawPanel(GuiGraphicsExtractor context, float x, float y,
                                 float width, float height) {
        GlassRenderer.recordPanel(context, x, y, width, height, PANEL_RADIUS, isLight());
    }

    public static void drawHeader(GuiGraphicsExtractor context, float x, float y,
                                  float width, float height) {
        // Flat subtle tint so the 20px header reads as a title bar, not a slab.
        drawGlass(context, x, y, width, height, PANEL_RADIUS,
                isLight() ? 0x90FFFFFF : 0xB012161D);
    }

    public static void drawControl(GuiGraphicsExtractor context, float x, float y,
                                   float width, float height, boolean active,
                                   boolean hovered) {
        // Base surfaces are near-transparent to keep the panel's glass integrity;
        // the enabled "active" state borrows a faint slate accent + is brought
        // out further by a subtle border in drawGlass.
        int color;
        if (active) {
            // On-state: faint accent tint (light keeps it readable, dark glows).
            color = isLight() ? 0x245C7288 : 0x2E9DB1C4;
        } else {
            // Neutral fill, barely above the panel background.
            color = isLight() ? 0x14FFFFFF : 0x20FFFFFF;
        }
        if (hovered) {
            // Hover lifts the fill so rows feel interactive.
            color = (active ? mix(color, 0x1AFFFFFF) : addAlpha(color, 0x1C));
        }
        drawGlass(context, x, y, width, height, CONTROL_RADIUS, color);
    }

    public static void drawGlass(GuiGraphicsExtractor context, float x, float y,
                                 float width, float height, float radius, int fillColor) {
        // Soft drop shadow to separate controls from the blurry background.
        RenderUtils.drawRoundedRect(context, x + 1, y + 2, width, height, radius,
                (isLight() ? 0x0A1B2126 : 0x1A000000));
        // Main rounded fill.
        RenderUtils.drawRoundedRect(context, x, y, width, height, radius, fillColor);
        // Hairline border in a muted slate/white that stays crisp on glass.
        RenderUtils.drawRoundedBorder(context, x, y, width, height, radius,
                isLight() ? 0x33FFFFFF : 0x30AAB7C6);
        // Faint top glint across the rounded cap for a glassy highlight.
        float glintH = Math.min(2.0f, (height / 2.0f) - 1.0f);
        if (glintH > 0.5f) {
            RenderUtils.drawRoundedRect(context, x + 1.5f, y + 1.5f, width - 3,
                    glintH, Math.min(1.0f, radius / 4.0f), 0x10FFFFFF);
        }
    }

    private static boolean isLight() {
        return UI.clickGuiColor.getValue() == 0;
    }

    // ------------------------------------------------------------------
    // Small ARGB helpers (kept local to avoid importing a color lib).
    // ------------------------------------------------------------------
    private static int addAlpha(int argb, int delta) {
        int alpha = ((argb >>> 24) & 0xFF) + delta;
        alpha = Math.max(0, Math.min(255, alpha));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static int mix(int argb, int overlayArgb) {
        // 'overlayArgb' is treated as RGBA in [0,1]*255 added on top as soft light.
        int a0 = (argb >>> 24) & 0xFF;
        int r0 = (argb >> 16) & 0xFF, g0 = (argb >> 8) & 0xFF, b0 = argb & 0xFF;
        int oa = (overlayArgb >>> 24) & 0xFF, orr = (overlayArgb >> 16) & 0xFF;
        int og = (overlayArgb >> 8) & 0xFF, ob = overlayArgb & 0xFF;
        int na = Math.min(255, a0 + oa);
        // Blend overlay over base by its own alpha (use the overlay alpha only).
        int fa = oa;
        int rr = (orr * fa + r0 * (255 - fa)) / 255;
        int gg = (og * fa + g0 * (255 - fa)) / 255;
        int bb = (ob * fa + b0 * (255 - fa)) / 255;
        return (na << 24) | (rr << 16) | (gg << 8) | bb;
    }
}
