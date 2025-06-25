package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import pwf.xenova.PowerFly;
import pwf.xenova.commands.CheckCommand;
import pwf.xenova.commands.FlyCommand;
import pwf.xenova.commands.HelpCommand;
import pwf.xenova.commands.ReloadCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandManager {

    // Comandos y tab completions
    public static void registerCommands(JavaPlugin plugin) {

        // Establece el ejecutor para el comando /fly
        Objects.requireNonNull(plugin.getCommand("fly")).setExecutor(new FlyCommand());

        // Instancia comandos para /powerfly subcomandos
        CheckCommand checkCommand = new CheckCommand((PowerFly) plugin);
        ReloadCommand reloadCommand = new ReloadCommand((PowerFly) plugin);
        HelpCommand helpCommand = new HelpCommand((PowerFly) plugin);

        // Asigna ejecutor para /powerfly que delega según subcomando
        Objects.requireNonNull(plugin.getCommand("powerfly")).setExecutor((sender, command, label, args) -> {
            if (args.length == 0) {
                return false;
            }
            switch (args[0].toLowerCase()) {
                case "reload":
                    reloadCommand.onCommand(sender, command, label, args);
                    break;
                case "help":
                    helpCommand.onCommand(sender, command, label, args);
                    break;
                case "check":
                    checkCommand.onCommand(sender, command, label, args);
                    break;
                default:
                    return false;
            }
            return true;
        });

        // Tab completion
        Objects.requireNonNull(plugin.getCommand("powerfly")).setTabCompleter((sender, command, label, args) -> {
            if (args.length == 1) {

                // Sugerencias para el primer argumento (subcomandos)
                List<String> subCommands = List.of("reload", "help", "check");
                return StringUtil.copyPartialMatches(args[0], subCommands, new ArrayList<>());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("check")) {

                // Sugerencias de nombres de jugador para /powerfly check
                String prefix = args[1].toLowerCase();
                List<String> playerNames = new ArrayList<>();
                for (var player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(prefix)) {
                        playerNames.add(player.getName());
                    }
                }
                return playerNames;
            }
            return new ArrayList<>();
        });
    }
}
