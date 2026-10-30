package xyz.revampes.Revamp.commands.impl;

import xyz.revampes.Revamp.commands.Command;
import xyz.revampes.Revamp.utility.Utils;

import static xyz.revampes.Revamp.Frosty.mc;

public class PosCommand extends Command {
    public PosCommand() {
        super("position", "Get the block pos under player", "pos");
    }

    @Override
    public void execute(String[] args) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (mc.player.blockPosition().below() != null) {
            String pos = Math.round(mc.player.blockPosition().below().getX()) + " " + Math.round(mc.player.blockPosition().below().getY()) + " " + Math.round(mc.player.blockPosition().below().getZ());
            Utils.addToClipboard(pos);
        }
    }
}
