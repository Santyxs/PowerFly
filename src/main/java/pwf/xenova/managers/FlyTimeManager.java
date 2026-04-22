package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pwf.xenova.PowerFly;
import pwf.xenova.commands.FlyCommand;
import pwf.xenova.storage.StorageInterface;
import pwf.xenova.utils.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class FlyTimeManager {

    private static final int INFINITE = -1;

    private final PowerFly plugin;
    private final Map<UUID, Integer> flyTimeMap = new ConcurrentHashMap<>();
    private final StorageInterface storage;

    public FlyTimeManager(PowerFly plugin) {
        this.plugin = plugin;
        this.storage = plugin.getStorage();
        load();
    }

    private void load() {
        flyTimeMap.clear();
        flyTimeMap.putAll(storage.loadAllFlyTimes());
    }

    public int getRemainingFlyTime(UUID playerUUID) {
        return flyTimeMap.getOrDefault(playerUUID, 0);
    }

    public boolean hasInfiniteFlyTime(UUID playerUUID) {
        return getRemainingFlyTime(playerUUID) == INFINITE;
    }

    public void addFlyTime(UUID playerUUID, int seconds) {
        if (seconds == INFINITE) {
            setFlyTime(playerUUID, INFINITE);
            return;
        }

        int currentTime = getRemainingFlyTime(playerUUID);
        if (currentTime == INFINITE) return;

        int newTime = currentTime + seconds;
        flyTimeMap.put(playerUUID, newTime);
        storage.addFlyTime(playerUUID, seconds);

        updateLiveFlight(playerUUID, newTime);
    }

    public void delFlyTime(UUID playerUUID, int seconds) {
        int currentTime = getRemainingFlyTime(playerUUID);
        if (currentTime == INFINITE) return;

        int newTime = Math.max(0, currentTime - seconds);
        flyTimeMap.put(playerUUID, newTime);
        storage.delFlyTime(playerUUID, seconds);

        updateLiveFlight(playerUUID, newTime);
    }

    public void setFlyTime(UUID playerUUID, int seconds) {
        int newTime = Math.max(INFINITE, seconds);
        flyTimeMap.put(playerUUID, newTime);
        storage.setFlyTime(playerUUID, newTime);

        updateLiveFlight(playerUUID, newTime);
    }

    public void setFlyTimeInternal(UUID playerUUID, int seconds) {
        int newTime = Math.max(INFINITE, seconds);
        flyTimeMap.put(playerUUID, newTime);
        storage.setFlyTime(playerUUID, newTime);
    }

    public void reloadFlyTime(UUID playerUUID) {
        String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(playerUUID);
        int flyTime = plugin.getGroupFlyTimeManager().getGroupFlyTime(group);
        setFlyTime(playerUUID, flyTime);
    }

    public void removePlayer(UUID playerUUID) {
        flyTimeMap.remove(playerUUID);
        storage.removePlayer(playerUUID);
    }

    public void reload() {
        flyTimeMap.clear();
        load();
    }

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();
        if (flyTimeMap.containsKey(uuid)) return;

        int flyTime;
        if (plugin.getFileManager().getConfig().getBoolean("use-groups-fly-time", false)) {
            String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(uuid);
            flyTime = plugin.getGroupFlyTimeManager().getGroupFlyTime(group);
        } else {
            flyTime = plugin.getFileManager().getConfig().getInt("global-fly-time", 100);
        }

        flyTimeMap.put(uuid, flyTime);
        storage.createPlayerIfNotExists(uuid, player.getName(), flyTime);
    }

    public StorageInterface getStorage() {
        return storage;
    }

    public String formatTime(int totalSeconds) {
        return MessageFormat.formatTime(totalSeconds);
    }

    private void updateLiveFlight(UUID uuid, int newTime) {
        if (!FlyCommand.hasPluginFlyActive(uuid)) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;

        if (plugin.getFileManager().getConfig().getBoolean("show-bossbar", true)) {
            plugin.getFlyRuntimeManager().showBossBar(player, newTime);
        }
    }
}