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

public record ResetCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NonNull [] args) {

        if (!sender.hasPermission("powerfly.reset") && !sender.hasPermission("powerfly.admin")) {
            sender.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            sendWithPrefix(sender, plugin.getMessageString("no-reset-type-specified", "&cYou must specify 'cooldown' or 'flytime'."));
            return true;
        }

        if (args.length < 2) {
            sendWithPrefix(sender, plugin.getMessageString("no-player-specified", "&cYou must specify a player."));
            return true;
        }

        String type = args[0].toLowerCase();
        String targetName = args[1];

        if (targetName.equalsIgnoreCase("all")) {
            resetAll(sender, type);
            return true;
        }

        resetPlayer(sender, type, targetName);
        return true;
    }

    private void resetAll(CommandSender sender, String type) {
        int affected = 0;

        var allowedWorlds = plugin.getConfig().getStringList("allowed-worlds");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
                continue;
            }

            UUID uuid = player.getUniqueId();

            if (type.equals("cooldown")) {
                plugin.getCooldownFlyManager().removeCooldown(uuid);
            } else if (type.equals("flytime")) {
                plugin.getFlyTimeManager().setFlyTime(uuid, 0);

                if (player.isFlying() || player.getAllowFlight()) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }

            affected++;
        }

        String messageKey = type.equals("cooldown") ? "cooldown-reset-all" : "fly-time-reset-all";
        String defaultMsg = type.equals("cooldown")
                ? "&aReset &fcooldown &cfor &eall players."
                : "&aReset &ffly time &cfor &eall players.";

        String msg = plugin.getMessageString(messageKey, defaultMsg)
                .replace("{affected}", String.valueOf(affected));

        sendWithPrefix(sender, msg);

        plugin.getLogger().info("Reset " + type + " for " + affected + " players");
    }

    private void resetPlayer(CommandSender sender, String type, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sendWithPrefix(sender, plugin.getMessageString("player-not-found", "&cPlayer not found."));
            return;
        }

        UUID uuid = target.getUniqueId();

        if (type.equals("cooldown")) {
            plugin.getCooldownFlyManager().removeCooldown(uuid);
        } else if (type.equals("flytime")) {
            plugin.getFlyTimeManager().setFlyTime(uuid, 0);

            if (target.isOnline() && target.getPlayer() != null) {
                Player player = target.getPlayer();
                if (player.isFlying() || player.getAllowFlight()) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
        }

        String messageKey = type.equals("cooldown") ? "cooldown-reset" : "fly-time-reset";
        String defaultMsg = type.equals("cooldown")
                ? "&aReset &fcooldown &afor &e{player}."
                : "&aReset &ffly time &afor &e{player}.";

        String msg = plugin.getMessageString(messageKey, defaultMsg)
                .replace("{player}", target.getName() != null ? target.getName() : targetName);

        sendWithPrefix(sender, msg);
    }

    private void sendWithPrefix(CommandSender sender, String message) {
        String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        Component component = MessageFormat.parseMessageWithPrefix(prefix, message);
        sender.sendMessage(component);
    }
}
