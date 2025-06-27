package pwf.xenova.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;

import java.util.Objects;
import java.util.UUID;

public class AddFlyTimeCommand implements CommandExecutor {

    private final PowerFly plugin;

    public AddFlyTimeCommand(PowerFly plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull [] args) {

        if (args.length != 3 || !args[0].equalsIgnoreCase("addflytime")) {
            sender.sendMessage(plugin.getPrefixedMessage("invalid-arguments", "&cUsage: /powerfly addflytime <player> <seconds>"));
            return true;
        }

        String playerName = args[1];
        String secondsStr = args[2];

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getPrefixedMessage("player-not-found", "&cPlayer not found."));
            return true;
        }

        int secondsToAdd;
        try {
            secondsToAdd = Integer.parseInt(secondsStr);
            if (secondsToAdd <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getPrefixedMessage("invalid-time", "&cInvalid time."));
            return true;
        }

        UUID uuid = target.getUniqueId();
        plugin.getFlyTimeManager().addFlyTime(uuid, secondsToAdd);

        String raw = plugin.getMessages().getString("fly-time-added", "&aAdded {seconds}s of fly time to {player}.");
        raw = raw.replace("{player}", Objects.requireNonNull(target.getName()))
                .replace("{seconds}", String.valueOf(secondsToAdd));

        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r") + raw);
        sender.sendMessage(message);

        return true;
    }
}
