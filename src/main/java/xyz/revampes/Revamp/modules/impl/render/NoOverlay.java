package xyz.revampes.Revamp.modules.impl.render;

import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.settings.impl.ButtonSetting;

public class NoOverlay extends Module {

    public ButtonSetting fire, water, inWall;

    public NoOverlay() {
        super("NoOverlay", "无视野遮挡", category.Render);

        this.registerSetting(fire = new ButtonSetting("Fire", "火", true));
        this.registerSetting(water = new ButtonSetting("Water", "水", true));
        this.registerSetting(inWall = new ButtonSetting("In Wall", "墙内", true));
    }
}
