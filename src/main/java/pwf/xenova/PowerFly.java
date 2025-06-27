package pwf.xenova;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pwf.xenova.managers.CommandManager;
import pwf.xenova.managers.FlyTimeManager;
import pwf.xenova.managers.GroupFlyTimeManager;
import pwf.xenova.managers.SoundEffectsManager;
import pwf.xenova.utils.ErrorUtils;

import java.io.File;

public class PowerFly extends JavaPlugin {

    private static PowerFly instance;

    // ----------------- Managers -----------------

    private YamlConfiguration messages;
    private FlyTimeManager flyTimeManager;
    private GroupFlyTimeManager groupFlyTimeManager;
    private LuckPerms luckPerms;
    private SoundEffectsManager soundEffectsManager;

    // ----------------- Activación -----------------

    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveDefaultMessages();
        reloadMessages();

        this.flyTimeManager = new FlyTimeManager(this);
        this.soundEffectsManager = new SoundEffectsManager(this);

        CommandManager.registerCommands(this);

        try {
            luckPerms = LuckPermsProvider.get();
            groupFlyTimeManager = new GroupFlyTimeManager(this, luckPerms);
        } catch (IllegalStateException e) {
            ErrorUtils.handleLuckPermsError(e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("&aPowerFly plugin has been enabled.");
    }

    // ----------------- Desactivación -----------------

    public void onDisable() {

        if (flyTimeManager != null) {
            flyTimeManager.save();
        }

        if (soundEffectsManager != null) {
            soundEffectsManager.cleanupAllLoops();
        }

        getLogger().info("&cPowerFly plugin has been disabled.");
    }

    // ----------------- Getters -----------------

    public static PowerFly getInstance() {
        return instance;
    }

    public YamlConfiguration getMessages() {
        return messages;
    }

    public FlyTimeManager getFlyTimeManager() {
        return flyTimeManager;
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

    // ----------------- Mensajes -----------------

    public Component getPrefixedMessage(String key, String defaultMessage) {
        String prefix = getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        String message = messages.getString(key, defaultMessage);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message);
    }

    public String getPrefixedConsoleMessage(String message) {
        String prefix = getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    // ----------------- Gestión de traducción -----------------

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
    public String getMessage(String key, String defaultMessage) {
        return messages.getString(key, "&cMissing message for " + key);
    }

    public void saveDefaultMessages() {
        File translationsFolder = new File(getDataFolder(), "translations");

        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            getLogger().warning("&cThe translations folder could not be created.");
        }

        if (!new File(translationsFolder, "en.yml").exists()) {
            saveResource("translations/en.yml", false);
        }
        if (!new File(translationsFolder, "es.yml").exists()) {
            saveResource("translations/es.yml", false);
        }
        if (!new File(translationsFolder, "pt.yml").exists()) {
            saveResource("translations/pt.yml", false);
        }

        File messagesFile = new File(translationsFolder, "en.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
}
