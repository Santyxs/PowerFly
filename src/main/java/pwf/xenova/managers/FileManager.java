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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class FileManager {

    private static final String[] LANG_FILES = {"es.yml", "en.yml", "rus.yml", "pt.yml", "cn.yml"};

    private final JavaPlugin plugin;
    private YamlDocument config;
    private final Map<String, YamlDocument> languages = new HashMap<>();
    private boolean langWarningLogged = false;

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
        if (!langDir.exists() && !langDir.mkdirs()) {
            plugin.getLogger().warning("Could not create translations folder.");
        }
    }

    public void loadFiles() {
        loadConfig();
        loadLanguages();
    }

    private void loadConfig() {
        try {
            InputStream configResource = plugin.getResource("config.yml");
            if (configResource == null) {
                plugin.getLogger().severe("config.yml not found in JAR resources — plugin cannot load.");
                return;
            }

            config = YamlDocument.create(
                    new File(plugin.getDataFolder(), "config.yml"),
                    configResource,
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    updaterSettings
            );

            config.save();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Critical error loading PowerFly config", e);
        }
    }

    private void loadLanguages() {
        languages.clear();

        for (String fileName : LANG_FILES) {
            InputStream langResource = plugin.getResource("translations/" + fileName);
            if (langResource == null) {
                plugin.getLogger().warning("Language file not found in JAR: " + fileName + " — skipping.");
                continue;
            }

            try {
                YamlDocument langDoc = YamlDocument.create(
                        new File(plugin.getDataFolder(), "translations/" + fileName),
                        langResource,
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

        plugin.getLogger().info("Loaded " + languages.size() + " languages.");
    }

    public YamlDocument getConfig() {
        return config;
    }

    public YamlDocument getLang(String langCode) {
        if (langCode == null) langCode = "en";

        String code = langCode.toLowerCase();
        YamlDocument lang = languages.get(code);

        if (lang == null) {
            if (!langWarningLogged && !code.equals("en")) {
                plugin.getLogger().warning("Language '" + code + "' not found, using 'en' as default.");
                langWarningLogged = true;
            }
            lang = languages.get("en");
        }

        if (lang == null && !languages.isEmpty()) {
            return languages.values().iterator().next();
        }

        return lang;
    }

    public void reload() {
        langWarningLogged = false;
        try {
            config.reload();
            config.update();
            config.save();

            for (YamlDocument lang : languages.values()) {
                lang.reload();
                lang.save();
            }

            plugin.getLogger().info("PowerFly files reloaded successfully.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error reloading configurations", e);
        }
    }
}