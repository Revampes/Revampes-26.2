package xyz.revampes.Revamp.modules.impl.client;

import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.settings.impl.ButtonSetting;
import xyz.revampes.Revamp.settings.impl.InputSetting;

public class Title extends Module {

    public ButtonSetting keepOriginal;

    public Title() {
        super("Title", "游戏标题", category.Client);

        this.registerSetting(keepOriginal = new ButtonSetting("Keep original", "保留原始", true));
    }
}