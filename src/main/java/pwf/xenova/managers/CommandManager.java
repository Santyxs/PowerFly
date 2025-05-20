package pwf.xenova.managers;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import pwf.xenova.PowerFly;
import pwf.xenova.commands.FlyCommand;
import pwf.xenova.commands.HelpCommand;
import pwf.xenova.commands.ReloadCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandManager {

    public static void registerCommands(JavaPlugin plugin) {
        // Registro del comando /fly
        Objects.requireNonNull(plugin.getCommand("fly")).setExecutor(new FlyCommand());

        // Registro del comando /powerfly y manejo de subcomandos y autocompletado
        Objects.requireNonNull(plugin.getCommand("powerfly")).setExecutor((sender, command, label, args) -> {
            // Si no se pasan argumentos, mostramos el uso del comando
            if (args.length == 0) {
                return false;
            }

            // Lógica para manejar los subcomandos
            switch (args[0].toLowerCase()) {
                case "reload":
                    new ReloadCommand((PowerFly) plugin).onCommand(sender, command, label, args);
                    break;
                case "help":
                    new HelpCommand((PowerFly) plugin).onCommand(sender, command, label, args);
                    break;
                default:
                    return false; // Si el subcomando no es reconocido, se muestra el uso
            }
            return true;
        });

        // Registro del TabCompleter para el comando /powerfly
        Objects.requireNonNull(plugin.getCommand("powerfly")).setTabCompleter((sender, command, label, args) -> {
            // Lista de subcomandos disponibles
            List<String> subCommands = new ArrayList<>();
            if (args.length == 1) {
                subCommands.add("reload");
                subCommands.add("help");
            }

            // Retorna los resultados autocompletados
            return StringUtil.copyPartialMatches(args[args.length - 1], subCommands, new ArrayList<>());
        });
    }
}
