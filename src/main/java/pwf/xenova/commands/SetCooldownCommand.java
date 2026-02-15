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

            var allowedWorlds = plugin.getConfig().getStringList("allowed-worlds");

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
            String messageKey = secondsToSet == -1 ? "set-cooldown-set-all" : "set-cooldown-all";
            String defaultMsg = secondsToSet == -1
                    ? "&aRemoved cooldown for &e{affected} &aplayers."
                    : "&aSet cooldown to &f{time} &afor &eall &aplayers.";

            String msg = plugin.getMessageString(messageKey, defaultMsg)
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

        if (secondsToSet == -1) {
            plugin.getCooldownFlyManager().removeCooldown(uuid);

            String msg = plugin.getMessageString("set-cooldown-removed", "&aRemoved cooldown for &e{player}&a.")
                    .replace("{player}", target.getName() != null ? target.getName() : targetName);
            sendWithPrefix(sender, msg);

            if (target.isOnline() && target.getPlayer() != null) {
                sendWithPrefix(target.getPlayer(), plugin.getMessageString("set-cooldown-removed-notify", "&aYour cooldown has been removed."));
            }
        } else {
            plugin.getCooldownFlyManager().setCooldown(uuid, secondsToSet);

            String timeDisplay = plugin.getFlyTimeManager().formatTime(secondsToSet);
            String msg = plugin.getMessageString("set-cooldown-player", "&aSet cooldown to &f{time} &afor &e{player}&a.")
                    .replace("{time}", timeDisplay)
                    .replace("{player}", target.getName() != null ? target.getName() : targetName);

            sendWithPrefix(sender, msg);
        }

        return true;
    }

    private void sendWithPrefix(CommandSender sender, String message) {
        String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        Component component = MessageFormat.parseMessageWithPrefix(prefix, message);
        sender.sendMessage(component);
    }
}