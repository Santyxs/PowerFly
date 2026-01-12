package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pwf.xenova.PowerFly;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class CooldownFlyManager {

    private final PowerFly plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public CooldownFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        load();

        BukkitRunnable task = new BukkitRunnable() {
            public void run() {
                long now = System.currentTimeMillis();
                List<UUID> toRemove = new ArrayList<>();

                synchronized (cooldowns) {
                    for (Map.Entry<UUID, Long> entry : cooldowns.entrySet()) {
                        if (now >= entry.getValue()) {
                            toRemove.add(entry.getKey());
                        }
                    }
                }

                if (!toRemove.isEmpty()) {
                    for (UUID uuid : toRemove) {
                        cooldowns.remove(uuid);
                        rechargePlayerFly(uuid);
                        config.set(uuid + ".cooldown", 0);
                    }
                    save();
                }
            }
        };
        task.runTaskTimer(plugin, 20L, 20L);
    }

    public void reload() {
        save();
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "database.yml");

        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    plugin.getLogger().info("Created database.yml");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create database.yml: " + e.getMessage());
                return;
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        long now = System.currentTimeMillis();
        boolean changed = false;

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long cooldownUntil = config.getLong(key + ".cooldown", 0L);

                if (cooldownUntil > now) {
                    cooldowns.put(uuid, cooldownUntil);
                } else if (cooldownUntil > 0) {
                    rechargePlayerFly(uuid);
                    config.set(key + ".cooldown", 0);
                    changed = true;
                }
            } catch (Exception ignored) {}
        }
        if (changed) save();
    }

    private void rechargePlayerFly(UUID uuid) {
        String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(uuid);
        int groupTime = plugin.getGroupFlyTimeManager().getGroupFlyTime(group);

        plugin.getFlyTimeManager().setFlyTime(uuid, groupTime);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(plugin.getPrefixedMessage("fly-time-recharged", "&aYour fly time has been recharged."));
        }
    }

    public void save() {
        if (config == null || file == null) return;

        for (Map.Entry<UUID, Long> entry : cooldowns.entrySet()) {
            config.set(entry.getKey().toString() + ".cooldown", entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save database.yml: " + e.getMessage());
        }
    }

    public void startCooldown(UUID playerUUID) {
        int cooldownSeconds;

        boolean useGroupCooldown = plugin.getFileManager().getConfig().getBoolean("use-groups-cooldown", false);

        if (useGroupCooldown) {
            String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(playerUUID);

            String path = "groups-cooldown." + group;
            if (plugin.getFileManager().getConfig().contains(path)) {
                cooldownSeconds = plugin.getFileManager().getConfig().getInt(path, 50);
            } else {
                cooldownSeconds = plugin.getFileManager().getConfig().getInt("groups-cooldown.default", 100);
            }
        } else {
            cooldownSeconds = plugin.getFileManager().getConfig().getInt("global-cooldown", 100);
        }

        long cooldownUntil = System.currentTimeMillis() + (cooldownSeconds * 1000L);

        cooldowns.put(playerUUID, cooldownUntil);
        plugin.getFlyTimeManager().setFlyTime(playerUUID, 0);

        config.set(playerUUID + ".cooldown", cooldownUntil);
        save();
    }

    public void setCooldown(UUID playerUUID, int seconds) {
        long cooldownUntil = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.put(playerUUID, cooldownUntil);
        config.set(playerUUID + ".cooldown", cooldownUntil);
        save();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isOnCooldown(UUID playerUUID) {
        return cooldowns.containsKey(playerUUID) && cooldowns.get(playerUUID) > System.currentTimeMillis();
    }

    public String getRemainingCooldownFormatted(UUID playerUUID) {
        if (!isOnCooldown(playerUUID)) return "0s";

        long remainingMillis = cooldowns.get(playerUUID) - System.currentTimeMillis();
        long totalSeconds = remainingMillis / 1000;

        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }

        if (totalSeconds < 3600) {
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            return minutes + "m " + seconds + "s";
        }

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }

    public void removeCooldown(UUID playerUUID) {
        cooldowns.remove(playerUUID);
        config.set(playerUUID + ".cooldown", 0L);
        save();
    }
}
