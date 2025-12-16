package pwf.xenova;

import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import pwf.xenova.managers.*;
import pwf.xenova.utils.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class PowerFly extends JavaPlugin {

    private static PowerFly instance;

    // ----------------- Managers -----------------

    private YamlConfiguration messages;
    private LuckPerms luckPerms;
    private UpdateChecker updateChecker;
    private FlyTimeManager flyTimeManager;
    private GroupFlyTimeManager groupFlyTimeManager;
    private CooldownFlyManager cooldownManager;
    private SoundEffectsManager soundEffectsManager;
    private ControlFlyManager controlFlyManager;
    private ClaimFlyManager claimFlyManager;
    private CombatFlyManager combatFlyManager;
    private Economy economy;

    // ----------------- Plugin Enable -----------------

    private final Set<UUID> noFallDamage = new HashSet<>();

    public Set<UUID> getNoFallDamageSet() {
        return noFallDamage;
    }

    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveDefaultMessages();

        // LuckPerms
        try {
            luckPerms = LuckPermsProvider.get();
            groupFlyTimeManager = new GroupFlyTimeManager(this, luckPerms);
        } catch (IllegalStateException e) {
            handleLuckPermsError(e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // CommandManager
        CommandManager.registerCommands(this);

        // FlyTimeManager
        flyTimeManager = new FlyTimeManager(this);

        // CooldownManager
        cooldownManager = new CooldownFlyManager(this);

        // SoundEffectsManager
        soundEffectsManager = new SoundEffectsManager(this);

        // CombatFlyManager
        combatFlyManager = new CombatFlyManager(this);
        getConfig().getBoolean("disable-fly-in-combat", true);

        // ControlFlyManager
        controlFlyManager = new ControlFlyManager(this);
        getServer().getPluginManager().registerEvents(controlFlyManager, this);

        // ClaimFlyManager
        claimFlyManager = new ClaimFlyManager(this);
        getServer().getPluginManager().registerEvents(claimFlyManager, this);

        registerPlayerJoinEvent();
        registerNoFallDamageEvent();

        // Metrics
        new Metrics(this, 26789);

        // Vault
        if (setupEconomy()) {
            getLogger().info("Economy hooked successfully: " + economy.getName());
        } else {
            getLogger().info("Economy features disabled (Vault not found or no provider).");
        }

        // PlaceholderAPI
        setupExpansion();

        reloadMessages();
        handleOnlinePlayersFlyTime();
        checkForUpdates();

        getLogger().info("\u001B[32mPowerFly plugin has been enabled.\u001B[0m");
    }

    // ----------------- Plugin Disable -----------------

    public void onDisable() {
        if (flyTimeManager != null) flyTimeManager.save();
        if (soundEffectsManager != null) soundEffectsManager.cleanupAllLoops();
        getLogger().info("\u001B[31mPowerFly plugin has been disabled.\u001B[0m");
    }

    // ----------------- Events -----------------

    private void registerPlayerJoinEvent() {
        Bukkit.getPluginManager().registerEvent(
                PlayerJoinEvent.class,
                new org.bukkit.event.Listener() {},
                org.bukkit.event.EventPriority.NORMAL,
                (listener, event) -> {
                    PlayerJoinEvent joinEvent = (PlayerJoinEvent) event;
                    Player player = joinEvent.getPlayer();
                    flyTimeManager.handleJoin(player);
                },
                this
        );
    }

    private void registerNoFallDamageEvent() {
        Bukkit.getPluginManager().registerEvent(
                EntityDamageEvent.class,
                new org.bukkit.event.Listener() {},
                org.bukkit.event.EventPriority.NORMAL,
                (listener, event) -> {
                    EntityDamageEvent damageEvent = (EntityDamageEvent) event;
                    if (!(damageEvent.getEntity() instanceof Player player)) return;
                    if (damageEvent.getCause() != EntityDamageEvent.DamageCause.FALL) return;
                    if (noFallDamage.remove(player.getUniqueId())) {
                        damageEvent.setCancelled(true);
                    }
                },
                this
        );
    }

    // ----------------- Fly Time Handling -----------------

    private void handleOnlinePlayersFlyTime() {
        for (Player player : getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            int flyTime = flyTimeManager.getRemainingFlyTime(uuid);
            boolean onCooldown = cooldownManager.isOnCooldown(uuid);

            if (flyTime <= 0 && !onCooldown) {
                cooldownManager.startCooldown(uuid);
            } else if (onCooldown) {
                flyTimeManager.setFlyTime(uuid, 0);
            }
        }
    }

    // ----------------- Update Checker -----------------

    private void checkForUpdates() {
        if (getConfig().getBoolean("check-updates", true)) {
            updateChecker = new UpdateChecker(this, "Santyxs", "PowerFly");
            updateChecker.checkForUpdates(() -> {
                if (updateChecker.isUpdateAvailable()) {
                    getLogger().warning("=====================================");
                    getLogger().warning("A new version of PowerFly is available!");
                    logCurrentVersion();
                    getLogger().warning("Latest version: " + updateChecker.getLatestVersion());
                    getLogger().warning("Download: " + updateChecker.getDownloadUrl());
                    getLogger().warning("=====================================");

                    Bukkit.getOnlinePlayers().forEach(player -> {
                        if (player.isOp() || player.hasPermission("powerfly.admin")) {
                            player.sendMessage(MessageFormat.parseMessage("&e[PowerFly] &aNew version available: &f" + updateChecker.getLatestVersion()));
                        }
                    });
                } else {
                    getLogger().info("You are running the latest version.");
                }
            });
        }
    }

    @SuppressWarnings("deprecation")
    private void logCurrentVersion() {
        getLogger().warning("Current version: " + getDescription().getVersion());
    }

    // ----------------- Getters -----------------

    public static PowerFly getInstance() {
        return instance;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
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

    public ControlFlyManager getControlFlyManager() {
        return controlFlyManager;
    }

    public ClaimFlyManager getClaimFlyManager() {
        return claimFlyManager;
    }

    public CombatFlyManager getCombatFlyManager() {
        return combatFlyManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    // ----------------- Economy -----------------

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return true;
    }

    // ----------------- Expansion -----------------

    private void setupExpansion() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new pwf.xenova.managers.ExpansionManager(this).register();
            getLogger().info("PlaceholderAPI detected, registered placeholders!");
        }
    }

    // ----------------- Messages -----------------

    private String getDefaultPrefix() {
        return getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
    }

    public Component getPrefixedMessage(String key, String defaultMessage) {
        String prefix = getDefaultPrefix();
        String message = messages != null ? messages.getString(key, defaultMessage) : defaultMessage;
        return MessageFormat.parseMessageWithPrefix(prefix, message);
    }

    public Component getMessage(String key, String defaultMessage) {
        String message = messages != null ? messages.getString(key, defaultMessage) : defaultMessage;
        return MessageFormat.parseMessage(message);
    }

    public String getMessageString(String key, String defaultMessage) {
        return messages != null ? messages.getString(key, defaultMessage) : defaultMessage;
    }

    public String getPrefixedConsoleMessage(String message) {
        Component component = MessageFormat.parseMessageWithPrefix(getDefaultPrefix(), message);
        return MessageFormat.toConsoleString(component);
    }

    public void reloadMessages() {
        reloadConfig();
        String language = getConfig().getString("language", "en");
        File messagesFile = new File(getDataFolder(), "translations/" + language + ".yml");
        if (messagesFile.exists()) messages = YamlConfiguration.loadConfiguration(messagesFile);
        else {
            handleMissingMessagesFile(language);
            messages = null;
        }
    }

    public void saveDefaultMessages() {
        File translationsFolder = new File(getDataFolder(), "translations");
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            getLogger().warning("Could not create translations folder.");
        }
        saveIfNotExists(translationsFolder, "en.yml");
        saveIfNotExists(translationsFolder, "es.yml");
        saveIfNotExists(translationsFolder, "pt.yml");
        saveIfNotExists(translationsFolder, "rus.yml");
        File messagesFile = new File(translationsFolder, "en.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void saveIfNotExists(File folder, String fileName) {
        if (!new File(folder, fileName).exists()) {
            saveResource("translations/" + fileName, false);
        }
    }

    // ----------------- Error Handling -----------------

    public void handleLuckPermsError(Exception e) {
        getLogger().log(Level.SEVERE, "LuckPerms is not loaded. PowerFly will not manage group times.", e);
    }

    public void handleMissingMessagesFile(String language) {
        getLogger().warning("Messages file for language " + language + " does not exist.");
    }
}