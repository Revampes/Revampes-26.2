package xyz.revampes.Revamp.utility.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Anti aliased rounded rectangles.
 *
 * <p>{@code GuiGraphics} can only fill axis aligned integer rectangles, so the old implementation
 * approximated a rounded corner with one inset fill per pixel row. At a GUI scale of 2 or 3 that
 * turns a 12px radius into a visible staircase.
 *
 * <p>Instead this draws a nine-slice from a pre rendered, anti aliased silhouette
 * ({@code frosty:dungeons/ui/rounded.png}, linear filtered and clamped through its
 * {@code .png.mcmeta}): the four corners are blitted from the curved regions of the atlas, the
 * edges and the centre are stretched from the straight ones. Corners and edges therefore stay
 * smooth at any radius, any GUI scale and any fractional position, at the cost of nine quads.
 */
public final class RoundedRects {

    /** Corner region of the atlas, in texels. Also the radius the atlas was drawn with. */
    private static final int SLICE = 16;
    private static final int ATLAS_SIZE = SLICE * 2 + 1;

    private static final Identifier ATLAS =
            Identifier.fromNamespaceAndPath("frosty", "dungeons/ui/rounded.png");

    /** Same corner geometry as the atlas above, but with the area outside the corner opaque. */
    private static final Identifier CUTOUT =
            Identifier.fromNamespaceAndPath("frosty", "dungeons/ui/rounded_cutout.png");
    private static final int CUTOUT_SIZE = SLICE * 2 + 1;

    private static final float[] CUT_TOP_LEFT = {0, 0, SLICE, SLICE};
    private static final float[] CUT_TOP_RIGHT = {SLICE + 1, 0, SLICE, SLICE};
    private static final float[] CUT_BOTTOM_LEFT = {0, SLICE + 1, SLICE, SLICE};
    private static final float[] CUT_BOTTOM_RIGHT = {SLICE + 1, SLICE + 1, SLICE, SLICE};

    private static final float[] CORNER_TOP_LEFT = {0, 0, SLICE, SLICE};
    private static final float[] CORNER_TOP_RIGHT = {SLICE + 1, 0, SLICE, SLICE};
    private static final float[] CORNER_BOTTOM_LEFT = {0, SLICE + 1, SLICE, SLICE};
    private static final float[] CORNER_BOTTOM_RIGHT = {SLICE + 1, SLICE + 1, SLICE, SLICE};

    private static final float[] EDGE_TOP = {SLICE, 0, 1, SLICE};
    private static final float[] EDGE_BOTTOM = {SLICE, SLICE + 1, 1, SLICE};
    private static final float[] EDGE_LEFT = {0, SLICE, SLICE, 1};
    private static final float[] EDGE_RIGHT = {SLICE + 1, SLICE, SLICE, 1};

    private RoundedRects() {
    }

    public static void fill(GuiGraphicsExtractor ctx, float x, float y, float width, float height,
                            float radius, int argb) {
        if ((argb >>> 24) == 0 || width < 0.5f || height < 0.5f) return;

        if (!atlasAvailable()) {
            fillBlocky(ctx, x, y, width, height, radius, argb);
            return;
        }

        // Keep the geometry on integer pixels and put the sub pixel offset in the pose, so a
        // sliding or scaling rectangle stays smooth instead of snapping between pixels.
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.max(1, Math.round(width));
        int ih = Math.max(1, Math.round(height));
        int r = (int) Math.min(Math.max(0.0f, radius), Math.min(iw, ih) / 2.0f);

        float fracX = x - ix;
        float fracY = y - iy;

        Matrix3x2fStack pose = ctx.pose();
        boolean shifted = fracX != 0.0f || fracY != 0.0f;
        if (shifted) {
            pose.pushMatrix();
            pose.translate(fracX, fracY);
        }

        if (r <= 0) {
            ctx.fill(ix, iy, ix + iw, iy + ih, argb);
        } else {
            int innerW = iw - r * 2;
            int innerH = ih - r * 2;

            if (innerW > 0 && innerH > 0) {
                ctx.fill(ix + r, iy + r, ix + iw - r, iy + ih - r, argb);
            }
            if (innerW > 0) {
                blit(ctx, EDGE_TOP, ix + r, iy, innerW, r, argb);
                blit(ctx, EDGE_BOTTOM, ix + r, iy + ih - r, innerW, r, argb);
            }
            // Side strips survive when the rectangle is exactly two corners wide (a pill).
            if (innerH > 0) {
                blit(ctx, EDGE_LEFT, ix, iy + r, r, innerH, argb);
                blit(ctx, EDGE_RIGHT, ix + iw - r, iy + r, r, innerH, argb);
            }

            blit(ctx, CORNER_TOP_LEFT, ix, iy, r, r, argb);
            blit(ctx, CORNER_TOP_RIGHT, ix + iw - r, iy, r, r, argb);
            blit(ctx, CORNER_BOTTOM_LEFT, ix, iy + ih - r, r, r, argb);
            blit(ctx, CORNER_BOTTOM_RIGHT, ix + iw - r, iy + ih - r, r, r, argb);
        }

        if (shifted) pose.popMatrix();
    }

