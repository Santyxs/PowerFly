package pwf.xenova.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.utils.MessageFormat;
import pwf.xenova.PowerFly;

import java.util.Objects;
import java.util.UUID;

public record CheckCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull [] args) {

        if (!sender.hasPermission("powerfly.check") && !sender.hasPermission("powerfly.admin")) {
            sender.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        OfflinePlayer target;

        if (args.length < 1) {
            if (sender instanceof Player player) {
                target = player;
            } else {
                sendWithPrefix(sender, plugin.getMessageString("no-player-specified", "&cYou must specify a player name."));
                return true;
            }
        } else {
            target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sendWithPrefix(sender, plugin.getMessageString("player-not-found", "&cPlayer not found or offline."));
                return true;
            }
        }

        UUID uuid = target.getUniqueId();
        String playerName = Objects.requireNonNullElse(target.getName(), args.length > 0 ? args[0] : "Unknown");

        int flySeconds = plugin.getFlyTimeManager().getRemainingFlyTime(uuid);
        String flyTimeDisplay;

        if (flySeconds < 0) {
            flyTimeDisplay = "∞";
        } else {
            int flyMinutes = flySeconds / 60;
            int flyRemSeconds = flySeconds % 60;
            flyTimeDisplay = flyMinutes + "m " + flyRemSeconds + "s";
        }

        int cooldownSeconds = plugin.getCooldownFlyManager().getRemainingCooldownSeconds(uuid);
        int cooldownMinutes = cooldownSeconds / 60;
        int cooldownRemSeconds = cooldownSeconds % 60;
        String cooldownDisplay = cooldownMinutes + "m " + cooldownRemSeconds + "s";

        String raw = plugin.getMessageString("check-info",
                """
                &8&m-&r &b{player} Info &8&m-
                
                &bTime fly: &7{fly_time}
                &bCooldown: &7{cooldown_time}"""
        );

        raw = raw.replace("{player}", playerName)
                .replace("{fly_time}", flyTimeDisplay)
                .replace("{cooldown_time}", cooldownDisplay);

        sender.sendMessage(MessageFormat.parseMessage(raw));
        return true;
    }

    private void sendWithPrefix(CommandSender sender, String message) {
        String prefix = plugin.getFileManager().getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
    }
}