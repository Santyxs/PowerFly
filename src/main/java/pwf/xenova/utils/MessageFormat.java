package pwf.xenova.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.Map;
import java.util.regex.Pattern;

public class MessageFormat {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private static final Pattern HEX_AMPERSAND = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private static final Pattern HEX_SIMPLE = Pattern.compile("(?<!<)#([A-Fa-f0-9]{6})(?![^<>]*>)");

    private static final Map<String, String> LEGACY_TO_MINI = Map.ofEntries(
            Map.entry("&0", "<black>"),
            Map.entry("&1", "<dark_blue>"),
            Map.entry("&2", "<dark_green>"),
            Map.entry("&3", "<dark_aqua>"),
            Map.entry("&4", "<dark_red>"),
            Map.entry("&5", "<dark_purple>"),
            Map.entry("&6", "<gold>"),
            Map.entry("&7", "<gray>"),
            Map.entry("&8", "<dark_gray>"),
            Map.entry("&9", "<blue>"),
            Map.entry("&a", "<green>"),
            Map.entry("&A", "<green>"),
            Map.entry("&b", "<aqua>"),
            Map.entry("&B", "<aqua>"),
            Map.entry("&c", "<red>"),
            Map.entry("&C", "<red>"),
            Map.entry("&d", "<light_purple>"),
            Map.entry("&D", "<light_purple>"),
            Map.entry("&e", "<yellow>"),
            Map.entry("&E", "<yellow>"),
            Map.entry("&f", "<white>"),
            Map.entry("&F", "<white>"),
            Map.entry("&k", "<obfuscated>"),
            Map.entry("&K", "<obfuscated>"),
            Map.entry("&l", "<bold>"),
            Map.entry("&L", "<bold>"),
            Map.entry("&m", "<strikethrough>"),
            Map.entry("&M", "<strikethrough>"),
            Map.entry("&n", "<underlined>"),
            Map.entry("&N", "<underlined>"),
            Map.entry("&o", "<italic>"),
            Map.entry("&O", "<italic>"),
            Map.entry("&r", "<reset>"),
            Map.entry("&R", "<reset>")
    );

    public static Component parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        String processed = applyPipeline(message);

        try {
            return MINI_MESSAGE.deserialize(processed);
        } catch (Exception e) {
            return LEGACY_AMPERSAND.deserialize(message);
        }
    }

    private static String applyPipeline(String message) {
        message = convertHex(message);
        message = convertLegacy(message);
        return message;
    }

    private static String convertHex(String message) {
        message = HEX_AMPERSAND.matcher(message).replaceAll("<#$1>");

        var matcher = HEX_SIMPLE.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertLegacy(String message) {
        String result = message;
        for (var entry : LEGACY_TO_MINI.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static Component parseMessageWithPrefix(String prefix, String message) {
        return parseMessage(prefix).append(parseMessage(message));
    }

    public static String toConsoleString(Component component) {
        return LEGACY_SECTION.serialize(component);
    }

    public static String legacyToSection(String message) {
        return LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(message));
    }

    public static String stripFormatting(String message) {
        return message == null ? "" :
                message.replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("&[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&#[A-Fa-f0-9]{6}", "")
                .replaceAll("#[A-Fa-f0-9]{6}", "");
    }

    public static String formatTime(int totalSeconds) {
        if (totalSeconds == -1) return "∞";
        if (totalSeconds <= 0) return "0s";

        int days    = totalSeconds / 86400;
        int hours   = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0)    sb.append(days).append("d ");
        if (hours > 0)   sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}