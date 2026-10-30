package xyz.revampes.Revamp.gui.component.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xyz.revampes.Revamp.gui.LiquidGlassStyle;
import xyz.revampes.Revamp.gui.component.Component;
import xyz.revampes.Revamp.modules.impl.client.UI;

import java.awt.Color;

import static xyz.revampes.Revamp.Frosty.mc;

public class LabelComponent extends Component {

    private final String text;

    public LabelComponent(String text, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.text = text;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        boolean isLight = UI.clickGuiColor.getValue() == 0;
        int color = LiquidGlassStyle.isEnabled() ? LiquidGlassStyle.textColor()
                : isLight ? new Color(70, 70, 70).getRGB() : new Color(200, 200, 200).getRGB();

        context.fill((int) x, (int) (y + height / 2), (int) (x + width), (int) (y + height / 2 + 1),
                isLight ? 0x40000000 : 0x40FFFFFF);
        context.text(mc.font, text, (int) (x + 2), (int) (y + 1), color, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
