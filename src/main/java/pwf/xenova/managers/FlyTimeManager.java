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
                    if (player != null) {
                        config.set(uuid + ".name", player.getName());
                    }
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in database.yml: " + key);
            }
        }
    }

    public void save() {
        if (config == null) config = YamlConfiguration.loadConfiguration(file);

        for (Map.Entry<UUID, Integer> entry : flyTimeMap.entrySet()) {
            UUID uuid = entry.getKey();
            int time = entry.getValue();

            config.set(uuid + ".time", time);

            Player p = Bukkit.getPlayer(uuid);
            if (p != null) config.set(uuid + ".name", p.getName());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save database.yml: " + e.getMessage());
        }
    }

    public String formatTime(int totalSeconds) {
        if (totalSeconds == -1) return "∞";
        if (totalSeconds <= 0) return "0s";

        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder builder = new StringBuilder();
        if (days > 0) builder.append(days).append("d ");
        if (hours > 0) builder.append(hours).append("h ");
        if (minutes > 0) builder.append(minutes).append("m ");
        if (seconds > 0 || totalSeconds < 60) builder.append(seconds).append("s");

        return builder.toString().trim();
    }

    public int getRemainingFlyTime(UUID playerUUID) {
        return flyTimeMap.getOrDefault(playerUUID, 0);
    }

    public void addFlyTime(UUID playerUUID, int seconds) {
        if (seconds == -1) {
            setFlyTime(playerUUID, -1);
            return;
        }

        int currentTime = getRemainingFlyTime(playerUUID);
        if (currentTime == -1) return;

        int newTime = currentTime + seconds;
        flyTimeMap.put(playerUUID, newTime);
        save();

        updateLiveFlight(playerUUID, newTime);
    }

    public void delFlyTime(UUID playerUUID, int seconds) {
        int currentTime = getRemainingFlyTime(playerUUID);
        if (currentTime == -1) return;

        int newTime = Math.max(0, currentTime - seconds);
        flyTimeMap.put(playerUUID, newTime);
        save();

        updateLiveFlight(playerUUID, newTime);
    }

    public void setFlyTime(UUID playerUUID, int seconds) {
        int newTime = Math.max(-1, seconds);
        flyTimeMap.put(playerUUID, newTime);
        save();

        updateLiveFlight(playerUUID, newTime);
    }

    public void reloadFlyTime(UUID playerUUID) {
        String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(playerUUID);
        int flyTime = plugin.getGroupFlyTimeManager().getGroupFlyTime(group);
        setFlyTime(playerUUID, flyTime);
    }

    public void removePlayer(UUID playerUUID) {
        flyTimeMap.remove(playerUUID);
        config.set(playerUUID.toString(), null);
        save();
    }

    public void reload() {
        save();

        flyTimeMap.clear();
        load();
    }

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();
        if (!flyTimeMap.containsKey(uuid)) {
            int flyTime;
            if (plugin.getConfig().getBoolean("use-group-fly-time")) {
                String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(uuid);
                flyTime = plugin.getGroupFlyTimeManager().getGroupFlyTime(group);
            } else {
                flyTime = parseFlyTime(plugin.getConfig().getString("fly-time", "100"));
            }

            flyTimeMap.put(uuid, flyTime);
            config.set(uuid + ".name", player.getName());
            config.set(uuid + ".time", flyTime);
            config.set(uuid + ".cooldown", 0L);
            save();
        }
    }

    private int parseFlyTime(String value) {
        if (value == null) value = "100";
        value = value.trim().toLowerCase();
        if (value.equals("-1")) return -1;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            try {
                return Integer.parseInt("100");
            } catch (NumberFormatException ex) {
                return 100;
            }
        }
    }

    public int getInfiniteFlyTime() {
        return -1;
    }

    public boolean hasInfiniteFlyTime(UUID playerUUID) {
        return getRemainingFlyTime(playerUUID) == -1;
    }

    private void updateLiveFlight(UUID uuid, int newTime) {
        Player player = Bukkit.getPlayer(uuid);

        if (player != null) {
            player.isOnline();
        }
    }
}