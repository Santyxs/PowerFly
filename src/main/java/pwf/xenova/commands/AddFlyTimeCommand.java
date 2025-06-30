package pwf.xenova.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;

import java.util.UUID;

public class AddFlyTimeCommand implements CommandExecutor {

    private final PowerFly plugin;

    public AddFlyTimeCommand(PowerFly plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull [] args) {

        if (args.length != 3 || !args[0].equalsIgnoreCase("Addflytime")) {
            sender.sendMessage(plugin.getPrefixedMessage("invalid-arguments", "&cInvalid arguments. Usage: /powerfly Addflytime <player|all> <seconds>"));
            return true;
        }

        String targetName = args[1];
        String secondsStr = args[2];

        int secondsToRemove;
        try {
            secondsToRemove = Integer.parseInt(secondsStr);
            if (secondsToRemove <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getPrefixedMessage("invalid-time", "&cInvalid time."));
            return true;
        }

        if (targetName.equalsIgnoreCase("all")) {
            int affected = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!plugin.getConfig().getStringList("allowed-worlds").contains(player.getWorld().getName()))
                    continue;

                plugin.getFlyTimeManager().addFlyTime(player.getUniqueId(), secondsToRemove);
                affected++;
            }

            String rawMessage = plugin.getMessages().getString("fly-time-added-all",
                    "&aAdded {seconds}s of fly time to {players} players.");

            rawMessage = rawMessage.replace("{seconds}", String.valueOf(secondsToRemove))
                    .replace("{players}", String.valueOf(affected));

            String prefixedMessage = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r") + rawMessage;

            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefixedMessage));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getPrefixedMessage("player-not-found", "&cPlayer not found."));
            return true;
        }

        UUID uuid = target.getUniqueId();
        plugin.getFlyTimeManager().addFlyTime(uuid, secondsToRemove);

        String raw = plugin.getMessages().getString("fly-time-added", "&aAdded {seconds}s of fly time to {player}.");
        String playerName = target.getName() != null ? target.getName() : targetName;
        raw = raw.replace("{player}", playerName)
                .replace("{seconds}", String.valueOf(secondsToRemove));

        String prefixedMessage = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r") + raw;
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefixedMessage));

        return true;
    }
}
