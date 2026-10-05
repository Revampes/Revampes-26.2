package xyz.whatsyouss.frosty.gui.component.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xyz.whatsyouss.frosty.gui.LiquidGlassStyle;
import xyz.whatsyouss.frosty.gui.component.Component;
import xyz.whatsyouss.frosty.modules.impl.client.UI;
import xyz.whatsyouss.frosty.settings.impl.ColorSetting;
import xyz.whatsyouss.frosty.utility.RenderUtils;

import java.awt.Color;

import static xyz.whatsyouss.frosty.Frosty.mc;

/**
 * Colour picker for a {@link ColorSetting}: hue bar, saturation/brightness square and
 * alpha bar. Collapsed it shows a swatch, clicking the swatch opens the picker.
 */
public class ColorComponent extends Component {

    private static final int SQUARE_HEIGHT = 48;
    private static final int BAR_HEIGHT = 8;
    private static final int GAP = 2;
    private static final int GRADIENT_STEPS = 96;
    public static final int PICKER_HEIGHT =
            GAP + SQUARE_HEIGHT + GAP + BAR_HEIGHT + GAP + BAR_HEIGHT + GAP;

    private final ColorSetting setting;
    private boolean clickConsumed;

    private float hue;
    private float saturation;
    private float brightness;
    private boolean draggingSquare;
    private boolean draggingHue;
    private boolean draggingAlpha;

    public ColorComponent(ColorSetting setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
        syncFromSetting();
    }

    /** Re-reads the setting when the value was changed somewhere else (e.g. config load). */
    public void syncFromSetting() {
        if (draggingSquare || draggingHue || draggingAlpha) return;

        float[] hsb = Color.RGBtoHSB(setting.getRed(), setting.getGreen(), setting.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) return;

        syncFromSetting();

        boolean isLight = UI.clickGuiColor.getValue() == 0;
        int textColor = LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor()
                : isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB();

        isHovered = mouseX >= x + width - 100 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height;

        context.text(mc.font, setting.getTransName(), (int) (x + 2), (int) (y + height / 2 - 4), textColor, false);

        float swatchX = x + width - 100;
        float swatchWidth = 90;

        if (LiquidGlassStyle.isEnabled()) {
            LiquidGlassStyle.drawControl(context, swatchX, y, swatchWidth, height, setting.isExpanded(), isHovered);
        } else {
            context.fill((int) swatchX, (int) y, (int) (swatchX + swatchWidth), (int) (y + height), 0xFF101010);
        }

        context.fill((int) swatchX + 2, (int) y + 2, (int) (swatchX + swatchWidth - 2), (int) (y + height - 2),
                setting.getRGB());
        RenderUtils.drawBorder(context, (int) swatchX + 2, (int) y + 2,
                (int) swatchWidth - 4, (int) height - 4, 0xFF000000);

        String hex = "#" + setting.getHex().substring(0, 6);
        context.text(mc.font, net.minecraft.network.chat.Component.literal(hex),
                (int) (swatchX + (swatchWidth - mc.font.width(hex)) / 2.0f),
                (int) (y + height / 2 - 4), contrastingColor(), false);

        if (setting.isExpanded()) {
            renderPicker(context);
        }
    }

