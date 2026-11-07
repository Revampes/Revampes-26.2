package xyz.revampes.Revamp.utility.dungeon.leap;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.lwjgl.glfw.GLFW;
import xyz.revampes.Revamp.modules.impl.dungeon.LeapMenu;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonClass;
import xyz.revampes.Revamp.utility.dungeon.terminal.ExponentialSmooth;
import xyz.revampes.Revamp.utility.dungeon.terminal.TerminalColors;
import xyz.revampes.Revamp.utility.render.RingArcs;
import xyz.revampes.Revamp.utility.render.RoundedRects;

import java.util.List;

import static xyz.revampes.Revamp.Frosty.mc;

/**
 * The circular leap menu, ported from yqloss' {@code YCLeapMenuScreen}.
 *
 * <p>Geometry, in yqloss' base units and taken verbatim from the original:
 * <ul>
 *   <li>four evenly spaced ring segments of {@code 0.4 * PI} each, inner radius 55 and outer
 *       radius 120, drawn by {@link RingArcs} from a pre rendered anti aliased atlas;</li>
 *   <li>a centre disc of diameter 106 showing the currently preferred team mate;</li>
 *   <li>a 24x24 rounded head 85 units out along each segment's mid line, class name below;</li>
 *   <li>hover grows a segment to 1.05, pressing keeps it at 1.00 and drops the centre to 0.95,
 *       with yqloss' exponential smoothing on colour, lightness and scale.</li>
 * </ul>
 *
 * <p>Nothing here is assembled from axis aligned rectangles, so the ring stays smooth at any GUI
 * scale instead of turning into a staircase.
 */
public final class LeapMenuRenderer {

    private static final int SEGMENTS = 4;
    private static final double SEGMENT_SPAN = 0.4 * Math.PI;

    private static final double ARC_INNER = 55.0;
    private static final double ARC_OUTER = 120.0;
    private static final double CENTER_DIAMETER = 106.0;
    private static final double HEAD_DISTANCE = 85.0;
    private static final double HEAD_SIZE = 24.0;
    private static final double HEAD_ROUNDING = 8.0;

    private static final double CENTER_NAME_Y = -16.0;
    private static final double CENTER_CLASS_Y = 16.0;

    private static final double SMOOTH_SPEED = 0.5;

    private static final Piece[] segments = new Piece[SEGMENTS];
    private static final Piece center = new Piece();

    private static final ExponentialSmooth fade = new ExponentialSmooth(1.0);
    private static LeapTarget preferred;
    private static LeapTarget previousPreferred;
    private static boolean initialized;

    private LeapMenuRenderer() {
    }

    /** Drops hover and animation state so nothing leaks between two leap menus. */
    public static void reset() {
        for (int i = 0; i < SEGMENTS; i++) {
            if (segments[i] != null) segments[i].reset();
        }
        center.reset();
        fade.reset(1.0);
        preferred = null;
        previousPreferred = null;
        initialized = true;
    }

    /** The target the centre disc offers, i.e. the last hovered one. */
    public static LeapTarget preferredTarget() {
        return preferred;
    }

    public static void draw(GuiGraphicsExtractor ctx, Font font, LeapMenu module,
                            List<LeapTarget> targets, double mouseX, double mouseY) {
        draw(module, ctx, font, targets, mouseX, mouseY);
    }

    public static void draw(LeapMenu module, GuiGraphicsExtractor ctx, Font font,
                            List<LeapTarget> targets, double mouseX, double mouseY) {
        if (!initialized) reset();

        float scale = (float) Math.max(0.1, module.guiScale.getInput());
        float centerX = mc.getWindow().getGuiScaledWidth() / 2.0f;
        float centerY = mc.getWindow().getGuiScaledHeight() / 2.0f;

        // Mouse in base units: both the hover tests and yqloss' own maths work in this space.
        double localX = (mouseX - centerX) / scale;
        double localY = (mouseY - centerY) / scale;

        boolean anyDown = isMouseDown(0) || isMouseDown(1) || isMouseDown(2);
        boolean smooth = module.smoothGui.isToggled();

        int hovered = hoveredSegment(targets, localX, localY);
        if (hovered >= 0) setPreferred(targets.get(hovered));

        for (int i = 0; i < SEGMENTS; i++) {
            drawSegment(ctx, font, module, targets, i, centerX, centerY, scale,
                    hovered == i, anyDown, smooth);
        }

        drawCenter(ctx, font, module, centerX, centerY, scale, localX, localY, anyDown, smooth);
    }

    // ------------------------------------------------------------------ segments

