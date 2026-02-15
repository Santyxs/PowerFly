package pwf.xenova.managers;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public class FileManager {

    private final JavaPlugin plugin;
    private YamlDocument config;
    private final Map<String, YamlDocument> languages = new HashMap<>();

    private final UpdaterSettings updaterSettings = UpdaterSettings.builder()
            .setVersioning(new BasicVersioning("config-version"))
            .setKeepAll(true)
            .build();

    public FileManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupDirectory();
        loadFiles();
    }

    private void setupDirectory() {
        File langDir = new File(plugin.getDataFolder(), "translations");
        if (!langDir.exists()) {
            if (!langDir.mkdirs()) {
                plugin.getLogger().warning("Could not create translations folder (it might already exist or permissions are missing).");
            }
        }
    }

    public void loadFiles() {
        try {
            config = YamlDocument.create(
                    new File(plugin.getDataFolder(), "config.yml"),
                    Objects.requireNonNull(plugin.getResource("config.yml"), "config.yml not found in JAR resources"),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    updaterSettings
            );

            config.save();

            String[] langFiles = {"es.yml", "en.yml", "rus.yml", "pt.yml", "cn.yml"};

            for (String fileName : langFiles) {
                try {
                    YamlDocument langDoc = YamlDocument.create(
                            new File(plugin.getDataFolder(), "translations/" + fileName),
                            Objects.requireNonNull(plugin.getResource("translations/" + fileName), fileName + " not found in JAR resources"),
                            GeneralSettings.DEFAULT,
                            LoaderSettings.builder().setAutoUpdate(true).build(),
                            DumperSettings.DEFAULT,
                            updaterSettings
                    );

                    langDoc.update();
                    langDoc.save();

                    languages.put(fileName.replace(".yml", "").toLowerCase(), langDoc);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not load language file: " + fileName, e);
                }
            }

            plugin.getLogger().info("Configuration and " + languages.size() + " languages loaded and updated!");

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Critical error loading PowerFly configuration files", e);
        }
    }

    public YamlDocument getConfig() {
        return config;
    }

    public YamlDocument getLang(String langCode) {
        if (langCode == null) langCode = "en";

        String code = langCode.toLowerCase();
        YamlDocument lang = languages.get(code);

        if (lang == null) {
            if (!code.equals("en")) {
                plugin.getLogger().warning("Language '" + code + "' not found, using 'en' as default.");
            }
            lang = languages.get("en");
        }

        if (lang == null && !languages.isEmpty()) {
            return languages.values().iterator().next();
        }

        return lang;
    }

    public void reload() {
        try {
            config.reload();
            config.update();
            config.save();

            for (YamlDocument lang : languages.values()) {
                lang.reload();
                lang.update();
                lang.save();
            }

            plugin.getLogger().info("PowerFly files reloaded and updated successfully!");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error trying to reload configurations", e);
        }
    }
}