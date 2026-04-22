package pwf.xenova.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pwf.xenova.PowerFly;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class YAMLStorage implements StorageInterface {

    private static final String DATABASE_FILE = "database.yml";
    private static final String PATH_TIME = ".time";
    private static final String PATH_NAME = ".name";
    private static final String PATH_COOLDOWN = ".cooldown";

    private final PowerFly plugin;
    private final File file;
    private FileConfiguration config;
    private boolean dirty = false;

    public YAMLStorage(PowerFly plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), DATABASE_FILE);
        initialize();
        startAutoSave();
    }

    private void initialize() {
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("Could not create " + DATABASE_FILE);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create " + DATABASE_FILE + ": " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void startAutoSave() {
        int intervalTicks = plugin.getConfig().getInt("autosave-interval", 6000);

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (dirty) save();
        }, intervalTicks, intervalTicks);
    }

    public int getFlyTime(UUID uuid) {
        return config.getInt(uuid + PATH_TIME, 0);
    }

    public void setFlyTime(UUID uuid, int time) {
        config.set(uuid + PATH_TIME, time);
        markDirty();
    }

    public void addFlyTime(UUID uuid, int seconds) {
        if (seconds == -1) {
            setFlyTime(uuid, -1);
            return;
        }
        int current = getFlyTime(uuid);
        if (current != -1) {
            setFlyTime(uuid, current + seconds);
        }
    }

    public void delFlyTime(UUID uuid, int seconds) {
        int current = getFlyTime(uuid);
        if (current != -1) {
            setFlyTime(uuid, Math.max(0, current - seconds));
        }
    }

    public long getCooldown(UUID uuid) {
        return config.getLong(uuid + PATH_COOLDOWN, 0L);
    }

    public void setCooldown(UUID uuid, long cooldownUntil) {
        config.set(uuid + PATH_COOLDOWN, cooldownUntil);
        markDirty();
    }

    public void removeCooldown(UUID uuid) {
        config.set(uuid + PATH_COOLDOWN, null);
        markDirty();
    }

    public void createPlayerIfNotExists(UUID uuid, String name, int flyTime) {
        String uuidStr = uuid.toString();
        if (!config.contains(uuidStr)) {
            config.set(uuidStr + PATH_NAME, name);
            config.set(uuidStr + PATH_TIME, flyTime);
            config.set(uuidStr + PATH_COOLDOWN, 0L);
            markDirty();
        }
    }

    public void removePlayer(UUID uuid) {
        config.set(uuid.toString(), null);
        markDirty();
    }

    public Map<UUID, Integer> loadAllFlyTimes() {
        Map<UUID, Integer> map = new HashMap<>();
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int time = config.getInt(key + PATH_TIME, 0);
                map.put(uuid, time);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in " + DATABASE_FILE + ": " + key);
            }
        }
        return map;
    }

    public Map<UUID, Long> loadAllCooldowns() {
        Map<UUID, Long> map = new HashMap<>();
        long now = System.currentTimeMillis();

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long cooldownUntil = config.getLong(key + PATH_COOLDOWN, 0L);

                if (cooldownUntil == -1L || cooldownUntil > now) {
                    map.put(uuid, cooldownUntil);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in " + DATABASE_FILE + " (cooldowns): " + key);
            }
        }

        return map;
    }

    public void close() {
        save();
    }

    private void markDirty() {
        dirty = true;
    }

    private void save() {
        try {
            config.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + DATABASE_FILE + ": " + e.getMessage());
        }
    }
}