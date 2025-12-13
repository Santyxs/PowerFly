package pwf.xenova.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageFormat {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_ALT = Pattern.compile("#([A-Fa-f0-9]{6})");
    private static final Pattern MINI_MESSAGE_PATTERN = Pattern.compile("<[^>]+>");

    public static Component parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        message = convertHexToMiniMessage(message);

        if (containsMiniMessageTags(message)) {
            try {
                message = convertLegacyToMiniMessage(message);
                return MINI_MESSAGE.deserialize(message);
            } catch (Exception e) {
                return LEGACY_AMPERSAND.deserialize(message);
            }
        }

        return LEGACY_AMPERSAND.deserialize(message);
    }

    private static String convertHexToMiniMessage(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        message = matcher.replaceAll("<#$1>");

        matcher = HEX_PATTERN_ALT.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int start = matcher.start();
            if (start > 0 && message.charAt(start - 1) == '<') {
                continue;
            }
            String replacement = "<#" + matcher.group(1) + ">";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static boolean containsMiniMessageTags(String message) {
        return MINI_MESSAGE_PATTERN.matcher(message).find();
    }

    private static String convertLegacyToMiniMessage(String message) {
        message = message.replace("&0", "<black>");
        message = message.replace("&1", "<dark_blue>");
        message = message.replace("&2", "<dark_green>");
        message = message.replace("&3", "<dark_aqua>");
        message = message.replace("&4", "<dark_red>");
        message = message.replace("&5", "<dark_purple>");
        message = message.replace("&6", "<gold>");
        message = message.replace("&7", "<gray>");
        message = message.replace("&8", "<dark_gray>");
        message = message.replace("&9", "<blue>");
        message = message.replace("&a", "<green>");
        message = message.replace("&b", "<aqua>");
        message = message.replace("&c", "<red>");
        message = message.replace("&d", "<light_purple>");
        message = message.replace("&e", "<yellow>");
        message = message.replace("&f", "<white>");
        message = message.replace("&k", "<obfuscated>");
        message = message.replace("&l", "<bold>");
        message = message.replace("&m", "<strikethrough>");
        message = message.replace("&n", "<underlined>");
        message = message.replace("&o", "<italic>");
        message = message.replace("&r", "<reset>");

        return message;
    }

    public static Component parseMessageWithPrefix(String prefix, String message) {
        Component prefixComponent = parseMessage(prefix);
        Component messageComponent = parseMessage(message);
        return prefixComponent.append(messageComponent);
    }

    public static String toConsoleString(Component component) {
        return LEGACY_SECTION.serialize(component);
    }

    public static String legacyToSection(String message) {
        return LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(message));
    }

    public static String stripFormatting(String message) {
        return message.replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("&[0-9a-fk-or]", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&#[A-Fa-f0-9]{6}", "")
                .replaceAll("#[A-Fa-f0-9]{6}", "");
    }
}