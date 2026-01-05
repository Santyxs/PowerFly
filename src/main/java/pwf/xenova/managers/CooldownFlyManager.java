package pwf.xenova.managers;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.kyori.adventure.text.Component;
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
    private FileConfiguration config; // Este es database.yml

    public CooldownFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        load(); // Carga la base de datos de cooldowns

        new BukkitRunnable() {
            public void run() {
                long now = System.currentTimeMillis();

                Iterator<Map.Entry<UUID, Long>> iterator = cooldowns.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, Long> entry = iterator.next();
                    UUID uuid = entry.getKey();
                    long cooldownUntil = entry.getValue();

                    if (now >= cooldownUntil) {
                        iterator.remove();

                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            int groupTime = plugin.getGroupFlyTimeManager().getGroupFlyTime(
                                    plugin.getGroupFlyTimeManager().getPrimaryGroup(uuid)
                            );
                            plugin.getFlyTimeManager().setFlyTime(uuid, groupTime);

                            Component message = plugin.getPrefixedMessage("fly-time-recharged",
                                    "&aYour fly time has been automatically recharged.");
                            player.sendMessage(message);
                        }

                        config.set(uuid + ".cooldown", 0);
                        save();

                        plugin.getLogger().info("Cooldown ended for player " + uuid);
                    }
                }
                save();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // NUEVO MÉTODO: Permite que el ReloadCommand llame aquí
    public void reload() {
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "database.yml");

        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    plugin.getLogger().info("Created database.yml");
                } else {
                    plugin.getLogger().warning("Could not create database.yml.");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create database.yml: " + e.getMessage());
                return;
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
        cooldowns.clear();

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long cooldownUntil = config.getLong(uuid + ".cooldown", 0L);
                if (cooldownUntil > System.currentTimeMillis()) {
                    cooldowns.put(uuid, cooldownUntil);
                }

                if (!config.contains(uuid + ".name")) {
                    Player player = Bukkit.getPlayer(uuid);
                    String name = (player != null) ? player.getName() : "Unknown";
                    config.set(uuid + ".name", name);
                }

                if (!config.contains(uuid + ".time")) {
                    config.set(uuid + ".time", 0);
                }

            } catch (IllegalArgumentException ignored) {}
        }
        save();
    }

    public void save() {
        if (config == null) config = YamlConfiguration.loadConfiguration(file);

        for (UUID uuid : new ArrayList<>(cooldowns.keySet())) {
            long cooldownUntil = cooldowns.getOrDefault(uuid, config.getLong(uuid + ".cooldown", 0));
            Player player = Bukkit.getPlayer(uuid);
            String name = (player != null) ? player.getName() : config.getString(uuid + ".name", "Unknown");

            config.set(uuid + ".name", name);
            config.set(uuid + ".cooldown", cooldownUntil);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save database.yml: " + e.getMessage());
        }
    }

    public boolean isOnCooldown(UUID playerUUID) {
        long now = System.currentTimeMillis();
        return cooldowns.containsKey(playerUUID) && cooldowns.get(playerUUID) > now;
    }

    public String getRemainingCooldownFormatted(UUID playerUUID) {
        long now = System.currentTimeMillis();
        if (!cooldowns.containsKey(playerUUID)) return "0s";
        long millisLeft = cooldowns.get(playerUUID) - now;
        if (millisLeft <= 0) return "0s";
        return formatTime(millisLeft);
    }

    public int getRemainingCooldownSeconds(UUID playerUUID) {
        long now = System.currentTimeMillis();
        if (!cooldowns.containsKey(playerUUID)) return 0;
        long millisLeft = cooldowns.get(playerUUID) - now;
        return (int) Math.max(0, millisLeft / 1000);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400; seconds %= 86400;
        long hours = seconds / 3600; seconds %= 3600;
        long minutes = seconds / 60; seconds %= 60;

        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append("d ");
        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m ");
        if (seconds > 0 || result.isEmpty()) result.append(seconds).append("s");
        return result.toString().trim();
    }

    private long getCooldownMillis(UUID playerUUID) {
        YamlDocument cfg = plugin.getFileManager().getConfig();

        if (cfg.getBoolean("use-groups-cooldown", false)) {
            String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(playerUUID);

            if (cfg.contains("groups-cooldown." + group)) {
                int seconds = cfg.getInt("groups-cooldown." + group);
                return seconds * 1000L;
            }
            if (cfg.contains("groups-cooldown.default")) {
                int seconds = cfg.getInt("groups-cooldown.default");
                return seconds * 1000L;
            }
        }

        int globalSeconds = cfg.getInt("global-cooldown", cfg.getInt("cooldown", 100));
        return globalSeconds * 1000L;
    }

    public void startCooldown(UUID playerUUID) {
        if (isOnCooldown(playerUUID)) return;

        long cooldownMillis = getCooldownMillis(playerUUID);

        if (cooldownMillis > 0) {
            long cooldownUntil = System.currentTimeMillis() + cooldownMillis;
            cooldowns.put(playerUUID, cooldownUntil);

            plugin.getFlyTimeManager().setFlyTime(playerUUID, 0);

            Player player = Bukkit.getPlayer(playerUUID);
            String name = (player != null) ? player.getName() : config.getString(playerUUID + ".name", "Unknown");
            int time = config.getInt(playerUUID + ".time", 0);

            config.set(playerUUID + ".name", name);
            config.set(playerUUID + ".time", time);
            config.set(playerUUID + ".cooldown", cooldownUntil);

            save();
            plugin.getLogger().info("Cooldown started for player " + playerUUID + " (" + name + ") for " + formatTime(cooldownMillis));
        }
    }

    public void removeCooldown(UUID playerUUID) {
        cooldowns.remove(playerUUID);
        config.set(playerUUID + ".cooldown", 0);
        save();
    }
}
