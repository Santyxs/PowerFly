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

public class CheckCommand implements CommandExecutor {

    private final PowerFly plugin;

    public CheckCommand(PowerFly plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String[] args) {

        if (args.length == 0 || !args[0].equalsIgnoreCase("check")) {
            return false;
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

        int remainingSeconds = plugin.getFlyTimeManager().getRemainingFlyTime(target.getUniqueId());

        if (remainingSeconds <= 0) {
            String raw = plugin.getMessages().getString("no-fly-time-remaining", "&c{player} has no flight time remaining.")
                    .replace("{player}", Objects.requireNonNull(target.getName()));
            // Añadir prefijo manualmente para mensajes con variables
            String prefixedRaw = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r") + raw;
            Component msg = LegacyComponentSerializer.legacyAmpersand().deserialize(prefixedRaw);
            sender.sendMessage(msg);
            return true;
        }

        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;

        String raw = plugin.getMessages().getString("fly-time-remaining", "&a{player} has {minutes}m {seconds}s of flight time remaining.")
                .replace("{player}", Objects.requireNonNull(target.getName()))
                .replace("{minutes}", String.valueOf(minutes))
                .replace("{seconds}", String.valueOf(seconds));

        String prefixedRaw = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r") + raw;
        Component msg = LegacyComponentSerializer.legacyAmpersand().deserialize(prefixedRaw);
        sender.sendMessage(msg);

        return true;
    }
}
