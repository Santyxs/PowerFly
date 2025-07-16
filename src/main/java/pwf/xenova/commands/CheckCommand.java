package pwf.xenova.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;

import java.util.Objects;
import java.util.UUID;

public class CheckCommand implements CommandExecutor {

    private final PowerFly plugin;

    public CheckCommand(PowerFly plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull [] args) {

        if (!sender.hasPermission("powerfly.check")) {
            sender.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefixedMessage("no-player-specified", "&cYou must specify a player name."));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getPrefixedMessage("player-not-found", "&cPlayer not found."));
            return true;
        }

        UUID uuid = target.getUniqueId();
        String playerName = Objects.requireNonNullElse(target.getName(), args[1]);

        int flySeconds = plugin.getFlyTimeManager().getRemainingFlyTime(uuid);
        int cooldownSeconds = plugin.getCooldownFlyManager().getRemainingCooldownSeconds(uuid);

        int flyMinutes = flySeconds / 60;
        int flyRemSeconds = flySeconds % 60;

        int cooldownMinutes = cooldownSeconds / 60;
        int cooldownRemSeconds = cooldownSeconds % 60;

        String raw = plugin.getMessages().getString("check-info",
                """
                &8&m-&r &b{player} Info &8&m-
                
                &bTime fly: &7{fly_minutes}m {fly_seconds}s
                &bCooldown: &7{cooldown_minutes}m {cooldown_seconds}s"""
        );

        raw = raw.replace("{player}", playerName)
                .replace("{fly_minutes}", String.valueOf(flyMinutes))
                .replace("{fly_seconds}", String.valueOf(flyRemSeconds))
                .replace("{cooldown_minutes}", String.valueOf(cooldownMinutes))
                .replace("{cooldown_seconds}", String.valueOf(cooldownRemSeconds));

        Component msg = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        sender.sendMessage(msg);

        return true;
    }
}
