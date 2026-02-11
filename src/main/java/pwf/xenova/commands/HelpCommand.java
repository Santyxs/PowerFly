package pwf.xenova.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.utils.MessageFormat;
import pwf.xenova.PowerFly;

public record HelpCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NotNull [] args) {

        if (!sender.hasPermission("powerfly.help") && !sender.hasPermission("powerfly.admin")) {
            sender.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {

            String raw = plugin.getMessageString("help-message",
                    """
                    &8&m----&r &bPowerFly Help &8&m----&r
                    
                    &e/fly &7- Enable or disable fly for a limited time.
                    &e/powerfly fly <player | all> <on | off> &7- Enable or disable fly for other players.
                    &e/powerfly buyflytime <seconds> &7- Buy fly time.
                    &e/powerfly check <player> &7- Check fly time and cooldown of a player.
                    &e/powerfly addflytime <player | all> <seconds> &7- Add fly time.
                    &e/powerfly delflytime <player | all> <seconds> &7- Remove fly time.
                    &e/powerfly reset <cooldown | flytime> <player | all> &7- Reset cooldown or fly time.
                    &e/powerfly reload &7- Reload the plugin configuration.
                    """);

            sender.sendMessage(MessageFormat.parseMessage(raw));
            return true;
        }

        sender.sendMessage(plugin.getPrefixedMessage("help-unknown", "&cUnknown subcommand. Use &e/powerfly help"));
        return true;
    }
}