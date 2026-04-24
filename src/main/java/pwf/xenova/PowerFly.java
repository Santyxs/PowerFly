package pwf.xenova;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import pwf.xenova.commands.*;
import pwf.xenova.managers.*;
import pwf.xenova.utils.*;
import pwf.xenova.storage.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PowerFly extends JavaPlugin {

    private static PowerFly instance;

    // ----------------- Managers -----------------

    private FileManager fileManager;
    private LuckPerms luckPerms;
    private Economy economy;
    private UpdateChecker updateChecker;
    private FlyTimeManager flyTimeManager;
    private GroupFlyTimeManager groupFlyTimeManager;
    private CooldownFlyManager cooldownManager;
    private SoundEffectsManager soundEffectsManager;
    private CombatFlyManager combatFlyManager;
    private FlyRestrictionManager flyRestrictionManager;
    private ClaimFlyManager claimFlyManager;
    private SlowMiningManager slowMiningManager;
    private StorageInterface storage;
    private FlyTimeOnGroundManager flyTimeOnGroundManager;
    private FlyCommand flyCommand;
    private FlyRuntimeManager flyRuntimeManager;

    private final Set<UUID> noFallDamage = new HashSet<>();

    public static PowerFly getInstance() { return instance; }

    public Set<UUID> getNoFallDamageSet() { return noFallDamage; }
    public FileManager getFileManager() { return fileManager; }
    public LuckPerms getLuckPerms() { return luckPerms; }
    public FlyTimeManager getFlyTimeManager() { return flyTimeManager; }
    public GroupFlyTimeManager getGroupFlyTimeManager() { return groupFlyTimeManager; }
    public CooldownFlyManager getCooldownFlyManager() { return cooldownManager; }
    public SoundEffectsManager getSoundEffectsManager() { return soundEffectsManager; }
    public CombatFlyManager getCombatFlyManager() { return combatFlyManager; }
    public FlyRestrictionManager getFlyRestrictionManager() { return flyRestrictionManager; }
    public ClaimFlyManager getClaimFlyManager() { return claimFlyManager; }
    public Economy getEconomy() { return economy; }
    public SlowMiningManager getSlowMiningManager() { return slowMiningManager; }
    public StorageInterface getStorage() { return storage; }
    public FlyTimeOnGroundManager getFlyTimeOnGroundManager() { return flyTimeOnGroundManager; }
    public FlyCommand getFlyCommand() { return flyCommand; }
    public FlyRuntimeManager getFlyRuntimeManager() { return flyRuntimeManager; }
    public void setFlyCommand(FlyCommand flyCommand) { this.flyCommand = flyCommand; }

    public YamlDocument getMainConfig() {
        return fileManager.getConfig();
    }

    // ----------------- Plugin Enable -----------------

    public void onEnable() {
        instance = this;
        setupFiles();
        setupStorage();
        setupLuckPerms();
        setupManagers();
        setupCommands();
        setupEvents();
        setupMetrics();
        setupEconomy();
        setupExpansion();
        reloadMessages();
        checkForUpdates();
        getLogger().info("\u001B[32mPowerFly plugin has been enabled.\u001B[0m");
    }

    // ----------------- Plugin Disable -----------------

    public void onDisable() {
        if (storage != null) storage.close();
        if (soundEffectsManager != null) soundEffectsManager.cleanupAllLoops();
        if (slowMiningManager != null) slowMiningManager.clearAllOnDisable();
        if (combatFlyManager != null) combatFlyManager.cleanup();
        noFallDamage.clear();
        getLogger().info("\u001B[31mPowerFly plugin has been disabled.\u001B[0m");
    }

    // ----------------- Setup -----------------

    private void setupFiles() {
        fileManager = new FileManager(this);
    }

    private void setupStorage() {
        String storageType = getMainConfig().getString("storage-type", "yaml").toUpperCase();
        if (storageType.equals("SQL")) {
            storage = new SQLStorage(this);
            getLogger().info("Using SQL storage.");
        } else {
            storage = new YAMLStorage(this);
            getLogger().info("Using YAML storage.");
        }
    }

    private void setupLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            luckPerms = LuckPermsProvider.get();
            getLogger().info("LuckPerms hooked.");
        } else {
            luckPerms = null;
            getLogger().info("LuckPerms not found, group fly-time disabled.");
        }
    }

    private void setupManagers() {
        flyRuntimeManager = new FlyRuntimeManager(this);
        flyTimeManager = new FlyTimeManager(this);
        groupFlyTimeManager = new GroupFlyTimeManager(this, luckPerms);
        cooldownManager = new CooldownFlyManager(this);
        soundEffectsManager = new SoundEffectsManager(this);
        combatFlyManager = new CombatFlyManager(this);
        flyRestrictionManager = new FlyRestrictionManager(this);
        claimFlyManager = new ClaimFlyManager(this);
        slowMiningManager = new SlowMiningManager(this);
        flyTimeOnGroundManager = new FlyTimeOnGroundManager(this);
    }

    private void setupCommands() {
        CommandManager.registerCommands(this);
    }

    private void setupEvents() {
        getServer().getPluginManager().registerEvents(flyRestrictionManager, this);
        getServer().getPluginManager().registerEvents(claimFlyManager, this);
        getServer().getPluginManager().registerEvents(slowMiningManager, this);
        getServer().getPluginManager().registerEvents(combatFlyManager, this);
        getServer().getPluginManager().registerEvents(flyTimeOnGroundManager, this);
        registerPlayerJoinEvent();
        registerNoFallDamageEvent();
    }

    private void setupMetrics() {
        new Metrics(this, 26789);
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().info("Economy disabled (Vault not found).");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().info("Economy disabled (Vault not found).");
            return;
        }
        economy = rsp.getProvider();
        getLogger().info("Economy hooked: " + economy.getName());
    }

    private void setupExpansion() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ExpansionManager(this).register();
            getLogger().info("PlaceholderAPI detected, registered placeholders!");
        }
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
                    if (noFallDamage.remove(player.getUniqueId())) damageEvent.setCancelled(true);
                },
                this
        );
    }

    // ----------------- Update Checker -----------------

    private void checkForUpdates() {
        if (!getMainConfig().getBoolean("check-updates", true)) return;

        updateChecker = new UpdateChecker(this, "127043");
        updateChecker.checkForUpdates(success -> {
            if (!success) { getLogger().warning("Could not check for updates."); return; }
            if (updateChecker.isUpdateAvailable()) {
                getLogger().warning("=====================================");
                getLogger().warning("A new version of PowerFly is available!");
                logCurrentVersion();
                getLogger().warning("Latest version: " + updateChecker.getLatestVersion());
                getLogger().warning("Download: " + updateChecker.getDownloadUrl());
                getLogger().warning("=====================================");
                Bukkit.getOnlinePlayers().forEach(player -> {
                    if (player.isOp() || player.hasPermission("powerfly.admin"))
                        player.sendMessage(MessageFormat.parseMessage("&7[&ePower&fFly&7]&r &aNew version available: &f" + updateChecker.getLatestVersion()));
                });
            } else getLogger().info("You are running the latest version.");
        });
    }

    @SuppressWarnings("deprecation")
    private void logCurrentVersion() { getLogger().warning("Current version: " + getDescription().getVersion()); }

    // ----------------- Messages -----------------

    public void reloadMessages() {
        if (fileManager != null) {
            fileManager.reload();
        }
    }

    private String getDefaultPrefix() {
        return getMainConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
    }

    private String getLangCode() {
        return getMainConfig().getString("language", "en");
    }

    public Component getPrefixedMessage(String key, String defaultMessage) {
        String prefix = getDefaultPrefix();
        String message = fileManager.getLang(getLangCode()).getString(key, defaultMessage);
        return MessageFormat.parseMessageWithPrefix(prefix, message);
    }

    public Component getMessage(String key, String defaultMessage) {
        String message = fileManager.getLang(getLangCode()).getString(key, defaultMessage);
        return MessageFormat.parseMessage(message);
    }

    public String getMessageString(String key, String defaultMessage) {
        return fileManager.getLang(getLangCode()).getString(key, defaultMessage);
    }
}