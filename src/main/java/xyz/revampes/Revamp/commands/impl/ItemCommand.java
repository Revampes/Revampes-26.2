package xyz.revampes.Revamp.commands.impl;

import xyz.revampes.Revamp.commands.Command;
import xyz.revampes.Revamp.utility.Utils;


import static xyz.revampes.Revamp.Frosty.mc;

public class ItemCommand extends Command {
    public ItemCommand() {
        super("item", "Get item info", "i");
    }

    @Override
    public void execute(String[] args) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (mc.player.getMainHandItem() != null && mc.player.getMainHandItem().getCustomName() != null) {
            String itemName = Utils.getLiteral(mc.player.getMainHandItem().getCustomName().toString());
            Utils.addToClipboard(itemName);
        }
    }
}