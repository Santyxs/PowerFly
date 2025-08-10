package pwf.xenova.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;

public record BuyFlyTimeCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("&cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("powerfly.buyflytime")) {
            player.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        int timeArgIndex = (args.length > 0 && args[0].equalsIgnoreCase("buyflytime")) ? 1 : 0;

        if (args.length <= timeArgIndex) {
            player.sendMessage(plugin.getPrefixedMessage("no-time-specified", "&cYou must specify a time in seconds."));
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[timeArgIndex].trim());
            if (seconds <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getPrefixedMessage("invalid-time", "&cInvalid time."));
            return true;
        }

        double pricePerSecond = plugin.getConfig().getDouble("flytime-price", 100);
        double totalPrice = pricePerSecond * seconds;
        String currencySymbol = plugin.getConfig().getString("currency-symbol", "$");

        if (plugin.getEconomy() == null) {
            String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
            String combined = prefix + "&cEconomy plugin not found. Cannot buy fly time.";
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(combined));
            return true;
        }

        if (!plugin.getEconomy().has(player, totalPrice)) {
            String msg = LegacyComponentSerializer.legacyAmpersand()
                    .serialize(plugin.getPrefixedMessage(
                            "not-enough-money",
                            "&cYou need &e{price}{currency} &cto buy &f{secondstobuy}s &cof fly time."
                    ))
                    .replace("{price}", String.format("%.2f", totalPrice))
                    .replace("{currency}", currencySymbol)
                    .replace("{secondstobuy}", String.valueOf(seconds));

            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
            return true;
        }

        plugin.getEconomy().withdrawPlayer(player, totalPrice);
        plugin.getFlyTimeManager().addFlyTime(player.getUniqueId(), seconds);

        String boughtMsg = LegacyComponentSerializer.legacyAmpersand()
                .serialize(plugin.getPrefixedMessage(
                        "flytime-bought",
                        "&aYou bought &f{secondstobuy}s &aof fly time for &e{price}{currency}."
                ))
                .replace("{price}", String.format("%.2f", totalPrice))
                .replace("{currency}", currencySymbol)
                .replace("{secondstobuy}", String.valueOf(seconds));

        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(boughtMsg));

        return true;
    }
}