    /**
     * Soft drop shadow behind a rounded rectangle. The silhouette is grown outwards in a few
     * anti aliased rings with a quadratic falloff, which reads the same as the blurred box shadow
     * the yqloss terminal uses without needing a second texture or a custom pipeline.
     */
    public static void shadow(GuiGraphicsExtractor ctx, float x, float y, float width, float height,
                              float radius, float blur, int argb) {
        int baseAlpha = argb >>> 24;
        if (blur <= 0.0f || baseAlpha == 0) return;

        // A few more rings than the blur itself, so the falloff reads as a gradient rather than
        // as two stacked outlines.
        int steps = (int) Math.max(4.0f, Math.min(16.0f, Math.round(blur * 3.0f)));
        float stepSize = blur / steps;

        for (int step = steps; step >= 1; step--) {
            float grow = stepSize * step;
            float falloff = 1.0f - (float) (step - 1) / steps;
            int alpha = Math.round(baseAlpha * falloff * falloff * 1.6f / steps);
            if (alpha <= 0) continue;

            fill(ctx, x - grow, y - grow, width + grow * 2.0f, height + grow * 2.0f,
                    radius + grow, (alpha << 24) | (argb & 0x00FFFFFF));
        }
    }

    private static void blit(GuiGraphicsExtractor ctx, float[] region, int x, int y, int width, int height,
                             int argb) {
        if (width <= 0 || height <= 0) return;

        // One texel short of each region, so linear filtering samples texel centres and cannot
        // pull the neighbouring slice into the stretched quad.
        int srcWidth = Math.max(0, (int) region[2] - 1);
        int srcHeight = Math.max(0, (int) region[3] - 1);

        ctx.blit(RenderPipelines.GUI_TEXTURED, ATLAS, x, y,
                region[0] + 0.5f, region[1] + 0.5f,
                width, height,
                srcWidth, srcHeight,
                ATLAS_SIZE, ATLAS_SIZE,
                argb);
    }

    /** @return whether the nine slice atlas is present, so the fallback is only used when it is not. */
    private static boolean atlasAvailable() {
        if (atlasPresent != null) return atlasPresent;

        atlasPresent = RenderTextures.available(ATLAS);
        if (!atlasPresent) {
            System.err.println("Revampes: falling back to blocky rounded corners");
        }
        return atlasPresent;
    }

    /**
     * Erases the four corners of a square with the given colour, which is how a player face is
     * rounded: the face is blitted first and then the corners are painted over with whatever the
     * avatar is sitting on. Only correct on a flat background, which is exactly the use case
     * here (heads on the ring segments and on the centre disc).
     */
    public static void cutCorners(GuiGraphicsExtractor ctx, float x, float y, float width, float height,
                                  float radius, int argb) {
        if ((argb >>> 24) == 0 || width < 2.0f || height < 2.0f) return;
        if (radius < 0.5f || !RenderTextures.available(CUTOUT)) return;

        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.max(2, Math.round(width));
        int ih = Math.max(2, Math.round(height));
        int r = (int) Math.min(radius, Math.min(iw, ih) / 2.0f);
        if (r <= 0) return;

        blitCutout(ctx, CUT_TOP_LEFT, ix, iy, r, argb);
        blitCutout(ctx, CUT_TOP_RIGHT, ix + iw - r, iy, r, argb);
        blitCutout(ctx, CUT_BOTTOM_LEFT, ix, iy + ih - r, r, argb);
        blitCutout(ctx, CUT_BOTTOM_RIGHT, ix + iw - r, iy + ih - r, r, argb);
    }

    private static void blitCutout(GuiGraphicsExtractor ctx, float[] region, int x, int y, int size, int argb) {
        ctx.blit(RenderPipelines.GUI_TEXTURED, CUTOUT, x, y,
                region[0] + 0.5f, region[1] + 0.5f,
                size, size,
                SLICE - 1, SLICE - 1,
                CUTOUT_SIZE, CUTOUT_SIZE,
                argb);
    }

    /** Previous scanline implementation, kept only as a fallback when the atlas cannot be loaded. */
    private static void fillBlocky(GuiGraphicsExtractor ctx, float x, float y, float width, float height,
                                   float radius, int argb) {
        int left = Math.round(x);
        int top = Math.round(y);
        int right = Math.round(x + width);
        int bottom = Math.round(y + height);
        int r = Math.max(0, Math.min((int) radius, Math.min(right - left, bottom - top) / 2));

        for (int row = top; row < bottom; row++) {
            int inset = roundedInset(row - top, bottom - top, r);
            ctx.fill(left + inset, row, right - inset, row + 1, argb);
        }
    }

    private static int roundedInset(int row, int height, int radius) {
        if (radius <= 0 || (row >= radius && row < height - radius)) return 0;

        float centerOffset = row < radius ? radius - row - 0.5f : row - (height - radius) + 0.5f;
        double offset = Math.max(0.0, (double) radius * radius - (double) centerOffset * centerOffset);
        return Math.max(0, (int) Math.ceil(radius - Math.sqrt(offset)));
    }

    private static Boolean atlasPresent;
}

