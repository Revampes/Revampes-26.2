package xyz.revampes.Revamp.commands.impl;

import xyz.revampes.Revamp.commands.Command;
import xyz.revampes.Revamp.commands.CommandManager;
import xyz.revampes.Revamp.modules.Module;
import xyz.revampes.Revamp.modules.ModuleManager;
import xyz.revampes.Revamp.utility.Utils;

public class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle", "Toggles a module", "t");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            sendError("Usage: " + CommandManager.PREFIX + "toggle <module>");
            return;
        }

        Module module = ModuleManager.getModuleByName(args[0]);
        if (module == null) {
            sendError("Module not found: " + args[0]);
            return;
        }

        module.toggle();
        Utils.addChatMessage(module.getName() + " " + (module.isEnabled() ? "§a§lENABLED" : "§c§lDISABLED"));
    }
}