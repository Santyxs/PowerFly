package pwf.xenova;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pwf.xenova.managers.*;
import pwf.xenova.utils.ErrorUtils;

import java.io.File;

public class PowerFly extends JavaPlugin {

    private static PowerFly instance;

    // ----------------- Managers -----------------

    private FlyTimeManager flyTimeManager;
    private CooldownFlyManager cooldownManager;
    private GroupFlyTimeManager groupFlyTimeManager;
    private SoundEffectsManager soundEffectsManager;
    private LuckPerms luckPerms;
    private YamlConfiguration messages;

    // ----------------- Activación -----------------

    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveDefaultMessages();

        try {
            luckPerms = LuckPermsProvider.get();
            groupFlyTimeManager = new GroupFlyTimeManager(this, luckPerms);
        } catch (IllegalStateException e) {
            ErrorUtils.handleLuckPermsError(e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        reloadMessages();

        this.flyTimeManager = new FlyTimeManager(this);
        this.cooldownManager = new CooldownFlyManager(this);
        this.soundEffectsManager = new SoundEffectsManager(this);

        CommandManager.registerCommands(this);

        getLogger().info("PowerFly plugin has been enabled.");
    }

    // ----------------- Desactivación -----------------

    public void onDisable() {
        if (flyTimeManager != null) {
            flyTimeManager.save();
        }

        if (soundEffectsManager != null) {
            soundEffectsManager.cleanupAllLoops();
        }

        getLogger().info("PowerFly plugin has been disabled.");
    }

    // ----------------- Getters -----------------

    public static PowerFly getInstance() {
        return instance;
    }

    public FlyTimeManager getFlyTimeManager() {
        return flyTimeManager;
    }

    public CooldownFlyManager getCooldownFlyManager() {
        return cooldownManager;
    }

    public GroupFlyTimeManager getGroupFlyTimeManager() {
        return groupFlyTimeManager;
    }

    public SoundEffectsManager getSoundEffectsManager() {
        return soundEffectsManager;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public YamlConfiguration getMessages() {
        return messages;
    }

    // ----------------- Mensajes -----------------

    public Component getPrefixedMessage(String key, String defaultMessage) {
        String prefix = getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        String message = messages != null ? messages.getString(key, defaultMessage) : defaultMessage;
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message);
    }

    public String getPrefixedConsoleMessage(String message) {
        String prefix = getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public String getMessage(String key, String defaultMessage) {
        return messages != null ? messages.getString(key, defaultMessage) : defaultMessage;
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
            messages = null;
        }
    }

    public void saveDefaultMessages() {
        File translationsFolder = new File(getDataFolder(), "translations");

        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            getLogger().warning("The translations folder could not be created.");
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
