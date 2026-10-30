package xyz.revampes.Revamp.utility.dungeon.terminal;

/**
 * HSL helpers used by the slot hover/press animation.
 *
 * <p>yqloss brightens a hovered button by raising the HSL <em>lightness</em> (not the HSB
 * brightness), which is what keeps a hovered dark gray slot readable while a hovered green one
 * washes towards white. Ported so the tint behaves exactly like the original.
 */
public final class TerminalColors {

    private TerminalColors() {
    }

    /** ARGB packed colour to {@code {r, g, b, a}} in 0..1. */
    public static double[] toComponents(int argb) {
        return new double[]{
                ((argb >> 16) & 0xFF) / 255.0,
                ((argb >> 8) & 0xFF) / 255.0,
                (argb & 0xFF) / 255.0,
                ((argb >>> 24) & 0xFF) / 255.0
        };
    }

    /** {@code {r, g, b, a}} in 0..1 to an ARGB packed colour. */
    public static int toArgb(double[] components) {
        int a = clamp255(components[3]);
        int r = clamp255(components[0]);
        int g = clamp255(components[1]);
        int b = clamp255(components[2]);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp255(double value) {
        int scaled = (int) Math.round(value * 255.0);
        return Math.max(0, Math.min(255, scaled));
    }

    /** {@code {h, s, l}} all in 0..1. */
    public static double[] rgbToHsl(double r, double g, double b) {
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double l = (max + min) / 2.0;

        if (max == min) return new double[]{0.0, 0.0, l};

        double d = max - min;
        double s = l > 0.5 ? d / (2.0 - max - min) : d / (max + min);

        double h;
        if (max == r) {
            h = (g - b) / d + (g < b ? 6.0 : 0.0);
        } else if (max == g) {
            h = (b - r) / d + 2.0;
        } else {
            h = (r - g) / d + 4.0;
        }
        return new double[]{h / 6.0, s, l};
    }

    /** {@code {r, g, b, a}} in 0..1 from HSL plus an explicit alpha. */
    public static double[] hslToRgb(double h, double s, double l, double a) {
        double r;
        double g;
        double b;

        if (s == 0.0) {
            r = g = b = l;
        } else {
            double q = l < 0.5 ? l * (1.0 + s) : l + s - l * s;
            double p = 2.0 * l - q;
            r = hueToRgb(p, q, h + 1.0 / 3.0);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1.0 / 3.0);
        }
        return new double[]{r, g, b, a};
    }

    private static double hueToRgb(double p, double q, double t) {
        double tt = t;
        if (tt < 0.0) tt += 1.0;
        if (tt > 1.0) tt -= 1.0;
        if (tt < 1.0 / 6.0) return p + (q - p) * 6.0 * tt;
        if (tt < 1.0 / 2.0) return q;
        if (tt < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - tt) * 6.0;
        return p;
    }

    /** Multiplies the alpha of an ARGB colour, used for the title cross-fade. */
    public static int withAlpha(int argb, double factor) {
        int a = (int) Math.round(((argb >>> 24) & 0xFF) * factor);
        a = Math.max(0, Math.min(255, a));
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
