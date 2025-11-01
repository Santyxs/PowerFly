package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pwf.xenova.PowerFly;
import java.io.File;
import java.io.IOException;

import java.util.*;

public class FlyTimeManager {

    private final PowerFly plugin;
    private final Map<UUID, Integer> flyTimeMap = new HashMap<>();
    private final Set<UUID> warningSent = new HashSet<>();
    private File file;
    private FileConfiguration config;
    private int tickCounter = 0;

    public FlyTimeManager(PowerFly plugin) {
        this.plugin = plugin;
        load();
        startFlyTimeTask();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "database.yml");

        if (!file.exists()) {
            try {
                if (!file.createNewFile()) plugin.getLogger().warning("Could not create database.yml");
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create database.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        flyTimeMap.clear();

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int time = config.getInt(uuid + ".time", 0);
                flyTimeMap.put(uuid, time);

                if (!config.contains(uuid + ".name")) {
                    String name = Optional.ofNullable(Bukkit.getPlayer(uuid))
                            .map(Player::getName)
                            .orElse("Unknown");
                    config.set(uuid + ".name", name);
                }

                if (!config.contains(uuid + ".cooldown")) {
                    config.set(uuid + ".cooldown", 0L);
                }

            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in database.yml: " + key);
            }
        }

        saveSync();
    }

    private void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                try {
                    config.save(file);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to save database.yml: " + e.getMessage());
                }
            }
        });
    }

    private void saveSync() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save database.yml: " + e.getMessage());
        }
    }

    private void saveDataToConfig() {
        flyTimeMap.forEach((uuid, time) -> {
            String name = Optional.ofNullable(Bukkit.getPlayer(uuid))
                    .map(Player::getName)
                    .orElse(config.getString(uuid + ".name", "Unknown"));
            long cooldown = config.getLong(uuid + ".cooldown", 0L);

            config.set(uuid + ".name", name);
            config.set(uuid + ".time", time);
            config.set(uuid + ".cooldown", cooldown);
        });
    }

    private void startFlyTimeTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                int flyTime = getRemainingFlyTime(uuid);

                if (player.getAllowFlight() && flyTime > 0) {
                    delFlyTime(uuid, 1);
                    flyTime--;
                }

                if (flyTime == 10 && !warningSent.contains(uuid)) {
                    player.sendMessage(plugin.getPrefixedMessage(
                            "fly-time-warning",
                            "&6⚠ &eYou have &c10s &eof fly left."
                    ));
                    warningSent.add(uuid);
                }

                if (flyTime <= 0 && player.getAllowFlight()) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    plugin.getNoFallDamageSet().add(uuid);

                    player.sendMessage(plugin.getPrefixedMessage(
                            "fly-time-ended",
                            "&cFly time has ended."
                    ));

                    warningSent.remove(uuid);
                    plugin.getCooldownFlyManager().startCooldown(uuid);
                }
            }

            tickCounter++;
            if (tickCounter >= 60) {
                saveDataToConfig();
                saveAsync();
                tickCounter = 0;
            }
        }, 20L, 20L);
    }

    public int getRemainingFlyTime(UUID uuid) {
        return flyTimeMap.getOrDefault(uuid, 0);
    }

    public void addFlyTime(UUID uuid, int seconds) {
        flyTimeMap.merge(uuid, seconds, Integer::sum);
        if (getRemainingFlyTime(uuid) > 10) warningSent.remove(uuid);
        saveDataToConfig();
        saveAsync();
    }

    public void delFlyTime(UUID uuid, int seconds) {
        flyTimeMap.put(uuid, Math.max(0, getRemainingFlyTime(uuid) - seconds));
        saveDataToConfig();
        saveAsync();
    }

    public void setFlyTime(UUID uuid, int seconds) {
        flyTimeMap.put(uuid, Math.max(0, seconds));
        if (seconds > 10) warningSent.remove(uuid);
        saveDataToConfig();
        saveAsync();
    }

    public void reloadFlyTime(UUID uuid) {
        String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(uuid);
        int flyTime = plugin.getGroupFlyTimeManager().getGroupFlyTime(group);
        flyTimeMap.put(uuid, flyTime);
        warningSent.remove(uuid);
        saveDataToConfig();
        saveAsync();
    }

    public void removePlayer(UUID uuid) {
        flyTimeMap.remove(uuid);
        warningSent.remove(uuid);
        config.set(uuid.toString(), null);
        saveAsync();
    }

    public void reload() {
        flyTimeMap.clear();
        warningSent.clear();
        load();
    }

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();

        flyTimeMap.computeIfAbsent(uuid, id -> {
            int flyTime;
            if (plugin.getConfig().getBoolean("use-group-fly-time")) {
                String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(uuid);
                flyTime = plugin.getConfig().getInt("groups-fly-time." + group,
                        plugin.getConfig().getInt("fly-time", 10));
            } else {
                flyTime = plugin.getConfig().getInt("fly-time", 10);
            }

            config.set(uuid + ".name", player.getName());
            config.set(uuid + ".time", flyTime);
            config.set(uuid + ".cooldown", 0L);
            saveAsync();

            return flyTime;
        });

        warningSent.remove(uuid);
    }

    public void save() {
        saveDataToConfig();
        saveSync();
    }
}
