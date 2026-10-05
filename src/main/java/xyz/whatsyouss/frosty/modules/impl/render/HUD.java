package xyz.whatsyouss.frosty.modules.impl.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;
import xyz.whatsyouss.frosty.hud.HudManager;
import xyz.whatsyouss.frosty.hud.HudWidget;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Theme;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HUD extends Module {

    private SelectSetting color;
    private ButtonSetting flow, suffix, bar, background;
    private SliderSetting opacity;

    private String[] colors = new String[] {"Rainbow", "Cherry", "Cotton candy", "Flare", "Flower", "Gold", "Grayscale", "Royal", "Sky", "Vine"};
    private String[] CNcolors = new String[] {"彩虹", "粉樱", "棉花糖", "炽焰", "繁花", "流金", "灰阶", "皇室蓝", "晴空", "青藤"};

    private static final int INFO_COLOR = 0xFFA0A0A0;
    private static final int MINIMUM_WIDTH = 60;
    private int strColor;

    public HUD() {
        super("HUD", "界面", Module.category.Render);

        this.registerSetting(color = new SelectSetting("Color", "颜色", 0, colors, CNcolors));
        this.registerSetting(flow = new ButtonSetting("Gradient", "渐变", false));
        this.registerSetting(suffix = new ButtonSetting("Suffix", "尾标", true));
        this.registerSetting(bar = new ButtonSetting("Bar", "尾块", true));
        this.registerSetting(background = new ButtonSetting("Background", "背景", true));
        this.registerSetting(opacity = new SliderSetting("Opacity", "%", 50, 0, 100, 1, "透明度"));

        HudManager.register(new ModuleListWidget());
    }

    @Override
    public void guiUpdate() {
        this.opacity.setVisibilityCondition(() -> background.isToggled());
    }

    @Override
    public void onUpdate() {
        strColor = getCurrentColor(0);
    }

    int getCurrentColor(int moduleIndex) {
        int selectedIndex = (int) color.getValue();
        Theme theme = Theme.values()[selectedIndex];

        if (flow.isToggled()) {
            double offset = moduleIndex * 0.1;
            double speed = 0.001;

            if (theme == Theme.Rainbow) {
                long delay = moduleIndex * 50L;
                return Theme.getChroma(2, delay);
            } else {
                return theme.getAnimatedColor(offset, 255, speed);
            }
        } else {
            if (theme == Theme.Rainbow) {
                return Theme.getChroma(2, 0);
            } else {
                return theme.getAnimatedColor(0, 255, 0.001);
            }
        }
    }

    private boolean showSuffix() {
        return suffix.isToggled();
    }

    private String displayText(Module module) {
        String name = module.getNameInHud();
        String info = module.getInfo();
        return name + (info.isEmpty() || !showSuffix() ? "" : " " + info);
    }

    private List<Module> sortedModules() {
        return ModuleManager.organizedModules.stream()
                .filter(module -> module.isEnabled() && !module.isHidden())
                .sorted(Comparator.comparingInt((Module module) -> mc.font.width(displayText(module))).reversed())
                .collect(Collectors.toList());
    }

    /**
     * The enabled-module list. Draggable like every other widget; the lines stay right
     * aligned inside the element bounds so the classic corner layout is preserved.
     */
    public class ModuleListWidget extends HudWidget {

        public ModuleListWidget() {
            super("moduleList", "Enabled Modules", 0.0f, 5.0f, 1.0f);
        }

        @Override
        public void applyDefaultPosition(Font font) {
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            setPosition(Math.max(0.0f, screenWidth - getUnscaledWidth(font) - 5.0f), 5.0f);
            setScale(1.0f);
        }

        @Override
        public int getUnscaledWidth(Font font) {
            return Math.max(MINIMUM_WIDTH, super.getUnscaledWidth(font));
        }

        @Override
        public boolean isVisible() {
            return isEnabled() && Utils.nullCheck() && !mc.gui.hud.isHidden();
        }

        @Override
        public List<String> lines() {
            List<String> result = new ArrayList<>();
            for (Module module : sortedModules()) {
                result.add(displayText(module));
            }
            return result;
        }

        @Override
        public void tick() {
            strColor = getCurrentColor(0);
        }

        @Override
        public void render(GuiGraphicsExtractor context, Font font, float alpha) {
            List<Module> sortedModules = sortedModules();
            if (sortedModules.isEmpty()) return;

            boolean displaySuffix = showSuffix();
            boolean displayBar = bar.isToggled();
            boolean displayBackground = background.isToggled();

            int widgetWidth = getUnscaledWidth(font);
            float rightEdge = Math.min(getX() + getWidth(font), mc.getWindow().getGuiScaledWidth() - 5.0f);
            int extraOffset = displayBar ? 2 : 0;

            Matrix3x2fStack matrices = context.pose();
            matrices.pushMatrix();
            matrices.translate(rightEdge, getY());
            matrices.scale(getScale(), getScale());

            int yPos = 0;
            for (int i = 0; i < sortedModules.size(); i++) {
                Module module = sortedModules.get(i);
                String moduleName = module.getNameInHud();
                String info = module.getInfo();
                boolean hasInfo = !info.isEmpty() && displaySuffix;

                int totalWidth = font.width(moduleName + (hasInfo ? " " + info : ""));
                int xPos = widgetWidth - totalWidth;

                int moduleColor = flow.isToggled() ? getCurrentColor(i) : strColor;

                if (displayBackground) {
                    int alphaValue = (int) (25 + (opacity.getInput() / 100.0) * (125 - 25));
                    context.fill(xPos - 2, yPos - 1, widgetWidth + extraOffset + 2,
                            yPos + font.lineHeight + 1, (alphaValue << 24) | 0x000000);
                }

                context.text(font, Component.literal(moduleName), xPos, yPos, moduleColor, true);

                if (hasInfo) {
                    int nameWidth = font.width(moduleName);
                    context.text(font, Component.literal(" " + info), xPos + nameWidth, yPos, INFO_COLOR, true);
                }

                yPos += font.lineHeight + 2;
            }

            if (displayBar) {
                yPos = 0;
                for (int i = 0; i < sortedModules.size(); i++) {
                    int moduleColor = flow.isToggled() ? getCurrentColor(i) : strColor;
                    context.fill(widgetWidth, yPos - 1, widgetWidth + extraOffset, yPos + font.lineHeight + 1, moduleColor);
                    yPos += font.lineHeight + 2;
                }
            }

            matrices.popMatrix();
        }
    }
}
