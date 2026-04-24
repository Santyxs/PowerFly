package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pwf.xenova.PowerFly;
import pwf.xenova.storage.StorageInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownFlyManager {

    private final PowerFly plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final StorageInterface storage;

    public CooldownFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorage();
        load();
        startCleanupTimer();
    }

    public void reload() {
        cooldowns.clear();
        load();
    }

    private void load() {
        long now = System.currentTimeMillis();
        storage.loadAllCooldowns().forEach((uuid, cooldownUntil) -> {
            if (cooldownUntil == -1L || cooldownUntil > now) {
                cooldowns.put(uuid, cooldownUntil);
            }
        });
    }

    private void startCleanupTimer() {
        new BukkitRunnable() {
            public void run() {
                long now = System.currentTimeMillis();
                List<UUID> toRemove = new ArrayList<>();

                for (Map.Entry<UUID, Long> entry : cooldowns.entrySet()) {
                    if (entry.getValue() != -1L && now >= entry.getValue()) {
                        toRemove.add(entry.getKey());
                    }
                }

                for (UUID uuid : toRemove) {
                    storage.removeCooldown(uuid);
                    cooldowns.remove(uuid);
                    plugin.getFlyTimeManager().reloadFlyTime(uuid);
                    notifyRecharge(uuid);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void notifyRecharge(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(plugin.getPrefixedMessage("fly-time-recharged", "&aYour fly time has been recharged."));
        }
    }

    public void startCooldown(UUID playerUUID) {
        int cooldownSeconds;

        boolean useGroupCooldown = plugin.getMainConfig().getBoolean("use-groups-cooldown", false);

        if (useGroupCooldown) {
            String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(playerUUID);
            String path = "groups-cooldown." + group;
            if (plugin.getMainConfig().contains(path)) {
                cooldownSeconds = plugin.getMainConfig().getInt(path, 50);
            } else {
                cooldownSeconds = plugin.getMainConfig().getInt("groups-cooldown.default", 100);
            }
        } else {
            cooldownSeconds = plugin.getMainConfig().getInt("global-cooldown", 100);
        }

        if (cooldownSeconds == -1) {
            plugin.getFlyTimeManager().reloadFlyTime(playerUUID);
            return;
        }

        plugin.getFlyTimeManager().setFlyTime(playerUUID, 0);
        setCooldown(playerUUID, cooldownSeconds);
    }

    public void setCooldown(UUID playerUUID, int seconds) {
        long cooldownUntil = seconds == -1
                ? -1L
                : System.currentTimeMillis() + (seconds * 1000L);

        storage.setCooldown(playerUUID, cooldownUntil);
        cooldowns.put(playerUUID, cooldownUntil);
    }

    public void removeCooldown(UUID playerUUID) {
        storage.removeCooldown(playerUUID);
        cooldowns.remove(playerUUID);
    }

    public boolean isOnCooldown(UUID playerUUID) {
        if (!cooldowns.containsKey(playerUUID)) return false;
        long val = cooldowns.get(playerUUID);
        if (val == -1L) return true;
        return val > System.currentTimeMillis();
    }

    public boolean isNotOnCooldown(UUID playerUUID) {
        return !isOnCooldown(playerUUID);
    }

    public String getRemainingCooldownFormatted(UUID playerUUID) {
        if (!isOnCooldown(playerUUID)) return "0s";

        long val = cooldowns.get(playerUUID);
        if (val == -1L) return "∞";

        long remainingMillis = val - System.currentTimeMillis();
        long totalSeconds = remainingMillis / 1000;

        if (totalSeconds < 60) return totalSeconds + "s";

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
}