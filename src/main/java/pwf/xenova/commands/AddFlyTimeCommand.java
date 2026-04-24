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

import java.util.function.Consumer;

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

            if (secondsToAdd == 0 || (secondsToAdd < 0 && secondsToAdd != -1)) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            sendWithPrefix(sender, plugin.getMessageString("invalid-time", "&cInvalid time."));
            return true;
        }

        if (targetName.equalsIgnoreCase("all")) {
            int affected = 0;

            var allowedWorlds = plugin.getConfig().getStringList("whitelist-worlds");

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
                    continue;
                }

                if (!plugin.getFlyTimeManager().hasInfiniteFlyTime(player.getUniqueId())) {
                    plugin.getFlyTimeManager().addFlyTime(player.getUniqueId(), secondsToAdd);
                    affected++;
                }
            }

            String timeDisplay = secondsToAdd == -1 ? "∞" : secondsToAdd + "s";
            String msg = plugin.getMessageString("fly-time-added-all", "&aAdded &f{seconds} &aof fly time to all players.")
                    .replace("{seconds}", timeDisplay)
                    .replace("{affected}", String.valueOf(affected));

            sendWithPrefix(sender, msg);
            return true;
        }

        final int finalSecondsToAdd = secondsToAdd;

        resolvePlayer(targetName,
                target -> {
                    if (plugin.getFlyTimeManager().hasInfiniteFlyTime(target.getUniqueId())) {
                        String msg = plugin.getMessageString("already-flytime-infinite", "&c{player} already has infinite fly time.")
                                .replace("{player}", target.getName() != null ? target.getName() : targetName);
                        sendWithPrefix(sender, msg);
                        return;
                    }

                    plugin.getFlyTimeManager().addFlyTime(target.getUniqueId(), finalSecondsToAdd);

                    String timeDisplay = finalSecondsToAdd == -1 ? "∞" : finalSecondsToAdd + "s";
                    String msg = plugin.getMessageString("fly-time-added", "&aAdded &f{seconds} &aof fly time to {player}.")
                            .replace("{seconds}", timeDisplay)
                            .replace("{player}", target.getName() != null ? target.getName() : targetName);

                    sendWithPrefix(sender, msg);
                },
                () -> sendWithPrefix(sender, plugin.getMessageString("player-not-found", "&cPlayer not found."))
        );

        return true;
    }

    private void resolvePlayer(String name, Consumer<OfflinePlayer> onFound, Runnable onNotFound) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            onFound.accept(online);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(name);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.hasPlayedBefore()) onFound.accept(target);
                else onNotFound.run();
            });
        });
    }

    private void sendWithPrefix(CommandSender sender, String message) {
        String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        Component component = MessageFormat.parseMessageWithPrefix(prefix, message);
        sender.sendMessage(component);
    }
}