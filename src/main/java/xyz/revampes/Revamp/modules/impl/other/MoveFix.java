package xyz.revampes.Revamp.modules.impl.other;

import meteordevelopment.orbit.EventHandler;
import xyz.revampes.Revamp.events.impl.PostSendMovementPacketsEvent;
import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.modules.ModuleManager;
import xyz.revampes.Revamp.modules.impl.client.UI;
import xyz.revampes.Revamp.settings.impl.ButtonSetting;
import xyz.revampes.Revamp.settings.impl.SelectSetting;
import xyz.revampes.Revamp.utility.Rotations;

public class MoveFix extends Module {

    private final String[] modes = new String[]{"Strict", "Silent"};
    private final String[] CNmodes = new String[]{"严格", "静默"};

    public SelectSetting mode;
    public ButtonSetting foragingOnly;

    public MoveFix() {
        super("MoveFix", "移动修正", category.Other);

        this.registerSetting(mode = new SelectSetting("Mode", "模式", 0, modes, CNmodes));
    }

    @Override
    public String getDesc() {
        if (UI.lang.getValue() == 1) {
            return "修正静默转向时的移动";
        }
        return "Correct movement while silent rotating";
    }

    public static boolean shouldApply() {
        return ModuleManager.moveFix.isEnabled() && Rotations.rotating;
    }

    public static boolean isSilent() {
        return ModuleManager.moveFix != null
                && ModuleManager.moveFix.mode != null
                && ModuleManager.moveFix.mode.getValue() == 1;
    }

    @EventHandler
    public void onPostSendMovementPacket(PostSendMovementPacketsEvent event) {
        if (shouldApply()) {
        }
    }
}
