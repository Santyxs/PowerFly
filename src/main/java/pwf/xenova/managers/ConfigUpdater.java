package pwf.xenova.managers;

import dev.dejvokep.boostedyaml.YamlDocument;
import pwf.xenova.PowerFly;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

public class ConfigUpdater {

    public static void updateConfigWithDefaults(File file) {
        try {
            InputStream defaultConfig = getResourceAsStream();
            if (defaultConfig == null) {
                PowerFly.getInstance().getLogger().warning("Default config.yml not found in jar.");
                return;
            }

            // Crear el YamlDocument sin Settings
            YamlDocument config = YamlDocument.create(file, defaultConfig);

            // Si se actualizó correctamente, guardar los cambios
            if (config.update()) {
                config.save();
                PowerFly.getInstance().getLogger().info("Config updated with default values.");
            }
        } catch (IOException e) {
            PowerFly.getInstance().getLogger().log(Level.SEVERE, "Failed to update config.yml", e);
        }
    }

    private static InputStream getResourceAsStream() {
        return PowerFly.getInstance().getResource("config.yml");
    }
}
