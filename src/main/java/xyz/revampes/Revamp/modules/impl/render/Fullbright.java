package xyz.revampes.Revamp.modules.impl.render;

import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.settings.impl.SelectSetting;

public class Fullbright extends Module {

    private SelectSetting mode;

    private String[] modes = new String[]{"Gamma"};
    private String[] CNmodes = new String[]{"伽马值"};

    public int selectedMode;

    public Fullbright() {
        super("Fullbright", "夜视", category.Render);

        this.registerSetting(mode = new SelectSetting("Mode", "模式", 0, modes, CNmodes));
    }

    @Override
    public void onEnable() {
        selectedMode = (int) mode.getValue();
    }
}