    private static void drawSegment(GuiGraphicsExtractor ctx, Font font, LeapMenu module,
                                    List<LeapTarget> targets, int index,
                                    float centerX, float centerY, float scale,
                                    boolean hovered, boolean anyDown, boolean smooth) {
        LeapTarget target = index < targets.size() ? targets.get(index) : null;
        Piece piece = piece(index);

        double midAngle = (index * 0.5 - 0.75) * Math.PI;
        boolean active = hovered && target != null && !target.dead();

        double size = smooth
                ? piece.size(active, anyDown)
                : (active ? (anyDown ? 1.0 : 1.05) : 1.0);
        int arcColor = module.arcColor.getRGB();
        int color = smooth ? piece.color(arcColor, active, anyDown) : arcColor;

        float unit = (float) (scale * size);

        if (target == null) {
            // Empty slot: yqloss only draws the thin outer outline.
            RingArcs.segment(ctx, centerX, centerY, unit, (float) midAngle, true, arcColor);
            return;
        }

        RingArcs.segment(ctx, centerX, centerY, unit, (float) midAngle, false, color);
        if (target.dead()) {
            RingArcs.segment(ctx, centerX, centerY, unit, (float) midAngle, true, module.deadColor.getRGB());
        }

        float anchorX = (float) (centerX + Math.cos(midAngle) * HEAD_DISTANCE * unit);
        float anchorY = (float) (centerY + Math.sin(midAngle) * HEAD_DISTANCE * unit);

        float headSize = (float) (HEAD_SIZE * unit);
        drawHead(ctx, target, anchorX - headSize / 2.0f, anchorY - headSize / 2.0f,
                headSize, (float) (HEAD_ROUNDING * unit), color);

        int textColor = module.textColor.getRGB();
        int labelColor = target.dead() ? TerminalColors.withAlpha(textColor, 0.5) : textColor;
        centered(ctx, font, target.classLabel(), anchorX, anchorY + headSize / 2.0f + 3.0f, labelColor);
    }

    // ------------------------------------------------------------------ centre

    private static void drawCenter(GuiGraphicsExtractor ctx, Font font, LeapMenu module,
                                   float centerX, float centerY, float scale,
                                   double localX, double localY, boolean anyDown, boolean smooth) {
        boolean hovered = preferred != null && hitsCenter(localX, localY);

        double size = smooth
                ? center.size(hovered, anyDown)
                : (hovered ? (anyDown ? 0.95 : 1.0) : 1.0);
        int baseColor = module.centerColor.getRGB();
        int color = smooth ? center.color(baseColor, hovered, anyDown) : baseColor;

        float radius = (float) (CENTER_DIAMETER / 2.0 * scale * size);
        RoundedRects.fill(ctx, centerX - radius, centerY - radius, radius * 2.0f, radius * 2.0f, radius, color);

        double progress = smooth ? fade.approach(1.0, SMOOTH_SPEED) : 1.0;
        if (!smooth) fade.reset(1.0);

        if (progress < 1.0 && previousPreferred != null) {
            drawCenterContent(ctx, font, module, previousPreferred, centerX, centerY, scale,
                    (float) (1.0 - progress), color);
        }
        drawCenterContent(ctx, font, module, preferred, centerX, centerY, scale,
                (float) progress, color);
    }

    private static void drawCenterContent(GuiGraphicsExtractor ctx, Font font, LeapMenu module,
                                          LeapTarget target, float centerX, float centerY, float scale,
                                          float alpha, int backdrop) {
        if (alpha <= 0.02f) return;

        int textColor = TerminalColors.withAlpha(module.textColor.getRGB(), alpha);
        if (target == null) {
            centered(ctx, font, "No Target!", centerX, centerY - 5.0f,
                    TerminalColors.withAlpha(textColor, 0.5));
            return;
        }

        float headSize = (float) (HEAD_SIZE * scale);
        drawHead(ctx, target, centerX - headSize / 2.0f, centerY - headSize / 2.0f,
                headSize, (float) (HEAD_ROUNDING * scale),
                TerminalColors.withAlpha(backdrop, alpha), alpha);

        centered(ctx, font, target.name(), centerX,
                (float) (centerY + CENTER_NAME_Y * scale) - 5.0f, textColor);
        centered(ctx, font, target.classLabel(), centerX,
                (float) (centerY + CENTER_CLASS_Y * scale) - 5.0f,
                target.dead() ? TerminalColors.withAlpha(textColor, 0.5) : textColor);
    }

    // ------------------------------------------------------------------ heads

    /**
     * A player face rounded like yqloss' rounded avatar: the vanilla face extractor supplies head
     * and hat, then the four corners are painted over with the surface the avatar sits on.
     */
    private static void drawHead(GuiGraphicsExtractor ctx, LeapTarget target,
                                 float x, float y, float size, float rounding, int backdrop) {
        drawHead(ctx, target, x, y, size, rounding, backdrop, 1.0f);
    }

