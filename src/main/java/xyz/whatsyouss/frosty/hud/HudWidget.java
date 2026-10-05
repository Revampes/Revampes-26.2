package xyz.whatsyouss.frosty.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;
import xyz.whatsyouss.frosty.utility.StringUtils;

import java.util.List;

/**
 * A screen element that can be positioned by dragging it in the HUD editor.
 *
 * <p>{@code x}/{@code y} are the top-left corner in GUI scaled coordinates. Widgets only
 * report their lines and visibility; the {@link HudManager} handles layout and input.
 */
public abstract class HudWidget {

    protected static final Minecraft mc = Minecraft.getInstance();

    private final String id;
    private final String name;
    private final float defaultX;
    private final float defaultY;
    private final float defaultScale;

    private float x;
    private float y;
    private float scale;
    private boolean positionInitialized;

    protected HudWidget(String id, String name, float defaultX, float defaultY, float defaultScale) {
        this.id = id;
        this.name = name;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.defaultScale = defaultScale;
        this.x = defaultX;
        this.y = defaultY;
        this.scale = defaultScale;
    }

    public final String getId() {
        return id;
    }

    public final String getName() {
        return name;
    }

    public final float getX() {
        return x;
    }

    public final float getY() {
        return y;
    }

    public final void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.positionInitialized = true;
    }

    public final boolean isPositionInitialized() {
        return positionInitialized;
    }

    public final float getScale() {
        return scale;
    }

    public final void setScale(float scale) {
        this.scale = scale <= 0.0f ? defaultScale : scale;
    }

    /** Applied when the layout file has no entry for this widget yet. */
    public void applyDefaultPosition(Font font) {
        this.x = defaultX;
        this.y = defaultY;
        this.scale = defaultScale;
        this.positionInitialized = true;
    }

    /** Whether the widget currently wants to be drawn outside of the HUD editor. */
    public boolean isVisible() {
        return true;
    }

    /** The lines to draw, top to bottom. Legacy {@code §} colour codes are supported. */
    public abstract List<String> lines();

    /**
     * Preview the HUD editor shows for a widget that has nothing live to display (IQAddons'
     * {@code setExampleLines}), so context-only widgets stay selectable and draggable.
     */
    protected List<String> exampleLines() {
        return List.of(getName());
    }

    /** Lines to draw in the editor when nothing is live, so every widget stays draggable. */
    public final List<String> editorLines() {
        List<String> live = lines();
        if (isVisible() && !live.isEmpty()) return live;
        return exampleLines();
    }

    /** Called every client tick while the widget is registered. */
    public void tick() {
    }

    public int getUnscaledWidth(Font font) {
        int width = 0;
        for (String line : editorLines()) {
            width = Math.max(width, font.width(StringUtils.stripFormatting(line)));
        }
        return width;
    }

    public int getUnscaledHeight(Font font) {
        return Math.max(1, editorLines().size()) * (font.lineHeight + 2);
    }

    public float getWidth(Font font) {
        return getUnscaledWidth(font) * scale;
    }

    public float getHeight(Font font) {
        return getUnscaledHeight(font) * scale;
    }

    public void render(GuiGraphicsExtractor context, Font font, float alpha) {
        drawLines(context, font, lines(), 0xFFFFFFFF);
    }

    /** Draws the widget itself, or its preview lines when it has nothing live to show. */
    public void renderForEditor(GuiGraphicsExtractor context, Font font) {
        if (isVisible()) {
            render(context, font, 1.0f);
            return;
        }

        drawLines(context, font, exampleLines(), 0x99FFFFFF);
    }

    private void drawLines(GuiGraphicsExtractor context, Font font, List<String> lines, int color) {
        if (lines.isEmpty()) return;

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(getX(), getY());
        matrices.scale(scale, scale);

        int lineY = 0;
        for (String line : lines) {
            context.text(font, Component.literal(line), 0, lineY, color, true);
            lineY += font.lineHeight + 2;
        }

        matrices.popMatrix();
    }
}