    private void renderPicker(GuiGraphicsExtractor context) {
        float squareX = x + GAP;
        float squareY = y + height + GAP;
        float squareWidth = Math.max(20.0f, width - GAP * 2);

        context.fill((int) squareX - 1, (int) squareY - 1,
                (int) (squareX + squareWidth) + 1, (int) (squareY + SQUARE_HEIGHT) + 1, 0xFF000000);

        int pureHue = Color.HSBtoRGB(hue, 1.0f, 1.0f);
        context.fill((int) squareX, (int) squareY,
                (int) (squareX + squareWidth), (int) (squareY + SQUARE_HEIGHT), pureHue);

        for (int i = 0; i < GRADIENT_STEPS; i++) {
            float start = i / (float) GRADIENT_STEPS;
            float end = (i + 1) / (float) GRADIENT_STEPS;
            int alpha = Math.round((1.0f - (start + end) / 2.0f) * 255.0f);

            int x1 = Math.round(squareX + squareWidth * start);
            int x2 = Math.round(squareX + squareWidth * end);
            context.fill(x1, (int) squareY, x2, (int) (squareY + SQUARE_HEIGHT), (alpha << 24) | 0xFFFFFF);
        }

        for (int i = 0; i < GRADIENT_STEPS / 2; i++) {
            float start = i / (float) (GRADIENT_STEPS / 2);
            float end = (i + 1) / (float) (GRADIENT_STEPS / 2);
            int alpha = Math.round((start + end) / 2.0f * 255.0f);

            int y1 = Math.round(squareY + SQUARE_HEIGHT * start);
            int y2 = Math.round(squareY + SQUARE_HEIGHT * end);
            context.fill((int) squareX, y1, (int) (squareX + squareWidth), y2, (alpha << 24));
        }

        drawMarker(context, squareX + saturation * squareWidth, squareY + (1.0f - brightness) * SQUARE_HEIGHT);

        float barX = squareX;
        float hueY = squareY + SQUARE_HEIGHT + GAP;
        float alphaY = hueY + BAR_HEIGHT + GAP;

        for (int i = 0; i < GRADIENT_STEPS; i++) {
            float start = i / (float) GRADIENT_STEPS;
            float end = (i + 1) / (float) GRADIENT_STEPS;
            context.fill(Math.round(barX + squareWidth * start), (int) hueY,
                    Math.round(barX + squareWidth * end), (int) (hueY + BAR_HEIGHT),
                    0xFF000000 | Color.HSBtoRGB((start + end) / 2.0f, 1.0f, 1.0f));
        }

        drawMarker(context, barX + hue * squareWidth, hueY + BAR_HEIGHT / 2.0f);

        drawCheckerboard(context, barX, alphaY, squareWidth, BAR_HEIGHT);

        for (int i = 0; i < GRADIENT_STEPS; i++) {
            float start = i / (float) GRADIENT_STEPS;
            float end = (i + 1) / (float) GRADIENT_STEPS;
            context.fill(Math.round(barX + squareWidth * start), (int) alphaY,
                    Math.round(barX + squareWidth * end), (int) (alphaY + BAR_HEIGHT),
                    (Math.round(((start + end) / 2.0f) * 255.0f) << 24) | 0xFFFFFF);
        }

        drawMarker(context, barX + (setting.getAlpha() / 255.0f) * squareWidth, alphaY + BAR_HEIGHT / 2.0f);

        RenderUtils.drawBorder(context, (int) barX, (int) hueY, (int) squareWidth, BAR_HEIGHT + GAP + BAR_HEIGHT,
                LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.accentColor() : 0xFF000000);
    }

    private void drawCheckerboard(GuiGraphicsExtractor context, float x, float y, float width, float height) {
        int cell = 4;
        for (int row = 0; row < Math.ceil(height / cell); row++) {
            for (int column = 0; column < Math.ceil(width / cell); column++) {
                boolean dark = (row + column) % 2 == 0;
                float cellX = x + column * cell;
                float cellY = y + row * cell;

                int x1 = (int) cellX;
                int x2 = Math.min((int) (x + width), x1 + cell);
                int y1 = (int) cellY;
                int y2 = Math.min((int) (y + height), y1 + cell);
                if (x1 >= x2 || y1 >= y2) continue;

                int checker = dark ? 0xFF606060 : 0xFF9A9A9A;
                int alpha = (Math.round((cellX - x) / width * 255.0f) << 24) | 0xFFFFFF;
                context.fill(x1, y1, x2, y2, checker);
                context.fill(x1, y1, x2, y2, alpha);
            }
        }
    }

