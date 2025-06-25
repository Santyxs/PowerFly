package pwf.xenova.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull[] args) {

        try {
            // Comprobación de que config.yml existe
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                plugin.getLogger().warning("&cconfig.yml not found, creating default one...");
                plugin.saveDefaultConfig();
            }

            // Comprobación de que la carpeta y archivos de traducción existen
            File translationsFolder = new File(plugin.getDataFolder(), "translations");
            File enFile = new File(translationsFolder, "en.yml");
            File esFile = new File(translationsFolder, "es.yml");
            File ptFile = new File(translationsFolder, "pt.yml");

            if (!translationsFolder.exists() || !enFile.exists() || !esFile.exists() || !ptFile.exists()) {
                plugin.getLogger().warning("&cMissing translation files, creating default ones...");
                plugin.saveDefaultMessages();
            }

            // Recargar config.yml y messages
            plugin.reloadConfig();
            plugin.reloadMessages();

            String rawMessage = plugin.getMessages().getString("reload-success", "&aPowerFly configuration reloaded successfully!");
            Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(rawMessage);
            sender.sendMessage(message);

        } catch (Exception e) {
            plugin.getLogger().severe("Error reloading configuration or messages:");
            plugin.getLogger().severe("Exception: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe("    at " + element.toString());
            }
            Component errorMsg = LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&cAn error occurred while reloading configuration or messages.");
            sender.sendMessage(errorMsg);
        }

        return true;
    }
}
