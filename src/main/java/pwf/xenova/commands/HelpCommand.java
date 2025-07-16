package pwf.xenova.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;

public class HelpCommand implements CommandExecutor {

    private final PowerFly plugin;

    public HelpCommand(PowerFly plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NotNull [] args) {

        if (!sender.hasPermission("powerfly.help")) {
            sender.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&8&m----§r §bPowerFly Help &8&m----"));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(""));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&e/fly &7- Enable or disable fly for a limited time."));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&e/powerfly fly &7- Enable or disable fly for a limited time."));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&e/powerfly check <player> &7- Check fly time and cooldown of a player."));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&e/powerfly addflytime <player | all> <seconds> &7- Add fly time to a player or all."));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&e/powerfly delflytime <player | all> <seconds> &7- Remove fly time from a player or all."));
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&e/powerfly reload &7- Reload the plugin configuration."));
            return true;
        }

        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cUnknown subcommand. Use &e/powerfly help&c."));
        return true;
    }
}
