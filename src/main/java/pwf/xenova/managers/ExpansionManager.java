package pwf.xenova.managers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;

public class ExpansionManager extends PlaceholderExpansion {

    private static final long SECONDS_PER_DAY = 86400;
    private static final long SECONDS_PER_HOUR = 3600;
    private static final long SECONDS_PER_MINUTE = 60;
    private static final String INFINITE = "∞";

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

        long total = getTotalSeconds(player);

        return switch (identifier.toLowerCase()) {
            case "flytime"   -> formatTime(total);
            case "flytime_d" -> formatUnit(total, SECONDS_PER_DAY);
            case "flytime_h" -> formatUnit(total % SECONDS_PER_DAY, SECONDS_PER_HOUR);
            case "flytime_m" -> formatUnit(total % SECONDS_PER_HOUR, SECONDS_PER_MINUTE);
            case "flytime_s" -> total == -1 ? INFINITE : String.valueOf(total % SECONDS_PER_MINUTE);
            case "cooldown"  -> plugin.getCooldownFlyManager().getRemainingCooldownFormatted(player.getUniqueId());
            case "enabled"   -> player.getAllowFlight() ? "yes" : "no";
            default          -> null;
        };
    }

    private long getTotalSeconds(Player player) {
        return plugin.getFlyTimeManager().getRemainingFlyTime(player.getUniqueId());
    }

    private String formatUnit(long totalSeconds, long divisor) {
        if (totalSeconds == -1) return INFINITE;
        return String.valueOf(totalSeconds / divisor);
    }

    private String formatTime(long totalSeconds) {
        if (totalSeconds == -1) return INFINITE;
        if (totalSeconds <= 0) return "0s";

        long days = totalSeconds / SECONDS_PER_DAY;
        long hours = (totalSeconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR;
        long minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
        long seconds = totalSeconds % SECONDS_PER_MINUTE;

        StringBuilder builder = new StringBuilder();

        if (days > 0) builder.append(days).append("d ");
        if (hours > 0) builder.append(hours).append("h ");
        if (minutes > 0) builder.append(minutes).append("m ");
        if (seconds > 0 || builder.isEmpty()) builder.append(seconds).append("s");

        return builder.toString().trim();
    }
}