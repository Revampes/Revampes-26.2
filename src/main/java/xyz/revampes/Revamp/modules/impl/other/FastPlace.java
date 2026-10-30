package xyz.revampes.Revamp.modules.impl.other;

import meteordevelopment.orbit.EventHandler;
import xyz.revampes.Revamp.events.impl.PreUpdateEvent;
import xyz.revampes.Revamp.mixin.accessor.MinecraftAccessor;
import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.settings.impl.SliderSetting;

public class FastPlace extends Module {

    private SliderSetting delay;

    public FastPlace() {
        super("FastPlace", "快速放置", category.Other);

        this.registerSetting(delay = new SliderSetting("Delay", 1, 0, 3, 1, "延迟"));
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (((MinecraftAccessor)mc).frosty$getRightClickDelay() > delay.getInput()) {
            ((MinecraftAccessor)mc).frosty$setRightClickDelay((int) delay.getInput());
        }
    }
}
