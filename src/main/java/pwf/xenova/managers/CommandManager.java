package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.util.StringUtil;
import pwf.xenova.PowerFly;
import pwf.xenova.commands.*;

import java.util.*;

public class CommandManager {

    public static void registerCommands(PowerFly plugin) {
        if (!(plugin instanceof PowerFly powerFly)) {
            throw new IllegalArgumentException("Plugin must be an instance of PowerFly");
        }

        HelpCommand helpCommand = new HelpCommand(powerFly);
        ReloadCommand reloadCommand = new ReloadCommand(powerFly);
        FlyCommand flyCommand = new FlyCommand(powerFly);
        CheckCommand checkCommand = new CheckCommand(powerFly);
        AddFlyTimeCommand addFlyTimeCommand = new AddFlyTimeCommand(powerFly);
        DelFlyTimeCommand delFlyTimeCommand = new DelFlyTimeCommand(powerFly);
        BuyFlyTimeCommand buyFlyTimeCommand = new BuyFlyTimeCommand(powerFly);

        Objects.requireNonNull(plugin.getCommand("fly")).setExecutor(flyCommand);
        Objects.requireNonNull(plugin.getCommand("buyflytime")).setExecutor(buyFlyTimeCommand);
        Objects.requireNonNull(plugin.getCommand("powerfly")).setExecutor((sender, command, label, args) -> {
            if (args.length < 1) return false;

            return switch (args[0].toLowerCase()) {
                case "help" -> helpCommand.onCommand(sender, command, label, args);
                case "reload" -> reloadCommand.onCommand(sender, command, label, args);
                case "fly" -> flyCommand.onCommand(sender, command, label, args);
                case "check" -> checkCommand.onCommand(sender, command, label, args);
                case "addflytime" -> addFlyTimeCommand.onCommand(sender, command, label, args);
                case "delflytime" -> delFlyTimeCommand.onCommand(sender, command, label, args);
                case "buyflytime" -> buyFlyTimeCommand.onCommand(sender, command, label, args);
                default -> false;
            };
        });

        Objects.requireNonNull(plugin.getCommand("powerfly")).setTabCompleter((sender, command, label, args) -> {
            if (args.length == 1) {
                List<String> subcommands = List.of(
                        "help", "reload", "fly", "check", "addflytime", "delflytime", "buyflytime"
                );
                return StringUtil.copyPartialMatches(args[0], subcommands, new ArrayList<>());
            }

            if (args.length == 2) {
                String subcommand = args[0].toLowerCase();

                if (List.of("fly", "addflytime", "delflytime").contains(subcommand)) {
                    List<String> options = new ArrayList<>();
                    options.add("all");
                    Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                    return StringUtil.copyPartialMatches(args[1], options, new ArrayList<>());
                }

                if ("check".equals(subcommand)) {
                    List<String> options = new ArrayList<>();
                    Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                    return StringUtil.copyPartialMatches(args[1], options, new ArrayList<>());
                }

                if ("buyflytime".equals(subcommand)) {
                    return List.of("<seconds>");
                }
            }

            if (args.length == 3) {
                String subcommand = args[0].toLowerCase();

                if ("fly".equals(subcommand)) {
                    return StringUtil.copyPartialMatches(args[2], List.of("on", "off"), new ArrayList<>());
                }

                if ("addflytime".equals(subcommand) || "delflytime".equals(subcommand)) {
                    return List.of("<seconds>");
                }
            }

            return new ArrayList<>();
        });

        Objects.requireNonNull(plugin.getCommand("fly")).setTabCompleter((sender, command, label, args) -> {
            if (args.length == 1) {
                List<String> options = new ArrayList<>();
                options.add("all");
                Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>());
            }

            if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1], List.of("on", "off"), new ArrayList<>());
            }

            return new ArrayList<>();
        });

        Objects.requireNonNull(plugin.getCommand("buyflytime")).setTabCompleter((sender, command, label, args) -> {
            if (args.length == 1) {
                return List.of("<seconds>");
            }
            return new ArrayList<>();
        });
    }
}
