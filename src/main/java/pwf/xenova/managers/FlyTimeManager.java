package pwf.xenova.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pwf.xenova.PowerFly;
import java.io.File;
import java.io.IOException;
import java.util.*;

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
                boolean created = file.createNewFile();
                if (!created) plugin.getLogger().warning("Could not create database.yml");
                config = YamlConfiguration.loadConfiguration(file);
                save();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create database.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        flyTimeMap.clear();

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int time = config.getInt(key + ".time", 0);
                flyTimeMap.put(uuid, time);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in database.yml: " + key);
            }
        }
    }

    public void save() {
        if (config == null) config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) config.set(key + ".time", null);

        for (Map.Entry<UUID, Integer> entry : flyTimeMap.entrySet())
            config.set(entry.getKey().toString() + ".time", entry.getValue());

        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().severe("Failed to save database.yml: " + e.getMessage()); }
    }

    public int getRemainingFlyTime(UUID playerUUID) {
        return flyTimeMap.getOrDefault(playerUUID, 0);
    }

    public void addFlyTime(UUID playerUUID, int seconds) {
        int current = flyTimeMap.getOrDefault(playerUUID, 0);
        flyTimeMap.put(playerUUID, current + seconds);
        save();
    }

    public void delFlyTime(UUID playerUUID, int seconds) {
        int current = flyTimeMap.getOrDefault(playerUUID, 0);
        flyTimeMap.put(playerUUID, Math.max(0, current - seconds));
        save();
    }

    public void setFlyTime(UUID playerUUID, int seconds) {
        flyTimeMap.put(playerUUID, Math.max(0, seconds));
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
        if (config != null) {
            config.set(playerUUID.toString() + ".time", null);
            save();
        }
    }

    public void reload() {
        flyTimeMap.clear();
        load();
    }
}
