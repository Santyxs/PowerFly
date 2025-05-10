package pwf.xenova.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;

public class HelpCommand implements CommandExecutor {

    public HelpCommand(PowerFly plugin) {
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§8§m----§r §bPowerFly Help §8§m----");
            sender.sendMessage("");
            sender.sendMessage("§e/fly §7- Enable flying for a limited time.");
            sender.sendMessage("§e/powerfly addtime <player> <seconds> §7- Add flight time to a player.");
            sender.sendMessage("§e/powerfly removetime <player> <seconds> §7- Remove flight time from a player.");
            sender.sendMessage("§e/powerfly reload §7- Reload the plugin configuration.");
            return true;
        }

        sender.sendMessage("§cUnknown subcommand. Use §e/powerfly help§c.");
        return true;
    }
}
