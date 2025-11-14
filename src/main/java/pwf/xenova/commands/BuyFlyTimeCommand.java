package pwf.xenova.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.utils.MessageFormat;
import pwf.xenova.PowerFly;

public record BuyFlyTimeCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageFormat.parseMessage("&cOnly players can use this command."));
            return true;
        }

        if (!plugin.getConfig().getBoolean("use-economy", false)) {
            player.sendMessage(MessageFormat.parseMessage("&cEconomy support is disabled. You cannot buy fly time."));
            return true;
        }

        if (!player.hasPermission("powerfly.buyflytime") && !player.hasPermission("powerfly.admin")) {
            sender.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        int timeArgIndex = (args.length > 0 && args[0].equalsIgnoreCase("buyflytime")) ? 1 : 0;

        if (args.length <= timeArgIndex) {
            sendWithPrefix(player, "&cYou must specify a time in seconds.");
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[timeArgIndex].trim());
            if (seconds <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sendWithPrefix(player, "&cInvalid time.");
            return true;
        }

        double pricePerSecond = plugin.getConfig().getDouble("flytime-price", 100);
        double totalPrice = pricePerSecond * seconds;
        String currencySymbol = plugin.getConfig().getString("currency-symbol", "$");

        if (!plugin.getEconomy().has(player, totalPrice)) {
            String msg = "&cYou need &e" + String.format("%.2f", totalPrice) + currencySymbol +
                    " &cto buy &f" + seconds + "s &cof fly time.";
            sendWithPrefix(player, msg);
            return true;
        }

        plugin.getEconomy().withdrawPlayer(player, totalPrice);
        plugin.getFlyTimeManager().addFlyTime(player.getUniqueId(), seconds);

        String boughtMsg = "&aYou bought &f" + seconds + "s &aof fly time for &e" +
                String.format("%.2f", totalPrice) + currencySymbol + "&a.";
        sendWithPrefix(player, boughtMsg);

        return true;
    }

    private void sendWithPrefix(CommandSender sender, String message) {
        String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        Component component = MessageFormat.parseMessageWithPrefix(prefix, message);
        sender.sendMessage(component);
    }
}
