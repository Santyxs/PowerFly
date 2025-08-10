package pwf.xenova;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import pwf.xenova.managers.*;
import pwf.xenova.utils.ErrorUtils;

import java.io.File;

public class PowerFly extends JavaPlugin {

    private static PowerFly instance;

    // ----------------- Managers -----------------

    private YamlConfiguration messages;
    private FlyTimeManager flyTimeManager;
    private CooldownFlyManager cooldownManager;
    private GroupFlyTimeManager groupFlyTimeManager;
    private SoundEffectsManager soundEffectsManager;
    private LuckPerms luckPerms;
    private Economy economy;

    // ----------------- Activation -----------------

    public void onEnable() {
        instance = this;

        // bStats
        int pluginId = 26789;
        new Metrics(this, pluginId);

        saveDefaultConfig();
        saveDefaultMessages();

        // Vault
        if (setupEconomy()) {
            getLogger().info("Economy hooked successfully: " + economy.getName());
        } else {
            getLogger().info("Economy features disabled (Vault not found or no provider).");
        }

        // LuckPerms
        try {
            luckPerms = LuckPermsProvider.get();
            groupFlyTimeManager = new GroupFlyTimeManager(this, luckPerms);
        } catch (IllegalStateException e) {
            ErrorUtils.handleLuckPermsError(e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        reloadMessages();

        flyTimeManager = new FlyTimeManager(this);
        cooldownManager = new CooldownFlyManager(this);
        soundEffectsManager = new SoundEffectsManager(this);

        CommandManager.registerCommands(this);

        getLogger().info("PowerFly plugin has been enabled.");
    }

    // ----------------- Deactivation -----------------

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

    public YamlConfiguration getMessages() {
        return messages;
    }

    public FlyTimeManager getFlyTimeManager() {
        return flyTimeManager;
    }

    public GroupFlyTimeManager getGroupFlyTimeManager() {
        return groupFlyTimeManager;
    }

    public CooldownFlyManager getCooldownFlyManager() {
        return cooldownManager;
    }

    public SoundEffectsManager getSoundEffectsManager() {
        return soundEffectsManager;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public Economy getEconomy() {
        return economy;
    }

    // ----------------- Economy -----------------

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    // ----------------- Messages -----------------

    private String getDefaultPrefix() {
        return getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
    }

    public Component getPrefixedMessage(String key, String defaultMessage) {
        String prefix = getDefaultPrefix();
        String message = messages != null ? messages.getString(key, defaultMessage) : defaultMessage;
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message);
    }

    public String getPrefixedConsoleMessage(String message) {
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(getDefaultPrefix() + message);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public String getMessage(String key, String defaultMessage) {
        return messages != null ? messages.getString(key, defaultMessage) : defaultMessage;
    }

    // ----------------- Translation -----------------

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

        saveIfNotExists(translationsFolder, "en.yml");
        saveIfNotExists(translationsFolder, "es.yml");
        saveIfNotExists(translationsFolder, "pt.yml");

        File messagesFile = new File(translationsFolder, "en.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void saveIfNotExists(File folder, String fileName) {
        if (!new File(folder, fileName).exists()) {
            saveResource("translations/" + fileName, false);
        }
    }
}
