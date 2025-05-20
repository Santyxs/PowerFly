package pwf.xenova;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pwf.xenova.managers.CommandManager;
import pwf.xenova.managers.GroupFlyTimeManager;
import pwf.xenova.managers.SoundEffectsManager;
import pwf.xenova.utils.ErrorUtils;

import java.io.File;

public class PowerFly extends JavaPlugin {

    private static PowerFly instance;
    private YamlConfiguration messages;
    private GroupFlyTimeManager groupFlyTimeManager;
    private LuckPerms luckPerms;
    private SoundEffectsManager soundEffectsManager;

    public void onEnable() {
        instance = this;

        // Crea el archivo config.yml si no existe
        saveDefaultConfig();

        // Crea y carga los archivos de traducción predeterminados
        saveDefaultMessages();

        // Inicialización de managers
        this.soundEffectsManager = new SoundEffectsManager(this);
        CommandManager.registerCommands(this);
        reloadMessages();

        // Conexión con LuckPerms
        try {
            luckPerms = LuckPermsProvider.get();
            groupFlyTimeManager = new GroupFlyTimeManager(this, luckPerms);
        } catch (IllegalStateException e) {
            ErrorUtils.handleLuckPermsError(e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("PowerFly plugin has been enabled.");
    }

    public void onDisable() {
        // Limpieza de efectos
        if (soundEffectsManager != null) {
            soundEffectsManager.cleanupAllLoops();
        }
        getLogger().info("PowerFly plugin has been disabled.");
    }

    public static PowerFly getInstance() {
        return instance;
    }

    public YamlConfiguration getMessages() {
        return messages;
    }

    public GroupFlyTimeManager getGroupFlyTimeManager() {
        return groupFlyTimeManager;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public SoundEffectsManager getSoundEffectsManager() {
        return soundEffectsManager;
    }

    public Component getPrefixedMessage(String key, String defaultMessage) {
        String prefix = getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        String message = getMessages().getString(key, defaultMessage);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message);
    }

    public String getPrefixedConsoleMessage(String message) {
        String prefix = getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    // Carga los mensajes según el idioma especificado en config.yml
    public void reloadMessages() {
        reloadConfig();
        String language = getConfig().getString("language", "en");
        File messagesFile = new File(getDataFolder(), "translations/" + language + ".yml");

        if (messagesFile.exists()) {
            messages = YamlConfiguration.loadConfiguration(messagesFile);
            getLogger().info("Messages reloaded for language: " + language);
        } else {
            ErrorUtils.handleMissingMessagesFile(language);
        }
    }

    public String getMessage(String key) {
        return messages.getString(key, "Missing message for " + key);
    }

    // Guarda los archivos de mensajes por defecto si no existen
    public void saveDefaultMessages() {
        File translationsFolder = new File(getDataFolder(), "translations");

        // Crea la carpeta de traducciones si no existe
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            getLogger().warning("The translations folder could not be created.");
        }

        // Guarda las traducciones si no existen
        if (!new File(translationsFolder, "en.yml").exists()) {
            saveResource("translations/en.yml", false);
        }
        if (!new File(translationsFolder, "es.yml").exists()) {
            saveResource("translations/es.yml", false);
        }
        if (!new File(translationsFolder, "pt.yml").exists()) {
            saveResource("translations/pt.yml", false);
        }

        // Carga el archivo de mensajes por defecto
        File messagesFile = new File(translationsFolder, "en.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
}
