package pwf.xenova;

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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PowerFly extends JavaPlugin {

    private static PowerFly instance;

    // ----------------- Managers -----------------

    private FileManager fileManager;
    private LuckPerms luckPerms;
    private UpdateChecker updateChecker;
    private FlyTimeManager flyTimeManager;
    private GroupFlyTimeManager groupFlyTimeManager;
    private CooldownFlyManager cooldownManager;
    private SoundEffectsManager soundEffectsManager;
    private CombatFlyManager combatFlyManager;
    private ControlFlyManager controlFlyManager;
    private ClaimFlyManager claimFlyManager;
    private SlowMiningManager slowMiningManager;
    private Economy economy;

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
    public ControlFlyManager getControlFlyManager() { return controlFlyManager; }
    public ClaimFlyManager getClaimFlyManager() { return claimFlyManager; }
    public Economy getEconomy() { return economy; }
    public SlowMiningManager getSlowMiningManager() { return slowMiningManager; }

    // ----------------- Plugin Enable -----------------

    public void onEnable() {
        instance = this;

        // FileManager
        fileManager = new FileManager(this);

        // LuckPerms
        luckPerms = LuckPermsProvider.get();

        // Managers
        flyTimeManager = new FlyTimeManager(this);
        groupFlyTimeManager = new GroupFlyTimeManager(this, luckPerms);
        cooldownManager = new CooldownFlyManager(this);
        soundEffectsManager = new SoundEffectsManager(this);
        combatFlyManager = new CombatFlyManager(this);
        controlFlyManager = new ControlFlyManager(this);
        claimFlyManager = new ClaimFlyManager(this);
        slowMiningManager = new SlowMiningManager(this);

        // CommandManager
        CommandManager.registerCommands(this);

        // Events
        getServer().getPluginManager().registerEvents(controlFlyManager, this);
        getServer().getPluginManager().registerEvents(claimFlyManager, this);
        getServer().getPluginManager().registerEvents(slowMiningManager, this);

        registerPlayerJoinEvent();
        registerNoFallDamageEvent();

        // Metrics
        new Metrics(this, 26789);

        // Vault
        if (setupEconomy()) getLogger().info("Economy hooked: " + economy.getName());
        else getLogger().info("Economy disabled (Vault not found).");

        // PlaceholderAPI
        setupExpansion();

        reloadMessages();
        checkForUpdates();

        getLogger().info("\u001B[32mPowerFly plugin has been enabled.\u001B[0m");
    }

    // ----------------- Plugin Disable -----------------

    public void onDisable() {
        if (flyTimeManager != null) flyTimeManager.save();
        if (soundEffectsManager != null) soundEffectsManager.cleanupAllLoops();
        if (slowMiningManager != null) slowMiningManager.shutdown();
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
                    if (noFallDamage.remove(player.getUniqueId())) damageEvent.setCancelled(true);
                },
                this
        );
    }

    // ----------------- Update Checker -----------------

    private void checkForUpdates() {
        if (!fileManager.getConfig().getBoolean("check-updates", true)) return;

        updateChecker = new UpdateChecker(this, "127043");
        updateChecker.checkForUpdates(() -> {
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

    public void reloadMessages() {
        if (fileManager != null) {
            fileManager.reload();
        }
    }

    private String getDefaultPrefix() {
        return fileManager.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
    }

    private String getLangCode() {
        return fileManager.getConfig().getString("language", "en");
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
