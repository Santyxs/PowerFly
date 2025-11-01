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

import java.util.*;

public record DelFlyTimeCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NotNull [] args) {

        if (!sender.hasPermission("powerfly.delflytime")) {
            sender.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(plugin.getPrefixedMessage("no-player-specified", "&cYou must specify a player."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefixedMessage("no-time-specified", "&cYou must specify a time in seconds."));
            return true;
        }

        String targetName = args[0];
        String secondsStr = args[1];

        int secondsToRemove;
        try {
            secondsToRemove = Integer.parseInt(secondsStr.trim());
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

                plugin.getFlyTimeManager().delFlyTime(player.getUniqueId(), secondsToRemove);
                affected++;
            }

            String rawMessage = plugin.getMessages().getString("fly-time-deleted-all",
                    "&aRemoved &f{seconds}s &aof fly time from all players.");
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
        plugin.getFlyTimeManager().delFlyTime(uuid, secondsToRemove);

        String raw = plugin.getMessages().getString("fly-time-deleted",
                "&aRemoved &f{seconds}s &aof fly time from {player}.");
        String playerName = target.getName() != null ? target.getName() : targetName;
        raw = raw.replace("{player}", playerName)
                .replace("{seconds}", String.valueOf(secondsToRemove));

        String prefixedMessage = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r") + raw;
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefixedMessage));

        return true;
    }
}