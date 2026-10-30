package xyz.revampes.Revamp.gui.component.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xyz.revampes.Revamp.gui.LiquidGlassStyle;
import xyz.revampes.Revamp.gui.component.Component;
import xyz.revampes.Revamp.modules.impl.client.UI;
import xyz.revampes.Revamp.settings.impl.MultiSelectSetting;

import java.awt.Color;

import static xyz.revampes.Revamp.Frosty.mc;

public class MultiSelectComponent extends Component {

    private final MultiSelectSetting setting;
    private boolean expanded;

    public MultiSelectComponent(MultiSelectSetting setting, float x, float y, float width, float height) {
        super(setting, x, y, width, height);
        this.setting = setting;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!isVisible()) return;

        boolean isLight = UI.clickGuiColor.getValue() == 0;
        isHovered = mouseX >= x + width - 100 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height;

        int textColor = LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor()
                : isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB();

        context.text(mc.font, setting.getName(), (int) (x + 2), (int) (y + height / 2 - 4), textColor, false);

        if (LiquidGlassStyle.isEnabled()) {
            LiquidGlassStyle.drawControl(context, x + width - 100, y, 90, height, expanded, isHovered);
        } else {
            context.fill((int) (x + width - 100), (int) y, (int) (x + width - 10), (int) (y + height),
                    isHovered ? new Color(200, 200, 200).getRGB() : new Color(180, 180, 180).getRGB());
        }

        String summary = setting.getSummary();
        context.text(mc.font, net.minecraft.network.chat.Component.literal(summary), (int) (x + width - 95), (int) (y + height / 2 - 4),
                LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor() : isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(),
                false);

        context.text(mc.font, net.minecraft.network.chat.Component.literal(expanded ? "\u25B2" : "\u25BC"), (int) (x + width - 20),
                (int) (y + height / 2 - 4), isLight ? Color.BLACK.getRGB() : Color.WHITE.getRGB(), false);

        if (!expanded) return;

        String[] options = setting.getOptions();
        int listHeight = options.length * (int) height;

        if (LiquidGlassStyle.isEnabled()) {
            LiquidGlassStyle.drawGlass(context, x + width - 100, y + height, 90, listHeight, 5,
                    isLight ? 0xD8EAF2FF : 0xD624334D);
        } else {
            context.fill((int) (x + width - 100), (int) (y + height),
                    (int) (x + width - 10), (int) (y + height + listHeight), 0xFF000000);
        }

        for (int i = 0; i < options.length; i++) {
            int optionY = (int) (y + height * (i + 1));
            boolean optionHovered = mouseX >= x + width - 100 && mouseX <= x + width - 10
                    && mouseY >= optionY && mouseY < optionY + height;

            if (LiquidGlassStyle.isEnabled()) {
                LiquidGlassStyle.drawControl(context, x + width - 98, optionY + 1, 86, height - 2,
                        setting.isSelected(i), optionHovered);
            } else {
                int color = setting.isSelected(i) ? 0xFF3355AA : optionHovered ? 0xFF4444AA : 0xFF333333;
                context.fill((int) (x + width - 100), optionY, (int) (x + width - 10), optionY + (int) height, color);
            }

            String label = (setting.isSelected(i) ? "\u2714 " : "  ") + options[i];
            context.text(mc.font, net.minecraft.network.chat.Component.literal(label), (int) (x + width - 95),
                    optionY + (int) (height / 2 - 4),
                    LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor() : 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible() || button != 0) return;

        if (mouseX >= x + width - 100 && mouseX <= x + width - 10 && mouseY >= y && mouseY <= y + height) {
            expanded = !expanded;
            return;
        }

        if (!expanded) return;

        String[] options = setting.getOptions();
        for (int i = 0; i < options.length; i++) {
            int optionY = (int) (y + height * (i + 1));
            if (mouseX >= x + width - 100 && mouseX <= x + width - 10
                    && mouseY >= optionY && mouseY < optionY + height) {
                setting.toggle(i);
                return;
            }
        }
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}