    private static void drawHead(GuiGraphicsExtractor ctx, LeapTarget target,
                                 float x, float y, float size, float rounding, int backdrop,
                                 float alpha) {
        int pixelSize = Math.max(1, Math.round(size));
        int pixelX = Math.round(x);
        int pixelY = Math.round(y);
        int tint = (((int) Math.round(255 * Math.max(0.0f, Math.min(1.0f, alpha)))) << 24) | 0xFFFFFF;

        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(target.name());
        PlayerSkin skin = info == null ? null : info.getSkin();

        boolean drawn = false;
        if (skin != null && skin.body() != null) {
            try {
                PlayerFaceExtractor.extractRenderState(ctx, skin, pixelX, pixelY, pixelSize, tint);
                drawn = true;
            } catch (Exception ignored) {
                // fall through to the plain disc below
            }
        }

        if (drawn) {
            RoundedRects.cutCorners(ctx, pixelX, pixelY, pixelSize, pixelSize, rounding, backdrop);
        } else {
            RoundedRects.fill(ctx, pixelX, pixelY, pixelSize, pixelSize, pixelSize / 2.0f,
                    classColor(target.role()));
        }

        if (target.dead()) {
            RoundedRects.fill(ctx, pixelX, pixelY, pixelSize, pixelSize, rounding, 0x7F000000);
        }
    }

    private static int classColor(DungeonClass role) {
        return role == DungeonClass.Unknown ? 0xFF343A40 : role.colorRgb();
    }

    // ------------------------------------------------------------------ hit testing

    /** yqloss' {@code RingArcButton} test: inside the annulus and inside one angular slice. */
    public static int hoveredSegment(List<LeapTarget> targets, double localX, double localY) {
        double radiusSquared = localX * localX + localY * localY;
        if (radiusSquared < ARC_INNER * ARC_INNER || radiusSquared >= ARC_OUTER * ARC_OUTER) return -1;

        double angle = Math.atan2(localY, localX);
        double twoPi = 2.0 * Math.PI;

        for (int i = 0; i < SEGMENTS; i++) {
            LeapTarget target = i < targets.size() ? targets.get(i) : null;
            if (target == null || target.dead()) continue;

            double from = (i * 0.5 - 0.95) * Math.PI;
            double to = from + SEGMENT_SPAN;

            double beforeEnd = normalise(to - angle);
            double afterStart = normalise(angle - from);
            if (afterStart + beforeEnd < twoPi) return i;
        }
        return -1;
    }

    public static boolean hitsCenter(double localX, double localY) {
        double radius = CENTER_DIAMETER / 2.0;
        return localX * localX + localY * localY < radius * radius;
    }

    private static double normalise(double angle) {
        double twoPi = 2.0 * Math.PI;
        double value = angle % twoPi;
        return value < 0 ? value + twoPi : value;
    }

    // ------------------------------------------------------------------ helpers

    private static void setPreferred(LeapTarget target) {
        if (target == null || target.equals(preferred)) return;
        previousPreferred = preferred;
        preferred = target;
        fade.set(0.0);
    }

    private static Piece piece(int index) {
        if (segments[index] == null) segments[index] = new Piece();
        return segments[index];
    }

    private static void centered(GuiGraphicsExtractor ctx, Font font, String text, float centerX,
                                 float topY, int color) {
        if ((color >>> 24) == 0) return;
        ctx.centeredText(font, text, Math.round(centerX), Math.round(topY), color);
    }

    private static boolean isMouseDown(int button) {
        long handle = mc.getWindow().handle();
        if (handle == 0) return false;
        return GLFW.glfwGetMouseButton(handle, button) == GLFW.GLFW_PRESS;
    }

    /**
     * One animated segment or disc: colour channels ease with {@code approachOrSet}, the hover
     * lightness with {@code approach}, and the hover/press scale on top of that.
     */
    private static final class Piece {

        private final ExponentialSmooth red = new ExponentialSmooth(0.0);
        private final ExponentialSmooth green = new ExponentialSmooth(0.0);
        private final ExponentialSmooth blue = new ExponentialSmooth(0.0);
        private final ExponentialSmooth alpha = new ExponentialSmooth(0.0);
        private final ExponentialSmooth lightness = new ExponentialSmooth(0.0);
        private final ExponentialSmooth scale = new ExponentialSmooth(1.0);

        double size(boolean hovered, boolean anyDown) {
            double target = hovered ? (anyDown ? 1.0 : 1.05) : 1.0;
            return scale.approach(target, SMOOTH_SPEED);
        }

        int color(int target, boolean hovered, boolean anyDown) {
            double[] components = TerminalColors.toComponents(target);
            double r = red.approachOrSet(components[0], SMOOTH_SPEED);
            double g = green.approachOrSet(components[1], SMOOTH_SPEED);
            double b = blue.approachOrSet(components[2], SMOOTH_SPEED);
            double a = alpha.approachOrSet(components[3], SMOOTH_SPEED);

            double[] hsl = TerminalColors.rgbToHsl(r, g, b);
            double modifier = hovered ? (anyDown ? 0.05 : 0.10) : 0.0;
            double eased = lightness.approach(modifier, SMOOTH_SPEED);
            double l = Math.min(1.0, hsl[2] + eased);

            return TerminalColors.toArgb(TerminalColors.hslToRgb(hsl[0], hsl[1], l, a));
        }

        void reset() {
            red.reset(0.0);
            green.reset(0.0);
            blue.reset(0.0);
            alpha.reset(0.0);
            lightness.reset(0.0);
            scale.reset(1.0);
        }
    }
}
