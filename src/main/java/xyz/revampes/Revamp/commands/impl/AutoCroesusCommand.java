// package xyz.revampes.Revamp.commands.impl;

// import xyz.revampes.Revamp.commands.Command;
// import xyz.revampes.Revamp.commands.CommandManager;
// import xyz.revampes.Revamp.modules.ModuleManager;
// import xyz.revampes.Revamp.modules.impl.dungeon.AutoCroesus;
// import xyz.revampes.Revamp.utility.dungeon.croesus.CroesusData;

// import java.util.List;
// import java.util.Locale;

// public class AutoCroesusCommand extends Command {

//     public AutoCroesusCommand() {
//         super("autocroesus", "Start and configure Auto Croesus", "ac");
//     }

//     @Override
//     public void execute(String[] args) {
//         if (args.length < 1) {
//             sendUsage();
//             return;
//         }

//         switch (args[0].toLowerCase(Locale.ROOT)) {
//             case "go" -> ModuleManager.autoCroesus.start();
//             case "forcego" -> ModuleManager.autoCroesus.start(false);
//             case "alwaysbuy" -> modify(args, CroesusData.getAlwaysBuy(), CroesusData::saveAlwaysBuy, "Always Buy");
//             case "worthless" -> modify(args, CroesusData.getWorthless(), CroesusData::saveWorthless, "Worthless");
//             default -> sendUsage();
//         }
//     }

//     private void modify(String[] args, List<String> ids, Runnable save, String label) {
//         if (args.length < 2) {
//             AutoCroesus.modMessage(label + ": " + ids);
//             return;
//         }

//         String id = args[1].toUpperCase(Locale.ROOT);
//         if (id.equals("RESET")) {
//             ids.clear();
//             save.run();
//             AutoCroesus.modMessage("Cleared the " + label.toLowerCase(Locale.ROOT) + " list");
//             return;
//         }

//         if (ids.contains(id)) {
//             ids.remove(id);
//             save.run();
//             AutoCroesus.modMessage("Removed " + id + " from " + label.toLowerCase(Locale.ROOT));
//             return;
//         }

//         ids.add(id);
//         save.run();
//         AutoCroesus.modMessage("Added " + id + " to " + label.toLowerCase(Locale.ROOT));
//     }

//     private void sendUsage() {
//         AutoCroesus.modMessage("Usage: " + CommandManager.PREFIX
//                 + "ac <go|forcego|alwaysbuy|worthless> [sbId|reset]");
//     }
// }
