package pwf.xenova.commands;

import net.kyori.adventure.text.Component;
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

        if (!sender.hasPermission("powerfly.reload")) {
            sender.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                plugin.getLogger().warning("Config.yml not found, creating default one...");
                plugin.saveDefaultConfig();
            }

            File translationsFolder = new File(plugin.getDataFolder(), "translations");
            File enFile = new File(translationsFolder, "en.yml");
            File esFile = new File(translationsFolder, "es.yml");
            File ptFile = new File(translationsFolder, "pt.yml");

            if (!translationsFolder.exists() || !enFile.exists() || !esFile.exists() || !ptFile.exists()) {
                plugin.getLogger().warning("Missing translation files, creating default ones...");
                plugin.saveDefaultMessages();
            }

            File dbFile = new File(plugin.getDataFolder(), "database.yml");
            if (!dbFile.exists()) {
                plugin.getLogger().warning("Missing database.yml, recreating...");
            }

            plugin.reloadConfig();
            plugin.reloadMessages();
            plugin.getFlyTimeManager().reload();

            Component message = plugin.getPrefixedMessage("reload-success", "&aConfiguration reloaded successfully!");
            sender.sendMessage(message);

        } catch (Exception e) {
            plugin.getLogger().severe("Error reloading configuration or messages:");
            plugin.getLogger().severe("Exception: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe("    at " + element.toString());
            }
            Component errorMsg = plugin.getPrefixedMessage("reload-error", "&cAn error occurred while reloading configuration or messages.");
            sender.sendMessage(errorMsg);
        }

        return true;
    }
}
