package pwf.xenova.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;

import java.io.File;

public class ReloadCommand implements CommandExecutor {

    private final PowerFly plugin;

    public ReloadCommand(PowerFly plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull[] args) {
        try {
            // Comprobación de que config.yml existe
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                plugin.getLogger().warning("config.yml not found, creating default one...");
                plugin.saveDefaultConfig();
            }

            // Comprobación de que la carpeta y archivos de traducción existen
            File translationsFolder = new File(plugin.getDataFolder(), "translations");
            File enFile = new File(translationsFolder, "en.yml");
            File esFile = new File(translationsFolder, "es.yml");
            File ptFile = new File(translationsFolder, "pt.yml");

            if (!translationsFolder.exists() || !enFile.exists() || !esFile.exists() || !ptFile.exists()) {
                plugin.getLogger().warning("Missing translation files, creating default ones...");
                plugin.saveDefaultMessages();
            }

            // Recargar config.yml y messages
            plugin.reloadConfig();
            plugin.reloadMessages();

            String reloadMessage = plugin.getMessages().getString("reload-success", "§aPowerFly configuration reloaded successfully!");
            sender.sendMessage(reloadMessage);

        } catch (Exception e) {
            plugin.getLogger().severe("Error reloading configuration or messages:");
            plugin.getLogger().severe("Exception: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe("    at " + element.toString());
            }
            sender.sendMessage("§cAn error occurred while reloading configuration or messages.");
        }

        return true;
    }
}
