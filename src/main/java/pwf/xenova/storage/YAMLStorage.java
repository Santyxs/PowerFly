package pwf.xenova.storage;

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

public class YAMLStorage implements StorageInterface {

    private final PowerFly plugin;
    private File file;
    private FileConfiguration config;

    public YAMLStorage(PowerFly plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "database.yml");
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("Could not create database.yml");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create database.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public int getFlyTime(UUID uuid) {
        return config.getInt(uuid + ".time", 0);
    }

    @Override
    public void setFlyTime(UUID uuid, int time) {
        config.set(uuid + ".time", time);
        config.set(uuid + ".name", getPlayerName(uuid));
        save();
    }

    @Override
    public void addFlyTime(UUID uuid, int seconds) {
        int current = getFlyTime(uuid);
        if (current == -1) return;
        setFlyTime(uuid, current + seconds);
    }

    @Override
    public void delFlyTime(UUID uuid, int seconds) {
        int current = getFlyTime(uuid);
        if (current == -1) return;
        setFlyTime(uuid, Math.max(0, current - seconds));
    }

    @Override
    public long getCooldown(UUID uuid) {
        return config.getLong(uuid + ".cooldown", 0L);
    }

    @Override
    public void setCooldown(UUID uuid, long cooldownUntil) {
        config.set(uuid + ".cooldown", cooldownUntil);
        save();
    }

    @Override
    public void removeCooldown(UUID uuid) {
        config.set(uuid + ".cooldown", 0L);
        save();
    }

    @Override
    public void createPlayerIfNotExists(UUID uuid, String name, int flyTime) {
        if (!config.contains(uuid.toString())) {
            config.set(uuid + ".name", name);
            config.set(uuid + ".time", flyTime);
            config.set(uuid + ".cooldown", 0L);
            save();
        }
    }

    @Override
    public void removePlayer(UUID uuid) {
        config.set(uuid.toString(), null);
        save();
    }

    @Override
    public Map<UUID, Integer> loadAllFlyTimes() {
        Map<UUID, Integer> map = new HashMap<>();
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int time = config.getInt(uuid + ".time", 0);
                map.put(uuid, time);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in database.yml: " + key);
            }
        }
        return map;
    }

    public void close() {
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save database.yml: " + e.getMessage());
        }
    }

    private String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        return "Unknown";
    }
}