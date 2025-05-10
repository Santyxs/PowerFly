package pwf.xenova;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pwf.xenova.managers.CommandManager;
import pwf.xenova.managers.ConfigUpdater;
import pwf.xenova.managers.GroupFlyTimeManager;
import pwf.xenova.utils.ErrorUtils;

import java.io.File;

public class PowerFly extends JavaPlugin {

    private static PowerFly instance;
    private YamlConfiguration messages;
    private GroupFlyTimeManager groupFlyTimeManager;
    private LuckPerms luckPerms;

    public void onEnable() {
        instance = this;

        // Cargar la configuración predeterminada y actualizar con valores nuevos
        saveDefaultConfig();
        ConfigUpdater.updateConfigWithDefaults(new File(getDataFolder(), "config.yml"));

        // Guardar los mensajes predeterminados
        saveDefaultMessages();
        CommandManager.registerCommands(this);
        reloadMessages();

        // Inicializar LuckPerms y GroupFlyTimeManager
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

    @Override
    public void onDisable() {
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

    public void reloadPlugin() {
        reloadConfig();
        ConfigUpdater.updateConfigWithDefaults(new File(getDataFolder(), "config.yml"));
        reloadMessages();
        groupFlyTimeManager.loadTimesFromConfig();
        getLogger().info("PowerFly config, messages and group times reloaded.");
    }

    private void saveDefaultMessages() {
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
