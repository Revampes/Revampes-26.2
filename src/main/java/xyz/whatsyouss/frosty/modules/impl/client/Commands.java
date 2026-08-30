package xyz.whatsyouss.frosty.modules.impl.client;

import xyz.whatsyouss.frosty.modules.Module;

public class Commands extends Module {
    public Commands() {
        super("Commands", "指令", category.Client);
    }

    @Override
    public String getDesc() {
        if (UI.lang.getValue() == 1) {
            return "客户端指令 (.help)";
        }
        return "Client commands (.help)";
    }
}