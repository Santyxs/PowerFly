package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.util.StringUtil;
import pwf.xenova.commands.*;
import pwf.xenova.PowerFly;

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

        HelpCommand helpCommand = new HelpCommand(plugin);
        ReloadCommand reloadCommand = new ReloadCommand(plugin);
        FlyCommand flyCommand = new FlyCommand(plugin); plugin.setFlyCommand(flyCommand);
        CheckCommand checkCommand = new CheckCommand(plugin);
        AddFlyTimeCommand addFlyTimeCommand = new AddFlyTimeCommand(plugin);
        DelFlyTimeCommand delFlyTimeCommand = new DelFlyTimeCommand(plugin);
        BuyFlyTimeCommand buyFlyTimeCommand = new BuyFlyTimeCommand(plugin);
        ResetCommand resetCommand = new ResetCommand(plugin);
        SetFlyTimeCommand setFlyTimeCommand = new SetFlyTimeCommand(plugin);
        SetCooldownCommand setCooldownCommand = new SetCooldownCommand(plugin);

        PluginCommand flycmd = plugin.getCommand("fly");
        if (flycmd == null) throw new IllegalStateException("Command 'fly' not found — make sure it's defined in plugin.yml.");
        flycmd.setExecutor(flyCommand);
        flycmd.setTabCompleter((sender, command, label, args) -> {
            if (args.length == 1) {
                return StringUtil.copyPartialMatches(args[0], getPlayerOptions(), new ArrayList<>());
            }
            if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1], List.of("on", "off"), new ArrayList<>());
            }
            return new ArrayList<>();
        });

        PluginCommand buycmd = plugin.getCommand("buyflytime");
        if (buycmd == null) throw new IllegalStateException("Command 'buyflytime' not found — make sure it's defined in plugin.yml.");
        buycmd.setExecutor(buyFlyTimeCommand);
        buycmd.setTabCompleter((sender, command, label, args) -> {
            if (args.length == 1) return List.of("<seconds>");
            return new ArrayList<>();
        });

        PluginCommand pwfcmd = plugin.getCommand("powerfly");
        if (pwfcmd == null) throw new IllegalStateException("Command 'powerfly' not found — make sure it's defined in plugin.yml.");

        pwfcmd.setExecutor((sender, command, label, args) -> {
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

        pwfcmd.setTabCompleter((sender, command, label, args) -> {
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
    }
}