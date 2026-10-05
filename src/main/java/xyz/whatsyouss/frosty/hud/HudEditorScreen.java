package xyz.whatsyouss.frosty.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.utility.RenderUtils;

import java.awt.Color;

import static xyz.whatsyouss.frosty.Frosty.mc;

/**
 * Drag-and-drop layout editor for every registered {@link HudWidget}.
 *
 * <p>There is deliberately no coordinate input: the player grabs an element and moves it,
 * the layout is written back on release.
 */
public class HudEditorScreen extends Screen {

    private static final int HIGHLIGHT = 0x6022D3EE;
    private static final int BORDER = 0xFF22D3EE;
    private static final int HOVERED_BORDER = 0xFFFFD166;

    private HudWidget dragging;
    private float grabOffsetX;
    private float grabOffsetY;

    public HudEditorScreen() {
        super(Component.literal("HUD Editor"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int width = this.width;
        int height = this.height;

        context.fill(0, 0, width, height, new Color(0, 0, 0, 120).getRGB());

        HudWidget hovered = dragging != null ? dragging : HudManager.topmostAt(mouseX, mouseY, mc.font);

        for (HudWidget widget : HudManager.widgets()) {
            widget.renderForEditor(context, mc.font);

            float x = widget.getX();
            float y = widget.getY();
            float widgetWidth = Math.max(12.0f, widget.getWidth(mc.font) + 4.0f);
            float widgetHeight = Math.max(10.0f, widget.getHeight(mc.font) + 4.0f);

            context.fill((int) (x - 2), (int) (y - 2), (int) (x + widgetWidth), (int) (y + widgetHeight), HIGHLIGHT);
            RenderUtils.drawBorder(context, (int) (x - 2), (int) (y - 2),
                    (int) widgetWidth + 2, (int) widgetHeight + 2,
                    widget == hovered ? HOVERED_BORDER : BORDER);

            context.text(mc.font, Component.literal(widget.getName()), (int) (x - 2), (int) (y + widgetHeight + 1),
                    widget == hovered ? HOVERED_BORDER : BORDER, true);
        }

        String title = "Edit HUD";
        context.text(mc.font, Component.literal(title), (width - mc.font.width(title)) / 2, 8, 0xFFFFFFFF, true);

        String hint = "Drag an element to move it - Scroll to resize - Esc to save and go back";
        context.text(mc.font, Component.literal(hint), (width - mc.font.width(hint)) / 2, height - 18,
                0xFFB0B0B0, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        HudWidget widget = HudManager.topmostAt(click.x(), click.y(), mc.font);
        if (widget == null) {
            dragging = null;
            return super.mouseClicked(click, doubled);
        }

        dragging = widget;
        grabOffsetX = (float) click.x() - widget.getX();
        grabOffsetY = (float) click.y() - widget.getY();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (dragging == null) return super.mouseDragged(click, offsetX, offsetY);

        float x = (float) click.x() - grabOffsetX;
        float y = (float) click.y() - grabOffsetY;

        float widgetWidth = Math.max(12.0f, dragging.getWidth(mc.font) + 4.0f);
        float widgetHeight = Math.max(10.0f, dragging.getHeight(mc.font) + 4.0f);

        x = Math.clamp(x, -2.0f, Math.max(-2.0f, this.width - widgetWidth));
        y = Math.clamp(y, -2.0f, Math.max(-2.0f, this.height - widgetHeight));

        dragging.setPosition(x, y);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging != null) {
            dragging = null;
            HudManager.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        HudWidget widget = HudManager.topmostAt(mouseX, mouseY, mc.font);
        if (widget != null && verticalAmount != 0.0) {
            widget.setScale(Math.clamp(widget.getScale() + (float) verticalAmount * 0.05f, 0.5f, 3.0f));
            HudManager.save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == 256) {
            HudManager.save();
            ModuleManager.ui.enable();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
