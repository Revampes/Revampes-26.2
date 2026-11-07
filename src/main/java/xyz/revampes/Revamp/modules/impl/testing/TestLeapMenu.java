package xyz.revampes.Revamp.modules.impl.testing;

import com.mojang.blaze3d.platform.InputConstants;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import xyz.revampes.Revamp.events.impl.Render2DEvent;
import xyz.revampes.Revamp.modules.impl.dungeon.LeapMenu;
import xyz.revampes.Revamp.utility.dungeon.leap.LeapMenuRenderer;
import xyz.revampes.Revamp.utility.dungeon.leap.LeapTarget;
import xyz.revampes.Revamp.utility.dungeon.map.DungeonClass;

import java.util.List;

/**
 * Client-side preview of the leap menu for checking its layout without a dungeon chest.
 *
 * <p>Press F7 to toggle the preview. It supplies four targets to cover every available segment.
 */
public class TestLeapMenu extends LeapMenu {

    private static final List<LeapTarget> PREVIEW_TARGETS = List.of(
            new LeapTarget("ArcherTest", DungeonClass.Archer, false, -1),
            new LeapTarget("BerserkTest", DungeonClass.Berserk, false, -1),
            new LeapTarget("MageTest", DungeonClass.Mage, false, -1),
            new LeapTarget("TankTest", DungeonClass.Tank, false, -1)
    );

    public TestLeapMenu() {
        super("Test Leap Menu", category.Testing, InputConstants.KEY_F7);
    }

    @Override
    public AbstractContainerScreen<?> leapScreen() {
        return null;
    }

    @Override
    public void onEnable() {
        LeapMenuRenderer.reset();
    }

    @Override
    public void onDisable() {
        LeapMenuRenderer.reset();
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!isEnabled() || event.drawContext == null) return;

        LeapMenuRenderer.draw(this, event.drawContext, mc.font, PREVIEW_TARGETS,
                mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth(),
                mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());
    }
}