    private void drawMarker(GuiGraphicsExtractor context, float x, float y) {
        RenderUtils.drawBorder(context, (int) x - 3, (int) y - 3, 7, 7, 0xFF000000);
        RenderUtils.drawBorder(context, (int) x - 2, (int) y - 2, 5, 5, 0xFFFFFFFF);
    }

    private int contrastingColor() {
        double luminance = (0.299 * setting.getRed() + 0.587 * setting.getGreen() + 0.114 * setting.getBlue()) / 255.0;
        return luminance > 0.6 ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible()) return;

        clickConsumed = false;
        if (button != 0) return;

        float swatchX = x + width - 100;
        if (mouseX >= swatchX && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height) {
            setting.setExpanded(!setting.isExpanded());
            clickConsumed = true;
            return;
        }

        if (!setting.isExpanded()) return;

        if (insideSquare(mouseX, mouseY)) {
            draggingSquare = true;
            updateSquare(mouseX, mouseY);
            clickConsumed = true;
            return;
        }

        if (insideBar(mouseX, mouseY, hueBarY())) {
            draggingHue = true;
            updateHue(mouseX);
            clickConsumed = true;
            return;
        }

        if (insideBar(mouseX, mouseY, alphaBarY())) {
            draggingAlpha = true;
            updateAlpha(mouseX);
            clickConsumed = true;
            return;
        }

        if (mouseX >= pickerX() && mouseX <= pickerX() + pickerWidth()
                && mouseY >= y + height && mouseY <= y + height + PICKER_HEIGHT) {
            clickConsumed = true;
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!isVisible() || !setting.isExpanded()) return false;

        if (draggingSquare) {
            updateSquare(mouseX, mouseY);
            return true;
        }
        if (draggingHue) {
            updateHue(mouseX);
            return true;
        }
        if (draggingAlpha) {
            updateAlpha(mouseX);
            return true;
        }
        return false;
    }

    public void mouseReleased() {
        draggingSquare = false;
        draggingHue = false;
        draggingAlpha = false;
    }

    private void updateSquare(double mouseX, double mouseY) {
        float squareX = pickerX();
        float squareY = y + height + GAP;
        saturation = Math.clamp((float) ((mouseX - squareX) / pickerWidth()), 0.0f, 1.0f);
        brightness = Math.clamp(1.0f - (float) ((mouseY - squareY) / SQUARE_HEIGHT), 0.0f, 1.0f);
        applyHsv();
    }

    private void updateHue(double mouseX) {
        hue = Math.clamp((float) ((mouseX - pickerX()) / pickerWidth()), 0.0f, 1.0f);
        applyHsv();
    }

    private void updateAlpha(double mouseX) {
        setting.setAlpha(Math.round(Math.clamp((float) ((mouseX - pickerX()) / pickerWidth()), 0.0f, 1.0f) * 255.0f));
    }

    private void applyHsv() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        setting.setColor(new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, setting.getAlpha()));
    }

    private float pickerX() {
        return x + GAP;
    }

    private float pickerWidth() {
        return Math.max(20.0f, width - GAP * 2);
    }

    private float hueBarY() {
        return y + height + GAP + SQUARE_HEIGHT + GAP;
    }

    private float alphaBarY() {
        return hueBarY() + BAR_HEIGHT + GAP;
    }

    private boolean insideSquare(double mouseX, double mouseY) {
        return mouseX >= pickerX() && mouseX <= pickerX() + pickerWidth()
                && mouseY >= y + height + GAP && mouseY <= y + height + GAP + SQUARE_HEIGHT;
    }

    private boolean insideBar(double mouseX, double mouseY, float barY) {
        return mouseX >= pickerX() && mouseX <= pickerX() + pickerWidth()
                && mouseY >= barY - 2 && mouseY <= barY + BAR_HEIGHT + 2;
    }

    public boolean isClickConsumed() {
        return clickConsumed;
    }

    public boolean isExpanded() {
        return setting.isExpanded();
    }

    public int getPickerHeight() {
        return PICKER_HEIGHT;
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
