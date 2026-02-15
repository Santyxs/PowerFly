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
        ResetCommand resetCommand = new ResetCommand(powerFly);
        SetFlyTimeCommand setFlyTimeCommand = new SetFlyTimeCommand(powerFly);
        SetCooldownCommand setCooldownCommand = new SetCooldownCommand(powerFly);

        Objects.requireNonNull(plugin.getCommand("fly")).setExecutor(flyCommand);
        Objects.requireNonNull(plugin.getCommand("buyflytime")).setExecutor(buyFlyTimeCommand);
        Objects.requireNonNull(plugin.getCommand("powerfly")).setExecutor((sender, command, label, args) -> {
            if (args.length < 1) return false;

            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

            return switch (args[0].toLowerCase()) {
                case "help" -> helpCommand.onCommand(sender, command, label, subArgs);
                case "reload" -> reloadCommand.onCommand(sender, command, label, subArgs);
                case "fly" -> flyCommand.onCommand(sender, command, label, subArgs);
                case "check" -> checkCommand.onCommand(sender, command, label, subArgs);
                case "addflytime" -> addFlyTimeCommand.onCommand(sender, command, label, subArgs);
                case "delflytime" -> delFlyTimeCommand.onCommand(sender, command, label, subArgs);
                case "buyflytime" -> buyFlyTimeCommand.onCommand(sender, command, label, subArgs);
                case "reset" -> resetCommand.onCommand(sender, command, label, subArgs);
                case "setflytime" -> setFlyTimeCommand.onCommand(sender, command, label, subArgs);
                case "setcooldown" -> setCooldownCommand.onCommand(sender, command, label, subArgs);
                default -> false;
            };
        });

        Objects.requireNonNull(plugin.getCommand("powerfly")).setTabCompleter((sender, command, label, args) -> {
            if (args.length == 1) {
                List<String> subcommands = List.of(
                        "help", "reload", "fly", "check", "addflytime", "delflytime", "buyflytime", "reset", "setflytime", "setcooldown"
                );
                return StringUtil.copyPartialMatches(args[0], subcommands, new ArrayList<>());
            }

            if (args.length == 2) {
                String subcommand = args[0].toLowerCase();

                switch (subcommand) {
                    case "fly", "addflytime", "delflytime", "setflytime", "setcooldown" -> {
                        List<String> options = new ArrayList<>();
                        options.add("all");
                        Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                        return StringUtil.copyPartialMatches(args[1], options, new ArrayList<>());
                    }
                    case "reset" -> {
                        return StringUtil.copyPartialMatches(args[1], List.of("cooldown", "flytime"), new ArrayList<>());
                    }
                    case "check" -> {
                        List<String> options = new ArrayList<>();
                        Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                        return StringUtil.copyPartialMatches(args[1], options, new ArrayList<>());
                    }
                    case "buyflytime" -> {
                        return List.of("<seconds>");
                    }
                }

            }

            if (args.length == 3) {
                String subcommand = args[0].toLowerCase();

                switch (subcommand) {
                    case "fly" -> {
                        return StringUtil.copyPartialMatches(args[2], List.of("on", "off"), new ArrayList<>());
                    }
                    case "addflytime", "delflytime", "setflytime", "setcooldown" -> {
                        return StringUtil.copyPartialMatches(args[2], List.of("60", "300", "600", "3600", "-1"), new ArrayList<>());
                    }
                    case "reset" -> {
                        List<String> options = new ArrayList<>();
                        options.add("all");
                        Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                        return StringUtil.copyPartialMatches(args[2], options, new ArrayList<>());
                    }
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