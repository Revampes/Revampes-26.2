package xyz.revampes.Revamp.modules.impl.movement;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import meteordevelopment.orbit.EventHandler;
import xyz.revampes.Revamp.events.impl.PreUpdateEvent;
import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.utility.BlockUtils;
import xyz.revampes.Revamp.utility.Utils;

public class Eagle extends Module {

    private boolean onEdge;

    public Eagle() {
        super("Eagle", "自动下蹲", category.Movement);
    }

    @Override
    public void onDisable() {
        if (!isPressingShift()) {
            mc.options.keyShift.setDown(false);
        }
        onEdge = false;
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!mc.player.onGround()) {
            return;
        }
        if (BlockUtils.isEdgeOfBlock()) {
            mc.options.keyShift.setDown(true);
            if (!onEdge) {
                onEdge = true;
            }
        } else {
            if (onEdge) {
                if (!isPressingShift()) {
                    mc.options.keyShift.setDown(false);
                }
                onEdge = false;
            }
        }
    }

    private boolean isPressingShift() {
        Window windowHandle = mc.getWindow();
        return InputConstants.isKeyDown(windowHandle, mc.options.keyShift.getDefaultKey().getValue());
    }
}
