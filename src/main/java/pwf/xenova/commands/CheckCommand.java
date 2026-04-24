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

import java.util.UUID;
import java.util.function.Consumer;

public record CheckCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull [] args) {

        if (!sender.hasPermission("powerfly.check") && !sender.hasPermission("powerfly.admin")) {
            sender.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            if (sender instanceof Player player) {
                showInfo(sender, player.getUniqueId(), player.getName());
            } else {
                sendWithPrefix(sender, plugin.getMessageString("no-player-specified", "&cYou must specify a player name."));
            }
            return true;
        }

        Player online = Bukkit.getPlayerExact(args[0]);
        if (online != null) {
            showInfo(sender, online.getUniqueId(), online.getName());
            return true;
        }

        resolvePlayer(args[0],
                target -> showInfo(sender, target.getUniqueId(),
                        target.getName() != null ? target.getName() : args[0]),
                () -> sendWithPrefix(sender, plugin.getMessageString("player-not-found", "&cPlayer not found."))
        );

        return true;
    }

    private void showInfo(CommandSender sender, UUID uuid, String playerName) {
        int flySeconds = plugin.getFlyTimeManager().getRemainingFlyTime(uuid);
        String flyTimeDisplay;

        if (flySeconds < 0) {
            flyTimeDisplay = plugin.getMessageString("time-infinite", "∞");
        } else if (flySeconds == 0) {
            flyTimeDisplay = plugin.getMessageString("time-empty", "0s");
        } else {
            flyTimeDisplay = formatFlyTime(flySeconds);
        }

        String cooldownDisplay;
        if (plugin.getCooldownFlyManager().isOnCooldown(uuid)) {
            cooldownDisplay = "&7" + plugin.getCooldownFlyManager().getRemainingCooldownFormatted(uuid);
        } else {
            cooldownDisplay = plugin.getMessageString("cooldown-none", "&70s");
        }

        String raw = plugin.getMessageString("check-info",
                """
                &8&m-&r &b{player} Info &8&m-&r
                
                &bTime fly: &7{fly_time}
                &bCooldown: &7{cooldown_time}"""
        );

        raw = raw.replace("{player}", playerName)
                .replace("{fly_time}", flyTimeDisplay)
                .replace("{cooldown_time}", cooldownDisplay);

        sender.sendMessage(MessageFormat.parseMessage(raw));
    }

    private String formatFlyTime(int totalSeconds) {
        int d = totalSeconds / 86400;
        int h = (totalSeconds % 86400) / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0 || sb.isEmpty()) sb.append(s).append("s");
        return sb.toString().trim();
    }

    private void resolvePlayer(String name, Consumer<OfflinePlayer> onFound, Runnable onNotFound) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(name);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.hasPlayedBefore()) onFound.accept(target);
                else onNotFound.run();
            });
        });
    }

    private void sendWithPrefix(CommandSender sender, String message) {
        String prefix = plugin.getFileManager().getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
    }
}