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

        long totalSeconds = plugin.getFlyTimeManager().getRemainingFlyTime(player.getUniqueId());
        boolean isInfinite = (totalSeconds == -1);

        long days = isInfinite ? 0 : totalSeconds / 86400;
        long hours = isInfinite ? 0 : (totalSeconds % 86400) / 3600;
        long minutes = isInfinite ? 0 : (totalSeconds % 3600) / 60;
        long seconds = isInfinite ? 0 : totalSeconds % 60;

        return switch (identifier.toLowerCase()) {

            case "flytime" -> formatTime(totalSeconds);

            case "flytime_d" -> isInfinite ? "∞" : String.valueOf(days);
            case "flytime_h" -> isInfinite ? "∞" : String.valueOf(hours);
            case "flytime_m" -> isInfinite ? "∞" : String.valueOf(minutes);
            case "flytime_s" -> isInfinite ? "∞" : String.valueOf(seconds);

            case "cooldown" -> plugin.getCooldownFlyManager().getRemainingCooldownFormatted(player.getUniqueId());

            case "enabled" -> player.getAllowFlight() ? "yes" : "no";

            default -> null;
        };
    }

    private String formatTime(long totalSeconds) {
        if (totalSeconds == -1) {
            return "∞";
        }

        if (totalSeconds <= 0) return "0s";

        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder builder = new StringBuilder();

        if (days > 0) builder.append(days).append("d ");
        if (hours > 0) builder.append(hours).append("h ");
        if (minutes > 0) builder.append(minutes).append("m ");

        if (seconds > 0 || builder.isEmpty()) {
            builder.append(seconds).append("s");
        }

        return builder.toString().trim();
    }
}