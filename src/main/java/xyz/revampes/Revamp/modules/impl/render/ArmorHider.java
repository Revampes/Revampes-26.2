package xyz.revampes.Revamp.modules.impl.render;

import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.settings.impl.ButtonSetting;

public class ArmorHider extends Module {

    public ButtonSetting head, selfOnly;

    public ArmorHider() {
        super("ArmorHider", "装备隐形", category.Render);

        this.registerSetting(head = new ButtonSetting("Head", "头部", true));
        this.registerSetting(selfOnly = new ButtonSetting("Self only", "只对自己生效", true));
    }
}