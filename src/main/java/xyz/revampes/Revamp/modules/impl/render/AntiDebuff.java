package xyz.revampes.Revamp.modules.impl.render;

import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.settings.impl.ButtonSetting;

public class AntiDebuff extends Module {

    public ButtonSetting nausea;

    public AntiDebuff() {
        super("AntiDebuff", "防负面效果", category.Render);

        this.registerSetting(nausea = new ButtonSetting("Nausea", "反胃", true));
    }
}
