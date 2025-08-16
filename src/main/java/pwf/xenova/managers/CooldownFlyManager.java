package pwf.xenova.managers;

import net.kyori.adventure.text.Component;
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

                        Player player = plugin.getServer().getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            int groupTime = plugin.getGroupFlyTimeManager().getGroupFlyTime(
                                    plugin.getGroupFlyTimeManager().getPrimaryGroup(uuid)
                            );
                            plugin.getFlyTimeManager().setFlyTime(uuid, groupTime);

                            Component message = plugin.getPrefixedMessage("fly-time-recharged",
                                    "&aYour fly time has been automatically recharged.");
                            player.sendMessage(message);
                        }

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
                if (!file.createNewFile())
                    plugin.getLogger().warning("Could not create database.yml.");
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
                long cooldownUntil = config.getLong(key + ".cooldown", 0L);
                int flyTime = config.getInt(key + ".time", 0);

                if (flyTime <= 0 && cooldownUntil > System.currentTimeMillis())
                    cooldowns.put(uuid, cooldownUntil);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        if (config == null) config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            config.set(key + ".cooldown", null);
        }

        for (Map.Entry<UUID, Long> entry : cooldowns.entrySet())
            config.set(entry.getKey().toString() + ".cooldown", entry.getValue());

        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().severe("Failed to save cooldowns to database.yml: " + e.getMessage()); }
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

            save();
            plugin.getLogger().info("Cooldown started for player " + playerUUID + " for " + cooldownSeconds + " seconds.");
        }
    }

    public void removeCooldown(UUID playerUUID) {
        cooldowns.remove(playerUUID);
        if (config != null) {
            config.set(playerUUID.toString() + ".cooldown", null);
            save();
        }
    }
}
