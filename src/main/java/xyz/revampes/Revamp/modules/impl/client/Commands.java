package xyz.revampes.Revamp.modules.impl.client;

import xyz.revampes.Revamp.commands.CommandManager;
import xyz.revampes.Revamp.modules.Module;

public class Commands extends Module {
    public Commands() {
        super("Commands", "指令", category.Client);
    }

    @Override
    public String getDesc() {
        if (UI.lang.getValue() == 1) {
            return "客户端指令 (" + CommandManager.PREFIX + "help)";
        }
        return "Client commands (" + CommandManager.PREFIX + "help)";
    }
}