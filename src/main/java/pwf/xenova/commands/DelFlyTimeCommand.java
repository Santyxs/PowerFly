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
        int amount;

        try {
            amount = Integer.parseInt(args[1].trim());
        } catch (NumberFormatException e) {
            sendWithPrefix(sender, plugin.getMessageString("invalid-time", "&cInvalid time."));
            return true;
        }

        if (targetName.equalsIgnoreCase("all")) {

            int affected = 0;

            var allowedWorlds = plugin.getConfig().getStringList("allowed-worlds");

            for (Player player : Bukkit.getOnlinePlayers()) {
                processReduction(player.getUniqueId(), amount);
                refreshPlayer(player);
                affected++;
            }
            sendFeedback(sender, "all", amount, affected);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendWithPrefix(sender, plugin.getMessageString("player-not-found", "&cPlayer not found."));
            return true;
        }

        processReduction(target.getUniqueId(), amount);

        Player onlinePlayer = target.getPlayer();
        if (onlinePlayer != null) {
            refreshPlayer(onlinePlayer);
        }

        sendFeedback(sender, target.getName() != null ? target.getName() : targetName, amount, 1);
        return true;
    }

    private void processReduction(UUID uuid, int amount) {
        int current = plugin.getFlyTimeManager().getRemainingFlyTime(uuid);

        if (amount == -1) {
            plugin.getFlyTimeManager().setFlyTime(uuid, 1);
            return;
        }

        if (current == -1) return;

        int result = current - amount;
        if (result < 1) {
            result = 1;
        }

        plugin.getFlyTimeManager().setFlyTime(uuid, result);
    }

    private void refreshPlayer(Player player) {
        if (FlyCommand.hasPluginFlyActive(player.getUniqueId())) {
            int newTime = plugin.getFlyTimeManager().getRemainingFlyTime(player.getUniqueId());
            new FlyCommand(plugin).restartFlyTimer(player, newTime);
        }
    }

    private void sendFeedback(CommandSender sender, String target, int amount, int affected) {
        String display = (amount == -1) ? "∞" : amount + "s";
        String msg = (target.equalsIgnoreCase("all"))
                ? "&aRemoved &f" + display + " &afrom all online players."
                : "&aRemoved &f" + display + " &aof fly time from &e" + target + ".";
        sendWithPrefix(sender, msg);
    }

    private void sendWithPrefix(CommandSender sender, String message) {
        String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        Component component = MessageFormat.parseMessageWithPrefix(prefix, message);
        sender.sendMessage(component);
    }
}