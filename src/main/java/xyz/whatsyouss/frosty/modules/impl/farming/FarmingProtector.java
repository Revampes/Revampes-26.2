package xyz.whatsyouss.frosty.modules.impl.farming;

import meteordevelopment.orbit.EventHandler;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class FarmingProtector extends Module {

    private String[] sensitivity = new String[]{"High", "Medium", "Low"};
    private ButtonSetting movement, bps, block, rotation, item, restart;
    private SelectSetting sensitive;

    private long rsTime = 0;
    public boolean rejoined;
    public static boolean stopped;
    public boolean canRestart;

    public FarmingProtector() {
        super("FarmingProtector", "农业保护", category.Farming);

        this.registerSetting(restart = new ButtonSetting("Restart", "自动重启", true));
        // this.registerSetting(movement = new ButtonSetting("Movement", true));
        // this.registerSetting(bps = new ButtonSetting("BPS", true));
        // this.registerSetting(sensitive = new SelectSetting("Sensitive", 1, sensitivity));
        // this.registerSetting(block = new ButtonSetting("Spawned block", true));
        // this.registerSetting(rotation = new ButtonSetting("Rotation", true));
        // this.registerSetting(item = new ButtonSetting("Item change", true));
    }

    @Override
    public void onEnable() {
        canRestart = false;
        stopped = false;
        rejoined = false;
        rsTime = 0;
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        FarmingMacro fm = ModuleManager.farmingMacro;

        String sidebar = Utils.stripColor(Utils.getScoreboardSidebarLines().toString().toLowerCase());
        boolean inGarden = sidebar.contains("the garden") || sidebar.contains("plot");

        if (!inGarden) {
            if (fm.running && !stopped) {
                stopMacro("§eWorld changed / Not in garden");
                stopped = true;
                rsTime = System.currentTimeMillis();
                return;
            }

            if (stopped && restart.isToggled()) {
                long now = System.currentTimeMillis();
                if (now - rsTime >= 3000) {
                    if (sidebar.contains("skyblock")) {
                        mc.getConnection().sendCommand("warp garden");
                        rsTime = now;
                    } else if (sidebar.contains("hypixel")) {
                        mc.getConnection().sendCommand("play sb");
                        rsTime = now;
                    }
                }
            }
        } else {
            if (stopped) {
                Utils.addModuleMessage(this.getName(), "§aRestarting FarmingMacro...");

                fm.startMacro(0);

                stopped = false;
                rejoined = false;
                canRestart = false;
                rsTime = 0;
            }
        }
    }

    private void stopMacro(String reason) {
        Utils.addModuleMessage(this.getName(), reason);

        FarmingMacro fm = ModuleManager.farmingMacro;
        if (fm != null && fm.isEnabled()) {
            fm.stopMacro();
            Utils.addModuleMessage(this.getName(), "§cFarmingMacro stopped");
        }

        PestCleaner pc = ModuleManager.pestCleaner;
        if (pc != null && pc.isEnabled()) {
            pc.disable();
        }
    }
}