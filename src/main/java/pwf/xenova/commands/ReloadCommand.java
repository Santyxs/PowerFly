package pwf.xenova.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import pwf.xenova.utils.MessageFormat;
import pwf.xenova.PowerFly;

import java.io.File;

public record ReloadCommand(PowerFly plugin) implements CommandExecutor {

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NonNull [] args) {

        if (!sender.hasPermission("powerfly.reload") && !sender.hasPermission("powerfly.admin")) {
            sendWithPrefix(sender, plugin.getMessageString("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        try {

            reloadConfigFiles();
            reloadTranslations();
            reloadManagers();

            sendWithPrefix(sender, plugin.getMessageString("reload-success", "&Configuration reloaded!"));

        } catch (Exception e) {
            sendWithPrefix(sender, plugin.getMessageString("reload-error", "&cAn error occurred while reloading configuration or messages: " + e.getMessage()));
            logException(e);
        }

        return true;
    }

    private void reloadConfigFiles() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.getLogger().warning("Config.yml not found, creating default one...");
            plugin.saveDefaultConfig();
        } else {
            plugin.reloadConfig();
        }
    }

    private void reloadTranslations() {
        File translationsFolder = new File(plugin.getDataFolder(), "translations");
        File[] requiredFiles = {
                new File(translationsFolder, "en.yml"),
                new File(translationsFolder, "es.yml"),
                new File(translationsFolder, "pt.yml")
        };

        boolean missing = !translationsFolder.exists();
        if (!missing) {
            for (File file : requiredFiles) {
                if (!file.exists()) {
                    missing = true;
                    break;
                }
            }
        }

        if (missing) {
            plugin.getLogger().warning("Missing translation files, creating default ones...");
            plugin.saveDefaultMessages();
        } else {
            plugin.reloadMessages();
        }
    }

    private void reloadManagers() {
        plugin.getFlyTimeManager().reload();
        plugin.getControlFlyManager().reload();
        plugin.getClaimFlyManager().reload();
        plugin.getSoundEffectsManager().reload();
    }

    private void logException(Exception e) {
        plugin.getLogger().severe("=======================================");
        plugin.getLogger().severe("Critical error while reloading PowerFly");
        plugin.getLogger().severe("=======================================");
        plugin.getLogger().severe("Message: " + e.getMessage());
        plugin.getLogger().severe("Type: " + e.getClass().getName());
        plugin.getLogger().severe("Stack trace:");

        int maxLines = 10;
        StackTraceElement[] stackTrace = e.getStackTrace();

        for (int i = 0; i < Math.min(stackTrace.length, maxLines); i++) {
            plugin.getLogger().severe("  " + stackTrace[i].toString());
        }

        if (stackTrace.length > maxLines) {
            plugin.getLogger().severe("  ... " + (stackTrace.length - maxLines) + " more lines omitted");
        }

        if (e.getCause() != null) {
            plugin.getLogger().severe("Root cause: " + e.getCause().getMessage());
        }

        plugin.getLogger().severe("=====================================");
    }

    private void sendWithPrefix(CommandSender sender, String message) {
        String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
    }
}
