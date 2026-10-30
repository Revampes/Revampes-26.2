package xyz.revampes.Revamp.modules.impl.render;

import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.settings.impl.ButtonSetting;

public class NoHudElement extends Module {

    public ButtonSetting scoreboard, bossBar, title;

    public NoHudElement() {
        super("NoHudElement", "无界面元素", category.Render);

        this.registerSetting(scoreboard = new ButtonSetting("Scoreboard", "计分板", true));
        this.registerSetting(bossBar = new ButtonSetting("Boss bar", "Boss血条", true));
        this.registerSetting(title = new ButtonSetting("Title", "标题", true));
    }
}
