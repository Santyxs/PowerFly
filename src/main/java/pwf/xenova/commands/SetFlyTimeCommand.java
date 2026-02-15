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
import pwf.xenova.PowerFly;
import pwf.xenova.utils.MessageFormat;

import java.util.UUID;

public record SetFlyTimeCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NonNull [] args) {

        if (!sender.hasPermission("powerfly.setflytime") && !sender.hasPermission("powerfly.admin")) {
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

        int secondsToSet;

        try {
            secondsToSet = Integer.parseInt(secondsStr.trim());

            if (secondsToSet < -1) {
                throw new NumberFormatException();
            }

            if (secondsToSet == 0) {
                secondsToSet = 1;
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

                plugin.getFlyTimeManager().setFlyTime(player.getUniqueId(), secondsToSet);
                affected++;
            }

            String timeDisplay = secondsToSet == -1 ? "∞" : plugin.getFlyTimeManager().formatTime(secondsToSet);
            String msg = plugin.getMessageString("set-flytime-all", "&aSet fly time to &f{seconds} &afor &eall players.")
                    .replace("{time}", timeDisplay)
                    .replace("{affected}", String.valueOf(affected));

            sendWithPrefix(sender, msg);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendWithPrefix(sender, plugin.getMessageString("player-not-found", "&cPlayer not found."));
            return true;
        }

        UUID uuid = target.getUniqueId();
        plugin.getFlyTimeManager().setFlyTime(uuid, secondsToSet);

        String timeDisplay = secondsToSet == -1 ? "∞" : plugin.getFlyTimeManager().formatTime(secondsToSet);
        String msg = plugin.getMessageString("set-flytime-player", "&aSet fly time to &f{time} &afor &e{player}&a.")
                .replace("{time}", timeDisplay)
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