package pwf.xenova.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;

public class ReloadCommand implements CommandExecutor {

    private final PowerFly plugin;

    public ReloadCommand(PowerFly plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull[] args) {
        try {
            // Recargar la configuración principal
            plugin.reloadConfig();

            // Recargar los archivos de mensajes
            plugin.reloadMessages();

            // Obtener el mensaje de éxito desde los archivos de traducción con valor por defecto
            String reloadMessage = plugin.getMessages().getString("reload-success", "§aPowerFly configuration reloaded successfully!");

            // Enviar el mensaje de éxito al jugador o consola
            sender.sendMessage(reloadMessage);

        } catch (Exception e) {
            // En caso de error, enviar un mensaje a la consola y al jugador
            plugin.getLogger().warning("Error reloading configuration or messages.");
            sender.sendMessage("§cAn error occurred while reloading configuration or messages.");
        }
        return true;
    }
}
