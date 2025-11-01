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

    public static Component parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        message = convertHexToMiniMessage(message);

        if (message.contains("<") && message.contains(">")) {
            try {
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
            String replacement = "<#" + matcher.group(1) + ">";
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);

        return sb.toString();
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
}