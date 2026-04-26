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
import java.util.function.Consumer;

public record SetCooldownCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NonNull [] args) {

        if (!sender.hasPermission("powerfly.setcooldown") && !sender.hasPermission("powerfly.admin")) {
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
        } catch (NumberFormatException e) {
            sendWithPrefix(sender, plugin.getMessageString("invalid-time", "&cInvalid time."));
            return true;
        }

        if (targetName.equalsIgnoreCase("all")) {
            int affected = 0;

            var allowedWorlds = plugin.getMainConfig().getStringList("whitelist-worlds");

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
                    continue;
                }

                UUID uuid = player.getUniqueId();

                if (secondsToSet == -1) {
                    plugin.getCooldownFlyManager().removeCooldown(uuid);
                } else {
                    plugin.getCooldownFlyManager().setCooldown(uuid, secondsToSet);
                }

                affected++;
            }

            String timeDisplay = secondsToSet == -1 ? "removed" : plugin.getFlyTimeManager().formatTime(secondsToSet);
            String messageKey = secondsToSet == -1 ? "cooldown-reset-all" : "fly-cooldown-set-all";
            String defaultMsg = secondsToSet == -1
                    ? "&aRemoved cooldown for &e{affected} &aplayers."
                    : "&aSet cooldown time to &f{seconds} &afor &eall players.";

            String msg = plugin.getMessageString(messageKey, defaultMsg)
                    .replace("{seconds}", timeDisplay)
                    .replace("{affected}", String.valueOf(affected));

            sendWithPrefix(sender, msg);
            return true;
        }

        final int finalSecondsToSet = secondsToSet;

        resolvePlayer(targetName,
                target -> {
                    UUID uuid = target.getUniqueId();

                    if (finalSecondsToSet == -1) {
                        plugin.getCooldownFlyManager().removeCooldown(uuid);
                    } else {
                        plugin.getCooldownFlyManager().setCooldown(uuid, finalSecondsToSet);

                        String timeDisplay = plugin.getFlyTimeManager().formatTime(finalSecondsToSet);
                        String msg = plugin.getMessageString("fly-cooldown-set", "&aSet cooldown time to &f{seconds} &afor &e{player}.")
                                .replace("{seconds}", timeDisplay)
                                .replace("{player}", target.getName() != null ? target.getName() : targetName);

                        sendWithPrefix(sender, msg);
                    }
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
        String prefix = plugin.getMainConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        Component component = MessageFormat.parseMessageWithPrefix(prefix, message);
        sender.sendMessage(component);
    }
}