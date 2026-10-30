package xyz.revampes.Revamp.commands.impl;

import net.minecraft.network.chat.Component;
import xyz.revampes.Revamp.commands.Command;
import xyz.revampes.Revamp.commands.CommandManager;
import xyz.revampes.Revamp.utility.Utils;

import static xyz.revampes.Revamp.Frosty.mc;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Help info", "h");
    }

    @Override
    public void execute(String[] args) {
        if (!Utils.nullCheck()) {
            return;
        }
        Utils.addChatMessage("==============================");
        for (Command c : CommandManager.getCommands()) {
            mc.player.sendSystemMessage(Component.literal("§b" + CommandManager.PREFIX + c.getName()
                    + "  §7---  " + c.getDescription() + " - (§b" + CommandManager.PREFIX + c.getAliases() + "§7)"));
        }
        Utils.addChatMessage("==============================");
    }
}
