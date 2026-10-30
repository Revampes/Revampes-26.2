package xyz.revampes.Revamp.commands;

import java.util.ArrayList;
import java.util.List;

import xyz.revampes.Revamp.commands.impl.BindCommand;
import xyz.revampes.Revamp.commands.impl.FarmingCommand;
import xyz.revampes.Revamp.commands.impl.HelpCommand;
import xyz.revampes.Revamp.commands.impl.ItemCommand;
import xyz.revampes.Revamp.commands.impl.PosCommand;
import xyz.revampes.Revamp.commands.impl.ToggleCommand;

public class CommandManager {
    public static final String PREFIX = ";";
    public static List<Command> commands = new ArrayList<>();

    public static BindCommand bindCommand;
    public static HelpCommand helpCommand;
    public static ItemCommand itemCommand;
    public static PosCommand posCommand;
    public static ToggleCommand toggleCommand;
    public static FarmingCommand farmingCommand;
    // public static AutoCroesusCommand autoCroesusCommand;

    public void register() {
        this.addCommand(bindCommand = new BindCommand());
        this.addCommand(helpCommand = new HelpCommand());
        this.addCommand(itemCommand = new ItemCommand());
        this.addCommand(posCommand = new PosCommand());
        this.addCommand(toggleCommand = new ToggleCommand());
        this.addCommand(farmingCommand = new FarmingCommand());
        // this.addCommand(autoCroesusCommand = new AutoCroesusCommand());
    }

    public void addCommand(Command m) {
        commands.add(m);
    }

    public static List<Command> getCommands() {
        return commands;
    }

    public static Command getCommandByName(String input) {
        for (Command c : commands) {
            if (c.getName().equalsIgnoreCase(input) || c.getAliases().equalsIgnoreCase(input)) {
                return c;
            }
        }
        return null;
    }

}
