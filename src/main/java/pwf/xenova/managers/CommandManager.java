package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import pwf.xenova.PowerFly;
import pwf.xenova.commands.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandManager {

    public static void registerCommands(JavaPlugin plugin) {
        if (!(plugin instanceof PowerFly powerFly)) {
            throw new IllegalArgumentException();
        }

        FlyCommand flyCommand = new FlyCommand(powerFly);
        CheckCommand checkCommand = new CheckCommand(powerFly);
        AddFlyTimeCommand addFlyTimeCommand = new AddFlyTimeCommand(powerFly);
        DelFlyTimeCommand delFlyTimeCommand = new DelFlyTimeCommand(powerFly);
        ReloadCommand reloadCommand = new ReloadCommand(powerFly);
        HelpCommand helpCommand = new HelpCommand(powerFly);

        Objects.requireNonNull(plugin.getCommand("fly")).setExecutor(flyCommand);

        Objects.requireNonNull(plugin.getCommand("powerfly")).setExecutor((sender, command, label, args) -> {
            if (args.length < 1) return false;

            return switch (args[0].toLowerCase()) {
                case "fly" -> flyCommand.onCommand(sender, command, label, args);
                case "check" -> checkCommand.onCommand(sender, command, label, args);
                case "addflytime" -> addFlyTimeCommand.onCommand(sender, command, label, args);
                case "delflytime" -> delFlyTimeCommand.onCommand(sender, command, label, args);
                case "reload" -> reloadCommand.onCommand(sender, command, label, args);
                case "help" -> helpCommand.onCommand(sender, command, label, args);
                default -> false;
            };
    });
        Objects.requireNonNull(plugin.getCommand("powerfly")).setTabCompleter((sender, command, label, args) -> {
            if (args.length == 1) {
                return StringUtil.copyPartialMatches(args[0], List.of("fly", "check", "addflytime", "delflytime", "reload", "help"), new ArrayList<>());
            } else if (args.length == 2) {
                if (List.of("fly", "addflytime", "delflytime").contains(args[0].toLowerCase())) {
                    List<String> options = new ArrayList<>();
                    options.add("all");
                    Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                    return StringUtil.copyPartialMatches(args[1], options, new ArrayList<>());
                } else if (args[0].equalsIgnoreCase("check")) {
                    List<String> options = new ArrayList<>();
                    Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                    return StringUtil.copyPartialMatches(args[1], options, new ArrayList<>());
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("fly")) {
                return StringUtil.copyPartialMatches(args[2], List.of("on", "off"), new ArrayList<>());
            } else if ((args.length == 3) && (args[0].equalsIgnoreCase("addflytime") || args[0].equalsIgnoreCase("delflytime"))) {
                // Aquí devolver el placeholder en morado para indicar que va un número
                return List.of("<seconds>");
            }
            return new ArrayList<>();
        });
    }
}
