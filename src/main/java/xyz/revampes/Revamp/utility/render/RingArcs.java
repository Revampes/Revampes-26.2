package xyz.revampes.Revamp.utility.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Anti aliased ring segments, i.e. the slices of the circular leap menu.
 *
 * <p>{@code GuiGraphics} cannot draw a path, and building a circle out of axis aligned rectangles
 * is what makes the old radial menus look like staircases. Instead one 72 degree segment is
 * pre rendered into {@code frosty:dungeons/ui/leap_arc.png} (linear filtered and clamped through
 * its {@code .png.mcmeta}) and blitted five times, each rotated into place with the pose matrix.
 *
 * <p>The atlas holds two segments stacked vertically, each in a {@value #REGION}x{@value #REGION}
 * cell that maps one to one onto the leap menu's base units: the filled ring slice on top and the
 * thin outer outline underneath (empty slot / dead marker).
 */
public final class RingArcs {

    /** One atlas cell, in texels. Also the number of leap menu base units it covers. */
    public static final int REGION = 256;

    /** Angle of a single segment of the leap menu (72 degrees). */
    public static final float SEGMENT_SPAN = (float) (0.4 * Math.PI);

    private static final Identifier ATLAS =
            Identifier.fromNamespaceAndPath("frosty", "dungeons/ui/leap_arc.png");

    private static final int ATLAS_WIDTH = REGION;
    private static final int ATLAS_HEIGHT = REGION * 2;
    private static final int FILLED_ROW = 0;
    private static final int THIN_ROW = REGION;

    private RingArcs() {
    }

    /**
     * Draws one segment.
     *
     * @param centerX  ring centre, in GUI scaled pixels
     * @param centerY  ring centre, in GUI scaled pixels
     * @param unit     how many pixels one base unit is, i.e. the menu scale
     * @param rotation rotation of the segment's mid line, in radians (y grows downwards, matching
     *                 the {@code atan2} used for hit testing)
     * @param thin     draw the thin outer outline instead of the filled slice
     */
    public static void segment(GuiGraphicsExtractor ctx, float centerX, float centerY, float unit,
                               float rotation, boolean thin, int argb) {
        if ((argb >>> 24) == 0 || unit <= 0.0f) return;
        if (!RenderTextures.available(ATLAS)) return;

        float half = REGION / 2.0f;

        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.rotate(rotation);
        pose.scale(unit, unit);

        ctx.blit(RenderPipelines.GUI_TEXTURED, ATLAS,
                Math.round(-half), Math.round(-half),
                0.5f, (thin ? THIN_ROW : FILLED_ROW) + 0.5f,
                REGION, REGION,
                REGION - 1, REGION - 1,
                ATLAS_WIDTH, ATLAS_HEIGHT,
                argb);

        pose.popMatrix();
    }
}
