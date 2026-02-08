package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pwf.xenova.PowerFly;
import pwf.xenova.storage.StorageInterface;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlyTimeManager {

    private final PowerFly plugin;
    private final Map<UUID, Integer> flyTimeMap = new HashMap<>();
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

    public void save() {
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
        storage.addFlyTime(playerUUID, seconds);

        updateLiveFlight(playerUUID, newTime);
    }

    public void delFlyTime(UUID playerUUID, int seconds) {
        int currentTime = getRemainingFlyTime(playerUUID);
        if (currentTime == -1) return;

        int newTime = Math.max(0, currentTime - seconds);
        flyTimeMap.put(playerUUID, newTime);
        storage.delFlyTime(playerUUID, seconds);

        updateLiveFlight(playerUUID, newTime);
    }

    public void setFlyTime(UUID playerUUID, int seconds) {
        int newTime = Math.max(-1, seconds);
        flyTimeMap.put(playerUUID, newTime);
        storage.setFlyTime(playerUUID, newTime);

        updateLiveFlight(playerUUID, newTime);
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
        if (!flyTimeMap.containsKey(uuid)) {
            int flyTime;
            if (plugin.getConfig().getBoolean("use-group-fly-time")) {
                String group = plugin.getGroupFlyTimeManager().getPrimaryGroup(uuid);
                flyTime = plugin.getGroupFlyTimeManager().getGroupFlyTime(group);
            } else {
                flyTime = parseFlyTime(plugin.getConfig().getString("fly-time", "100"));
            }

            flyTimeMap.put(uuid, flyTime);
            storage.createPlayerIfNotExists(uuid, player.getName(), flyTime);
        }
    }

    private int parseFlyTime(String value) {
        if (value == null) value = "100";
        value = value.trim().toLowerCase();
        if (value.equals("-1")) return -1;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 100;
        }
    }

    public int getInfiniteFlyTime() {
        return -1;
    }

    public boolean hasInfiniteFlyTime(UUID playerUUID) {
        return getRemainingFlyTime(playerUUID) == -1;
    }

    public StorageInterface getStorage() {
        return storage;
    }

    private void updateLiveFlight(UUID uuid, int newTime) {
        Player player = Bukkit.getPlayer(uuid);

        if (player != null) {
            player.isOnline();
        }
    }
}