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

public class DelFlyTimeCommand implements CommandExecutor {

    private final PowerFly plugin;

    public DelFlyTimeCommand(PowerFly plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull [] args) {

        if (args.length != 3 || !args[0].equalsIgnoreCase("delflytime")) {
            sender.sendMessage(plugin.getPrefixedMessage("invalid-arguments", "&cUsage: /powerfly delflytime <player> <seconds>"));
            return true;
        }

        String playerName = args[1];
        String secondsStr = args[2];

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getPrefixedMessage("player-not-found", "&cPlayer not found."));
            return true;
        }

        int secondsToDel;
        try {
            secondsToDel = Integer.parseInt(secondsStr);
            if (secondsToDel <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getPrefixedMessage("invalid-time", "&cInvalid time."));
            return true;
        }

        UUID uuid = target.getUniqueId();
        plugin.getFlyTimeManager().delFlyTime(uuid, secondsToDel);

        String raw = plugin.getMessages().getString("fly-time-deleted", "&aDeleted {seconds}s of fly time to {player}.");
        raw = raw.replace("{player}", Objects.requireNonNull(target.getName()))
                .replace("{seconds}", String.valueOf(secondsToDel));

        Component msg = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r") + raw);
        sender.sendMessage(msg);

        return true;
    }
}
