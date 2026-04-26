package pwf.xenova.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import pwf.xenova.utils.MessageFormat;
import pwf.xenova.PowerFly;

public record BuyFlyTimeCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NonNull [] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageFormat.parseMessage("&cOnly players can use this command."));
            return true;
        }

        if (!player.hasPermission("powerfly.buyflytime") && !player.hasPermission("powerfly.admin")) {
            sender.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        if (!plugin.getMainConfig().getBoolean("use-economy", false)) {
            player.sendMessage(MessageFormat.parseMessage("&cEconomy support is disabled. You cannot buy fly time."));
            return true;
        }

        if (plugin.getEconomy() == null) {
            player.sendMessage(MessageFormat.parseMessage("&cEconomy support is enabled but no provider was found. You cannot buy fly time."));
            plugin.getLogger().warning("use-economy is enabled but no economy provider was found.");
            return true;
        }

        if (args.length == 0) {
            sendWithPrefix(player, plugin.getMessageString("no-time-specified", "&cYou must specify a time in seconds."));
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[0].trim());
            if (seconds <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sendWithPrefix(player, plugin.getMessageString("invalid-time", "&cInvalid time."));
            return true;
        }

        double pricePerSecond = plugin.getMainConfig().getDouble("flytime-price", 100.0);
        double totalPrice = pricePerSecond * seconds;
        String currencySymbol = plugin.getMainConfig().getString("currency-symbol", "$");

        if (!plugin.getEconomy().has(player, totalPrice)) {
            String msg = plugin.getMessageString("not-enough-money", "&cYou need &e{price}{currency} &cto buy &f{secondstobuy}s &cof fly time.")
                    .replace("{price}", String.format("%.2f", totalPrice))
                    .replace("{currency}", currencySymbol)
                    .replace("{secondstobuy}", String.valueOf(seconds));

            sendWithPrefix(player, msg);
            return true;
        }

        plugin.getEconomy().withdrawPlayer(player, totalPrice);
        plugin.getFlyTimeManager().addFlyTime(player.getUniqueId(), seconds);

        String boughtMsg = plugin.getMessageString("flytime-bought", "&aYou bought &f{secondstobuy}s &aof fly time for &e{price}{currency}&a.")
                .replace("{secondstobuy}", String.valueOf(seconds))
                .replace("{price}", String.format("%.2f", totalPrice))
                .replace("{currency}", currencySymbol);

        sendWithPrefix(player, boughtMsg);
        return true;
    }

    private void sendWithPrefix(CommandSender sender, String message) {
        String prefix = plugin.getMainConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        Component component = MessageFormat.parseMessageWithPrefix(prefix, message);
        sender.sendMessage(component);
    }
}