package pwf.xenova.utils;

import pwf.xenova.PowerFly;
import java.util.logging.Level;

public class ErrorUtils {

    public static void handleLuckPermsError(Exception e) {
        PowerFly.getInstance().getLogger().log(Level.SEVERE,
                "&cLuckPerms is not loaded. PowerFly will not be able to manage group times.", e);
    }

    public static void handleMissingMessagesFile(String language) {
        PowerFly.getInstance().getLogger().warning(
                "&cThe messages file for language " + language + " does not exist.");
    }
}
