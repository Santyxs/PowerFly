package pwf.xenova.managers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;

public class ExpansionManager extends PlaceholderExpansion {

    private final PowerFly plugin;

    public ExpansionManager(PowerFly plugin) {
        this.plugin = plugin;
    }

    public @NotNull String getIdentifier() {
        return "powerfly";
    }

    public @NotNull String getAuthor() {
        return "Xenova";
    }

    @SuppressWarnings("deprecation")
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        return switch (identifier.toLowerCase()) {
            case "time_left" -> {
                long flyTime = plugin.getFlyTimeManager().getRemainingFlyTime(player.getUniqueId());
                yield formatTime(flyTime);
            }
            case "cooldown" -> plugin.getCooldownFlyManager().getRemainingCooldownFormatted(player.getUniqueId());
            case "enabled" -> {
                boolean flying = player.isFlying();
                yield flying ? "yes" : "no";
            }
            default -> null;
        };
    }

    private String formatTime(long seconds) {
        if (seconds == plugin.getFlyTimeManager().getInfiniteFlyTime()) {
            return "∞";
        }

        if (seconds <= 0) return "0s";

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return minutes + "m " + remainingSeconds + "s";
        } else {
            return remainingSeconds + "s";
        }
    }
}
