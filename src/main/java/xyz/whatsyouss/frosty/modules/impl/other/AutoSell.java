package xyz.whatsyouss.frosty.modules.impl.other;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

import java.util.Arrays;
import java.util.List;

public class AutoSell extends Module {

    private final List<String> defaultItems = Arrays.asList(
            "enchanted ice", "superboom tnt", "rotten", "skeleton master", "skeleton grunt", "cutlass",
            "skeleton lord", "skeleton soldier", "zombie soldier", "zombie knight", "zombie commander", "zombie lord",
            "skeletor", "super heavy", "heavy", "sniper helmet", "dreadlord", "earth shard", "zombie commander whip",
            "machine gun", "sniper bow", "soulstealer bow", "silent death", "training weight",
            "beating heart", "premium flesh", "mimic fragment", "enchanted rotten flesh", "sign",
            "enchanted bone", "defuse kit", "optical lens", "tripwire hook", "button", "carpet", "lever", "diamond atom",
            "healing viii splash potion", "healing 8 splash potion", "candycomb", "rune", "flaming chestplate", "bouncy",
            "revive stone", "earthen blade"
    );

    private String[] types = new String[]{"Left", "Right", "Middle"};
//    private String[] CNtypes = new String[]{"左键", "右键", "中键"};

    private final SliderSetting clickIntervals = new SliderSetting("Click Intervals (ticks)", 5, 0, 40, 1);
    private final SelectSetting clickMethod = new SelectSetting("Click Method", 2, types);

    private int tickCounter = 0;

    public AutoSell() {
        super("AutoSell", "自动出售", category.Other);

        this.registerSetting(clickIntervals);
        this.registerSetting(clickMethod);

    }

    private boolean isValidContainer() {
        if (mc.gui.screen() instanceof ContainerScreen genericContainerScreen) {
            String title = genericContainerScreen.getTitle().getString().toLowerCase();
            return title.contains("trades") || title.contains("booster cookie") || title.contains("farm merchant") || title.contains("ophelia");
        }
        return false;
    }
}