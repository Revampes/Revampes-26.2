package xyz.revampes.Revamp.modules.impl.render;

import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.settings.impl.SliderSetting;

public class Nametags extends Module {

    public SliderSetting scale;

    public Nametags() {
        super("Nametags", "名字标签", category.Render);

        this.registerSetting(scale = new SliderSetting("Scale", 1, 0.05, 5, 0.05, "大小"));
    }
}
