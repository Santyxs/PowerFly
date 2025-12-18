package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pwf.xenova.PowerFly;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlyTimeManager {

    private final PowerFly plugin;
    private final Map<UUID, Integer> flyTimeMap = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public FlyTimeManager(PowerFly plugin) {
        this.plugin = plugin;
        load();
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
                    Player player = Bukkit.getPlayer(uuid);
                    String name = (player != null) ? player.getName() : "Unknown";
                    config.set(uuid + ".name", name);
                }

                if (!config.contains(uuid + ".cooldown")) {
                    config.set(uuid + ".cooldown", 0L);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in database.yml: " + key);
            }
        }

        save();
    }

    public void save() {
        if (config == null) config = YamlConfiguration.loadConfiguration(file);

        for (Map.Entry<UUID, Integer> entry : flyTimeMap.entrySet()) {
            UUID uuid = entry.getKey();
            int time = entry.getValue();
            String name = config.getString(uuid + ".name", "Unknown");
            long cooldown = config.getLong(uuid + ".cooldown", 0L);

            config.set(uuid + ".name", name);
            config.set(uuid + ".time", time);
            config.set(uuid + ".cooldown", cooldown);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save database.yml: " + e.getMessage());
        }
    }

    public int getRemainingFlyTime(UUID playerUUID) {
        return flyTimeMap.getOrDefault(playerUUID, 0);
    }

    public void addFlyTime(UUID playerUUID, int seconds) {
        flyTimeMap.put(playerUUID, getRemainingFlyTime(playerUUID) + seconds);
        save();
    }

    public void delFlyTime(UUID playerUUID, int seconds) {
        flyTimeMap.put(playerUUID, Math.max(0, getRemainingFlyTime(playerUUID) - seconds));
        save();
    }

    public void setFlyTime(UUID playerUUID, int seconds) {
        flyTimeMap.put(playerUUID, Math.max(-1, seconds)); // -1 = infinito
        save();
    }

    public void reloadFlyTime(UUID playerUUID) {
        String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(playerUUID);
        int flyTime = plugin.getGroupFlyTimeManager().getGroupFlyTime(group);
        flyTimeMap.put(playerUUID, flyTime);
        save();
    }

    public void removePlayer(UUID playerUUID) {
        flyTimeMap.remove(playerUUID);
        config.set(playerUUID.toString(), null);
        save();
    }

    public void reload() {
        flyTimeMap.clear();
        load();
    }

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();

        if (!flyTimeMap.containsKey(uuid)) {

            int flyTime;

            if (plugin.getConfig().getBoolean("use-group-fly-time")) {
                String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(uuid);

                Object groupValue = plugin.getConfig().get("groups-fly-time." + group);

                plugin.getLogger().info("[DEBUG] Group: " + group + ", Raw value: " + groupValue +
                        ", Type: " + (groupValue != null ? groupValue.getClass().getSimpleName() : "null"));

                if (groupValue instanceof Integer) {
                    flyTime = (Integer) groupValue;
                    plugin.getLogger().info("[DEBUG] Direct integer read: " + flyTime);
                } else if (groupValue instanceof String strValue) {
                    flyTime = parseFlyTime(strValue, plugin.getConfig().getString("fly-time", "100"));
                    plugin.getLogger().info("[DEBUG] Parsed from string '" + strValue + "': " + flyTime);
                } else {
                    String value = plugin.getConfig().getString("fly-time", "100");
                    flyTime = parseFlyTime(value, "100");
                    plugin.getLogger().info("[DEBUG] Using default fly-time: " + flyTime);
                }
            } else {
                String value = plugin.getConfig().getString("fly-time", "100");
                flyTime = parseFlyTime(value, "100");
            }

            plugin.getLogger().info("[DEBUG] Final fly time for " + player.getName() + ": " + flyTime);
            flyTimeMap.put(uuid, flyTime);

            config.set(uuid + ".name", player.getName());
            config.set(uuid + ".time", flyTime);
            config.set(uuid + ".cooldown", 0L);

            save();
        }
    }

    private int parseFlyTime(String value, String defaultValue) {
        if (value == null) value = defaultValue;
        value = value.trim().toLowerCase();

        if (value.equals("-1")) {
            return -1;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid fly-time value: '" + value + "', using default: " + defaultValue);
            try {
                return Integer.parseInt(defaultValue);
            } catch (NumberFormatException ex) {
                return 100;
            }
        }
    }

    public int getInfiniteFlyTime() {
        return -1;
    }
}