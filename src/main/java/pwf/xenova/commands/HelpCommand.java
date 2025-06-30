package pwf.xenova.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;

public class HelpCommand implements CommandExecutor {

    public HelpCommand(PowerFly plugin) {
    }

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull[] args) {

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&8&m----§r §bPowerFly Help &8&m----"));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(""));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&e/fly &7- Enable fly for a limited time."));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&e/powerfly fly <player | all> <on | off> &7- Enable or disable fly to a player or all."));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&e/powerfly check <player> &7- Check fly time of a player."));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&e/powerfly addflytime <player | all> <seconds> &7- Add fly time to a player or all."));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&e/powerfly delflytime <player | all> <seconds> &7- Remove fly time from a player or all."));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&e/powerfly reload &7- Reload the plugin configuration."));
            return true;
        }

        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUnknown subcommand. Use &e/powerfly help&c."));
        return true;
    }
}
