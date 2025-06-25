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

        if (args.length != 2 || !args[0].equalsIgnoreCase("check")) {
            return false;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            Component msg = plugin.getPrefixedMessage("player-not-found", "&cPlayer not found.");
            sender.sendMessage(msg);
            return true;
        }

        int remainingSeconds = plugin.getFlyTimeManager().getRemainingFlyTime(target.getUniqueId());

        if (remainingSeconds <= 0) {
            String raw = plugin.getMessages().getString("no-fly-time-remaining", "&c{player} has no flight time remaining.")
                    .replace("{player}", Objects.requireNonNull(target.getName()));
            Component msg = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
            sender.sendMessage(msg);
            return true;
        }

        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;

        String raw = plugin.getMessages().getString("fly-time-remaining", "&a{player} has {minutes}m {seconds}s of flight time remaining.")
                .replace("{player}", Objects.requireNonNull(target.getName()))
                .replace("{minutes}", String.valueOf(minutes))
                .replace("{seconds}", String.valueOf(seconds));
        Component msg = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        sender.sendMessage(msg);

        return true;
    }
}
