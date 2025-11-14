package pwf.xenova.managers;

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
    private FileConfiguration config;

    public CooldownFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        load();

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

        for (UUID uuid : config.getKeys(false).stream().map(UUID::fromString).toList()) {
            int time = config.getInt(uuid + ".time", 0);
            long cooldownUntil = cooldowns.getOrDefault(uuid, config.getLong(uuid + ".cooldown", 0));
            Player player = Bukkit.getPlayer(uuid);
            String name = (player != null) ? player.getName() : config.getString(uuid + ".name", "Unknown");

            config.set(uuid.toString(), null);
            config.set(uuid + ".name", name);
            config.set(uuid + ".time", time);
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

    public int getRemainingCooldownSeconds(UUID playerUUID) {
        long now = System.currentTimeMillis();
        if (!cooldowns.containsKey(playerUUID)) return 0;
        long millisLeft = cooldowns.get(playerUUID) - now;
        return (int) Math.max(0, millisLeft / 1000);
    }

    public void startCooldown(UUID playerUUID) {
        if (isOnCooldown(playerUUID)) return;

        int cooldownSeconds = plugin.getConfig().getInt("cooldown", 0);
        if (cooldownSeconds > 0) {
            long cooldownUntil = System.currentTimeMillis() + (cooldownSeconds * 1000L);
            cooldowns.put(playerUUID, cooldownUntil);

            plugin.getFlyTimeManager().setFlyTime(playerUUID, 0);

            Player player = Bukkit.getPlayer(playerUUID);
            String name = (player != null) ? player.getName() : config.getString(playerUUID + ".name", "Unknown");
            int time = config.getInt(playerUUID + ".time", 0);

            config.set(playerUUID.toString(), null);
            config.set(playerUUID + ".name", name);
            config.set(playerUUID + ".time", time);
            config.set(playerUUID + ".cooldown", cooldownUntil);

            save();
            plugin.getLogger().info("Cooldown started for player " + playerUUID + " (" + name + ") for " + cooldownSeconds + " seconds.");
        }
    }

    public void removeCooldown(UUID playerUUID) {
        cooldowns.remove(playerUUID);
        config.set(playerUUID + ".cooldown", 0);
        save();
    }
}
