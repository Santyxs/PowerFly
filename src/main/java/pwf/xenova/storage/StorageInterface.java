package pwf.xenova.storage;

import java.util.Map;
import java.util.UUID;

public interface StorageInterface {

    int getFlyTime(UUID uuid);

    void setFlyTime(UUID uuid, int time);
    void addFlyTime(UUID uuid, int seconds);
    void delFlyTime(UUID uuid, int seconds);

    long getCooldown(UUID uuid);
    void setCooldown(UUID uuid, long cooldownUntil);
    void removeCooldown(UUID uuid);

    void createPlayerIfNotExists(UUID uuid, String name, int flyTime);
    void removePlayer(UUID uuid);

    Map<UUID, Integer> loadAllFlyTimes();

    void close();
}