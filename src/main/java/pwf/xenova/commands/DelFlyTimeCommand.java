package pwf.xenova.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import pwf.xenova.utils.MessageFormat;
import pwf.xenova.PowerFly;
import java.util.UUID;

public record DelFlyTimeCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NonNull [] args) {

        if (!sender.hasPermission("powerfly.delflytime") && !sender.hasPermission("powerfly.admin")) {
            sender.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            sendWithPrefix(sender, plugin.getMessageString("no-player-specified", "&cYou must specify a player."));
            return true;
        }

        if (args.length < 2) {
            sendWithPrefix(sender, plugin.getMessageString("no-time-specified", "&cYou must specify a time in seconds."));
            return true;
        }

        String targetName = args[0];
        String secondsStr = args[1];

        int secondsToRemove;

        try {
            secondsToRemove = Integer.parseInt(secondsStr.trim());
            if (secondsToRemove == 0 || (secondsToRemove < 0 && secondsToRemove != -1)) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            sendWithPrefix(sender, plugin.getMessageString("invalid-time", "&cInvalid time."));
            return true;
        }

        if (targetName.equalsIgnoreCase("all")) {

            int affected = 0;

            var allowedWorlds = plugin.getConfig().getStringList("allowed-worlds");

            for (Player player : Bukkit.getOnlinePlayers()) {

                if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
                    continue;
                }

                if (secondsToRemove == -1) {
                    if (plugin.getFlyTimeManager().hasInfiniteFlyTime(player.getUniqueId())) {
                        plugin.getFlyTimeManager().setFlyTime(player.getUniqueId(), 0);
                        affected++;
                    }
                } else {
                    plugin.getFlyTimeManager().delFlyTime(player.getUniqueId(), secondsToRemove);
                    affected++;
                }
            }

            String timeDisplay = secondsToRemove == -1 ? "∞" : secondsToRemove + "s";
            String msg = plugin.getMessageString("fly-time-deleted-all", "&aRemoved &f{seconds} &afrom all players.")
                    .replace("{seconds}", timeDisplay)
                    .replace("{affected}", String.valueOf(affected));

            sendWithPrefix(sender, msg);

            plugin.getLogger().info("Removed " + timeDisplay + " from " + affected + " players");

            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendWithPrefix(sender, plugin.getMessageString("player-not-found", "&cPlayer not found."));
            return true;
        }

        UUID uuid = target.getUniqueId();

        if (secondsToRemove == -1) {
            if (plugin.getFlyTimeManager().hasInfiniteFlyTime(uuid)) {
                plugin.getFlyTimeManager().setFlyTime(uuid, 1);
            } else {
                sendWithPrefix(sender, "&cPlayer does not have infinite fly time.");
                return true;
            }
        } else {
            plugin.getFlyTimeManager().delFlyTime(uuid, secondsToRemove);
        }

        String timeDisplay = secondsToRemove == -1 ? "∞" : secondsToRemove + "s";
        String msg = plugin.getMessageString("fly-time-deleted", "&aRemoved &f{seconds} &aof fly time from {player}.")
                .replace("{seconds}", timeDisplay)
                .replace("{player}", target.getName() != null ? target.getName() : targetName);
        sendWithPrefix(sender, msg);

        return true;
    }

    private void sendWithPrefix(CommandSender sender, String message) {
        String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        Component component = MessageFormat.parseMessageWithPrefix(prefix, message);
        sender.sendMessage(component);
    }
}
