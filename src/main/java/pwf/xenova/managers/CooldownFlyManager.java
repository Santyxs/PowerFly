package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pwf.xenova.storage.StorageInterface;
import pwf.xenova.PowerFly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownFlyManager implements Listener {

    private static final int NO_COOLDOWN = -1;
    private static final int PERMANENT_COOLDOWN = -2;

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
        }.runTaskTimer(plugin, 200L, 200L);
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

        if (cooldownSeconds == NO_COOLDOWN) {
            plugin.getFlyTimeManager().reloadFlyTime(playerUUID);
            return;
        }

        plugin.getFlyTimeManager().setFlyTimeInternal(playerUUID, 0);

        if (cooldownSeconds == PERMANENT_COOLDOWN) {
            setPermanentCooldown(playerUUID);
            return;
        }

        setCooldown(playerUUID, cooldownSeconds);
    }

    public void setCooldown(UUID playerUUID, int seconds) {
        long cooldownUntil = seconds == -1
                ? -1L
                : System.currentTimeMillis() + (seconds * 1000L);

        storage.setCooldown(playerUUID, cooldownUntil);
        cooldowns.put(playerUUID, cooldownUntil);
    }

    private void setPermanentCooldown(UUID playerUUID) {
        storage.setCooldown(playerUUID, -1L);
        cooldowns.put(playerUUID, -1L);
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

    public boolean isPermanentCooldown(UUID playerUUID) {
        Long val = cooldowns.get(playerUUID);
        return val != null && val == -1L;
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!isOnCooldown(uuid)) {
            cooldowns.remove(uuid);
        }
    }
}