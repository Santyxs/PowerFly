package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.util.StringUtil;
import pwf.xenova.PowerFly;
import pwf.xenova.commands.*;

import java.util.*;

public class CommandManager {

    private static List<String> getPlayerOptions() {
        List<String> options = new ArrayList<>();
        options.add("all");
        Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        return options;
    }

    private static List<String> getOnlinePlayers() {
        List<String> options = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        return options;
    }

    public static void registerCommands(PowerFly plugin) {
        if (!(plugin instanceof PowerFly powerFly)) {
            throw new IllegalArgumentException("Plugin must be an instance of PowerFly");
        }

        HelpCommand helpCommand = new HelpCommand(powerFly);
        ReloadCommand reloadCommand = new ReloadCommand(powerFly);
        FlyCommand flyCommand = new FlyCommand(powerFly);
        powerFly.setFlyCommand(flyCommand);
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
                case "help"        -> helpCommand.onCommand(sender, command, label, subArgs);
                case "reload"      -> reloadCommand.onCommand(sender, command, label, subArgs);
                case "fly"         -> flyCommand.onCommand(sender, command, label, subArgs);
                case "check"       -> checkCommand.onCommand(sender, command, label, subArgs);
                case "addflytime"  -> addFlyTimeCommand.onCommand(sender, command, label, subArgs);
                case "delflytime"  -> delFlyTimeCommand.onCommand(sender, command, label, subArgs);
                case "buyflytime"  -> buyFlyTimeCommand.onCommand(sender, command, label, subArgs);
                case "reset"       -> resetCommand.onCommand(sender, command, label, subArgs);
                case "setflytime"  -> setFlyTimeCommand.onCommand(sender, command, label, subArgs);
                case "setcooldown" -> setCooldownCommand.onCommand(sender, command, label, subArgs);
                default            -> false;
            };
        });

        Objects.requireNonNull(plugin.getCommand("powerfly")).setTabCompleter((sender, command, label, args) -> {
            if (args.length == 1) {
                List<String> subcommands = List.of("help", "reload", "fly", "check", "addflytime", "delflytime", "buyflytime", "reset", "setflytime", "setcooldown");
                return StringUtil.copyPartialMatches(args[0], subcommands, new ArrayList<>());
            }

            if (args.length == 2) {
                return switch (args[0].toLowerCase()) {
                    case "fly", "addflytime", "delflytime", "setflytime", "setcooldown" ->
                            StringUtil.copyPartialMatches(args[1], getPlayerOptions(), new ArrayList<>());
                    case "check" ->
                            StringUtil.copyPartialMatches(args[1], getOnlinePlayers(), new ArrayList<>());
                    case "reset" ->
                            StringUtil.copyPartialMatches(args[1], List.of("cooldown", "flytime"), new ArrayList<>());
                    case "buyflytime" ->
                            List.of("<seconds>");
                    default -> new ArrayList<>();
                };
            }

            if (args.length == 3) {
                return switch (args[0].toLowerCase()) {
                    case "fly" ->
                            StringUtil.copyPartialMatches(args[2], List.of("on", "off"), new ArrayList<>());
                    case "addflytime", "delflytime", "setflytime", "setcooldown" ->
                            StringUtil.copyPartialMatches(args[2], List.of("60", "300", "600", "3600", "-1"), new ArrayList<>());
                    case "reset" ->
                            StringUtil.copyPartialMatches(args[2], getPlayerOptions(), new ArrayList<>());
                    default -> new ArrayList<>();
                };
            }

            return new ArrayList<>();
        });

        Objects.requireNonNull(plugin.getCommand("fly")).setTabCompleter((sender, command, label, args) -> {
            if (args.length == 1) {
                return StringUtil.copyPartialMatches(args[0], getPlayerOptions(), new ArrayList<>());
            }
            if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1], List.of("on", "off"), new ArrayList<>());
            }
            return new ArrayList<>();
        });

        Objects.requireNonNull(plugin.getCommand("buyflytime")).setTabCompleter((sender, command, label, args) -> {
            if (args.length == 1) return List.of("<seconds>");
            return new ArrayList<>();
        });
    }
}