package pwf.xenova.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import pwf.xenova.utils.MessageFormat;
import pwf.xenova.PowerFly;

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
            plugin.getFileManager().reload();

            reloadManagers();

            plugin.reloadMessages();

            sendWithPrefix(sender, plugin.getMessageString("reload-success", "&aConfiguration reloaded!"));

        } catch (Exception e) {
            String errorMsg = plugin.getMessageString("reload-error", "&cAn error occurred while reloading: {error}")
                    .replace("{error}", e.getMessage());
            try {
                sendWithPrefix(sender, errorMsg);
            } catch (Exception ex) {
                sender.sendMessage(MessageFormat.parseMessageWithPrefix("&7[&ePower&fFly&7] &r", errorMsg));
            }
            logException(e);
        }

        return true;
    }

    private void reloadManagers() {
        plugin.getFlyTimeManager().reload();
        plugin.getControlFlyManager().reload();
        plugin.getClaimFlyManager().reload();
        plugin.getSoundEffectsManager().reload();
        plugin.getSlowMiningManager().reload();
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
        String prefix = plugin.getFileManager().getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
    }
}
