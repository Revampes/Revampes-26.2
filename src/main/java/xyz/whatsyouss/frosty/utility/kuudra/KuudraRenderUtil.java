package xyz.whatsyouss.frosty.utility.kuudra;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import xyz.whatsyouss.frosty.utility.BufferSource;
import xyz.whatsyouss.frosty.utility.RenderLayers;
import xyz.whatsyouss.frosty.utility.RenderUtils;

import java.awt.Color;

/**
 * Small render helper mirroring the IQAddons WorldRenderUtils surface used by the
 * Kuudra waypoint modules (styled box, styled box + beacon beam, world geometry,
 * billboard markers and scaled world text), composed on top of the base
 * {@link RenderUtils} primitives.
 */
public final class KuudraRenderUtil {

    public enum RenderStyle {SOLID, OUTLINE, BOTH, NONE}

    private static final Minecraft mc = Minecraft.getInstance();
    private static final float LINE_WIDTH = 2f;
    private static final float DEFAULT_OUTLINE_LINE_WIDTH = 1f;
    private static final double BEAM_CROSS_SECTION = 0.5;

    private KuudraRenderUtil() {
    }

    public static RenderStyle parseStyle(String option) {
        if (option == null) return RenderStyle.BOTH;
        try {
            return RenderStyle.valueOf(option.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RenderStyle.BOTH;
        }
    }

    public static Color withOpacity(Color color, float opacityPercent) {
        float alpha = opacityPercent > 1.0f ? opacityPercent / 100.0f : opacityPercent;
        int a = Math.clamp((int) (alpha * 255f), 0, 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    public static void drawStyledBox(PoseStack stack, AABB box, boolean throughWalls, Color color, RenderStyle style) {
        boolean depthTest = !throughWalls;
        if (style == RenderStyle.SOLID || style == RenderStyle.BOTH) {
            RenderUtils.drawBoxFilled(stack, box, color, depthTest);
        }
        if (style == RenderStyle.OUTLINE || style == RenderStyle.BOTH) {
            RenderUtils.drawBox(stack, box, color, LINE_WIDTH, depthTest);
        }
    }

    /**
     * Draws a styled box plus the beacon beam above it. The beam is a filled column
     * (0.5 x height x 0.5) matching IQAddons' WorldRenderUtils.drawBeam, not a thin line.
     */
    public static void drawStyledWithBeam(PoseStack stack, AABB box, int height, boolean throughWalls,
                                          Color color, RenderStyle style) {
        drawStyledBox(stack, box, throughWalls, color, style);

        Vec3 center = box.getCenter();
        Vec3 beamStart = new Vec3(center.x, box.maxY, center.z);
        AABB beam = AABB.ofSize(beamStart, BEAM_CROSS_SECTION, 0.0, BEAM_CROSS_SECTION)
                .expandTowards(0, height, 0);

        RenderUtils.drawBoxFilled(stack, beam, color, !throughWalls);
    }

    public static void drawFilled(PoseStack stack, AABB box, boolean throughWalls, Color color) {
        RenderUtils.drawBoxFilled(stack, box, color, !throughWalls);
    }

    public static void drawOutline(PoseStack stack, AABB box, boolean throughWalls, Color color) {
        drawOutline(stack, box, throughWalls, color, DEFAULT_OUTLINE_LINE_WIDTH);
    }

    public static void drawOutline(PoseStack stack, AABB box, boolean throughWalls, Color color, float lineWidth) {
        RenderUtils.drawBox(stack, box, color, lineWidth, !throughWalls);
    }

    /**
     * Camera-facing square outline. Emitted as four independent segments because the
     * line layer uses the paired {@code LINES} topology (a strip would render only
     * every other edge - IQAddons avoids this by using a {@code DEBUG_LINE_STRIP}
     * pipeline that this client does not have for line width 2).
     */
    public static void drawBillboardSquareOutline(PoseStack stack, Vec3 center, float size,
                                                  boolean throughWalls, Color color) {
        float half = Math.max(1.0E-4f, size / 2.0f);

        stack.pushPose();
        translateBillboard(stack, center);

        BufferSource bs = new BufferSource();
        VertexConsumer buffer = bs.getBuffer(RenderLayers.getLines(!throughWalls));
        PoseStack.Pose entry = stack.last();
        int packed = packed(color);

        line(buffer, entry, -half, -half, half, -half, packed);
        line(buffer, entry, half, -half, half, half, packed);
        line(buffer, entry, half, half, -half, half, packed);
        line(buffer, entry, -half, half, -half, -half, packed);

        bs.uploadAndDraw();
        stack.popPose();
    }

    /**
     * Camera-facing ring with real (world-unit) thickness, matching IQAddons'
     * drawThickBillboardCircleOutline. Emitted as an unordered triangle list; the
     * QUADS layer used elsewhere would mis-parse a triangle list. Every quad is
     * emitted twice with opposite winding so the ring stays visible whichever way
     * it faces (IQAddons additionally disables culling).
     */
    public static void drawThickBillboardCircleOutline(PoseStack stack, Vec3 center, float radius,
                                                       float thickness, int segments,
                                                       boolean throughWalls, Color color) {
        int segmentCount = Math.max(3, segments);
        if (!Float.isFinite(radius) || !Float.isFinite(thickness) || radius <= 0.0f || thickness <= 0.0f) return;

        float halfThickness = Math.min(thickness / 2.0f, radius);
        float innerRadius = Math.max(0.0f, radius - halfThickness);
        float outerRadius = radius + halfThickness;

        stack.pushPose();
        translateBillboard(stack, center);

        BufferSource bs = new BufferSource();
        VertexConsumer buffer = bs.getBuffer(RenderLayers.getTriangles(!throughWalls));
        PoseStack.Pose entry = stack.last();

        for (int i = 0; i < segmentCount; i++) {
            double angle1 = (Math.PI * 2.0) * i / segmentCount;
            double angle2 = (Math.PI * 2.0) * (i + 1) / segmentCount;

            float innerX1 = (float) (Math.cos(angle1) * innerRadius);
            float innerY1 = (float) (Math.sin(angle1) * innerRadius);
            float outerX1 = (float) (Math.cos(angle1) * outerRadius);
            float outerY1 = (float) (Math.sin(angle1) * outerRadius);
            float innerX2 = (float) (Math.cos(angle2) * innerRadius);
            float innerY2 = (float) (Math.sin(angle2) * innerRadius);
            float outerX2 = (float) (Math.cos(angle2) * outerRadius);
            float outerY2 = (float) (Math.sin(angle2) * outerRadius);

            triangle(buffer, entry, innerX1, innerY1, outerX1, outerY1, outerX2, outerY2, color);
            triangle(buffer, entry, innerX1, innerY1, outerX2, outerY2, innerX2, innerY2, color);
            triangle(buffer, entry, outerX2, outerY2, outerX1, outerY1, innerX1, innerY1, color);
            triangle(buffer, entry, innerX2, innerY2, outerX2, outerY2, innerX1, innerY1, color);
        }

        bs.uploadAndDraw();
        stack.popPose();
    }

    public static void drawText(PoseStack stack, Vec3 pos, String text, float scale, boolean throughWalls, Color color) {
        RenderUtils.drawText3D(stack, text, pos.x, pos.y, pos.z, color, scale);
    }

    /** Interpolated entity box drawn with a render style, matching IQAddons' drawStyledHitbox. */
    public static void drawStyledHitbox(PoseStack stack, Entity entity, float tickDelta, boolean throughWalls,
                                        Color color, RenderStyle style) {
        if (entity == null) return;

        double x = entity.xo + (entity.getX() - entity.xo) * tickDelta;
        double y = entity.yo + (entity.getY() - entity.yo) * tickDelta;
        double z = entity.zo + (entity.getZ() - entity.zo) * tickDelta;

        float halfWidth = entity.getBbWidth() / 2.0f;
        AABB box = new AABB(
                x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + entity.getBbHeight(), z + halfWidth
        );

        drawStyledBox(stack, box, throughWalls, color, style);
    }

    /**
     * Flat ground ring with real (world-unit) thickness, the line primitive of IQAddons'
     * drawThickCircleOutline (the Fire Veil radius). Emitted as double-wound quads because
     * the quad layer culls back faces.
     */
    public static void drawThickCircleOutline(PoseStack stack, Vec3 center, float radius, float thickness,
                                              int segments, boolean throughWalls, Color color) {
        int count = Math.max(3, segments);
        if (!Float.isFinite(radius) || !Float.isFinite(thickness) || radius <= 0.0f || thickness <= 0.0f) return;

        float halfThickness = Math.min(thickness / 2.0f, radius);
        float innerRadius = Math.max(0.0f, radius - halfThickness);
        float outerRadius = radius + halfThickness;

        Vec3 cam = cameraPosition();
        float planeY = (float) (center.y - cam.y);
        int packed = packed(color);

        BufferSource bs = new BufferSource();
        VertexConsumer buffer = bs.getBuffer(RenderLayers.getQuads(!throughWalls));
        PoseStack.Pose entry = stack.last();

        for (int i = 0; i < count; i++) {
            double angle1 = (Math.PI * 2.0) * i / count;
            double angle2 = (Math.PI * 2.0) * (i + 1) / count;

            float innerX1 = (float) (center.x + Math.cos(angle1) * innerRadius - cam.x);
            float innerZ1 = (float) (center.z + Math.sin(angle1) * innerRadius - cam.z);
            float outerX1 = (float) (center.x + Math.cos(angle1) * outerRadius - cam.x);
            float outerZ1 = (float) (center.z + Math.sin(angle1) * outerRadius - cam.z);
            float innerX2 = (float) (center.x + Math.cos(angle2) * innerRadius - cam.x);
            float innerZ2 = (float) (center.z + Math.sin(angle2) * innerRadius - cam.z);
            float outerX2 = (float) (center.x + Math.cos(angle2) * outerRadius - cam.x);
            float outerZ2 = (float) (center.z + Math.sin(angle2) * outerRadius - cam.z);

            quadDoubleSided(buffer, entry,
                    innerX1, planeY, innerZ1,
                    outerX1, planeY, outerZ1,
                    outerX2, planeY, outerZ2,
                    innerX2, planeY, innerZ2,
                    packed);
        }

        bs.uploadAndDraw();
    }

    /**
     * Wireframe cylinder wall, the line primitive of IQAddons' drawCircleWireframeWall:
     * three stacked rings plus vertical markers.
     */
    public static void drawCircleWireframeWall(PoseStack stack, Vec3 center, float radius, float height,
                                               int segments, boolean throughWalls, Color color) {
        int count = Math.max(3, segments);
        if (!Float.isFinite(radius) || !Float.isFinite(height) || radius <= 0.0f || height <= 0.0f) return;

        float thickness = Math.max(0.02f, radius * 0.012f);
        drawThickCircleOutline(stack, center, radius, thickness, count, throughWalls, color);
        drawThickCircleOutline(stack, center.add(0.0, height * 0.5f, 0.0), radius, thickness, count, throughWalls, color);
        drawThickCircleOutline(stack, center.add(0.0, height, 0.0), radius, thickness, count, throughWalls, color);

        int markers = Math.max(8, count / 5);
        Vec3 cam = cameraPosition();
        int packed = packed(color);

        BufferSource bs = new BufferSource();
        VertexConsumer buffer = bs.getBuffer(RenderLayers.getQuads(!throughWalls));
        PoseStack.Pose entry = stack.last();

        float halfMarker = 0.015f;
        for (int i = 0; i < markers; i++) {
            double angle = (Math.PI * 2.0) * i / markers;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            float baseX = (float) (center.x + cos * radius - cam.x);
            float baseZ = (float) (center.z + sin * radius - cam.z);
            float minX = (float) (center.x + (cos * radius - cos * halfMarker) - cam.x);
            float maxX = (float) (center.x + (cos * radius + cos * halfMarker) - cam.x);
            float minZ = (float) (center.z + (sin * radius - sin * halfMarker) - cam.z);
            float maxZ = (float) (center.z + (sin * radius + sin * halfMarker) - cam.z);

            quadDoubleSided(buffer, entry,
                    minX, (float) (center.y - cam.y), minZ,
                    maxX, (float) (center.y - cam.y), maxZ,
                    maxX, (float) (center.y + height - cam.y), maxZ,
                    minX, (float) (center.y + height - cam.y), minZ,
                    packed);
        }

        bs.uploadAndDraw();
    }

    private static Vec3 cameraPosition() {
        return mc.getEntityRenderDispatcher().camera.position();
    }

    private static void quadDoubleSided(VertexConsumer buffer, PoseStack.Pose entry,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        float x3, float y3, float z3,
                                        float x4, float y4, float z4,
                                        int packed) {
        quad(buffer, entry, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, packed);
        quad(buffer, entry, x4, y4, z4, x3, y3, z3, x2, y2, z2, x1, y1, z1, packed);
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose entry,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             int packed) {
        buffer.addVertex(entry, x1, y1, z1).setColor(packed);
        buffer.addVertex(entry, x2, y2, z2).setColor(packed);
        buffer.addVertex(entry, x3, y3, z3).setColor(packed);
        buffer.addVertex(entry, x4, y4, z4).setColor(packed);
    }

    private static void translateBillboard(PoseStack stack, Vec3 center) {
        Vec3 camPos = mc.getEntityRenderDispatcher().camera.position();
        // The world render pose already carries the view rotation, so translating by the
        // world delta and then applying Camera#rotation() (the *inverse* view rotation,
        // see Camera#getViewRotationMatrix) leaves the marker in a view-aligned, camera
        // facing frame - the same frame IQAddons gets from CameraRenderState#orientation.
        stack.translate(
                (float) (center.x - camPos.x),
                (float) (center.y - camPos.y),
                (float) (center.z - camPos.z)
        );
        stack.mulPose(cameraRotation());
    }

    private static Quaternionf cameraRotation() {
        return mc.getEntityRenderDispatcher().camera.rotation();
    }

    private static int packed(Color color) {
        return color.getRGB() | (color.getAlpha() << 24);
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose entry,
                             float x1, float y1, float x2, float y2, int color) {
        buffer.addVertex(entry, x1, y1, 0.0f).setColor(color).setLineWidth(LINE_WIDTH);
        buffer.addVertex(entry, x2, y2, 0.0f).setColor(color).setLineWidth(LINE_WIDTH);
    }

    private static void triangle(VertexConsumer buffer, PoseStack.Pose entry,
                                 float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        buffer.addVertex(entry, x1, y1, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(entry, x2, y2, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(entry, x3, y3, 0.0f).setColor(r, g, b, a);
    }
}
