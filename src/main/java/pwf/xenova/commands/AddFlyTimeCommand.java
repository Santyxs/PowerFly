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

public record AddFlyTimeCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NonNull [] args) {

        if (!sender.hasPermission("powerfly.addflytime") && !sender.hasPermission("powerfly.admin")) {
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

        int secondsToAdd;

        try {
            secondsToAdd = Integer.parseInt(secondsStr.trim());
            if (secondsToAdd <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sendWithPrefix(sender, plugin.getMessageString("invalid-time", "&cInvalid time."));
            return true;
        }

        if (targetName.equalsIgnoreCase("all")) {

            int affected = 0;

            for (Player player : Bukkit.getOnlinePlayers()) {

                if (!plugin.getConfig().getStringList("allowed-worlds")
                        .contains(player.getWorld().getName()))
                    continue;

                plugin.getFlyTimeManager().addFlyTime(player.getUniqueId(), secondsToAdd);
                affected++;
            }

            String msg = plugin.getMessageString("fly-time-added-all", "&aAdded &f{seconds}s &aof fly time to all players.")
                    .replace("{seconds}", String.valueOf(secondsToAdd));

            sendWithPrefix(sender, msg);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendWithPrefix(sender, plugin.getMessageString("player-not-found", "&cPlayer not found."));
            return true;
        }

        UUID uuid = target.getUniqueId();
        plugin.getFlyTimeManager().addFlyTime(uuid, secondsToAdd);

        String msg = plugin.getMessageString("fly-time-added", "&aAdded &f{seconds}s &aof fly time to {player}.")
                .replace("{seconds}", String.valueOf(secondsToAdd))
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
