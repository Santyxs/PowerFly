package pwf.xenova.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlyTimeManager {

    private final Map<UUID, Integer> flyTimeMap = new HashMap<>();

    // Devuelve el tiempo restante de vuelo en segundos para un jugador
    public int getRemainingFlyTime(UUID playerUUID) {
        return flyTimeMap.getOrDefault(playerUUID, 0);
    }

    // Añade tiempo de vuelo en segundos
    public void addFlyTime(UUID playerUUID, int seconds) {
        int current = flyTimeMap.getOrDefault(playerUUID, 0);
        flyTimeMap.put(playerUUID, current + seconds);
    }

    // Quita tiempo de vuelo en segundos
    public void removeFlyTime(UUID playerUUID, int seconds) {
        int current = flyTimeMap.getOrDefault(playerUUID, 0);
        int updated = Math.max(0, current - seconds);
        flyTimeMap.put(playerUUID, updated);
    }

    // Establece el tiempo de vuelo en segundos
    public void setFlyTime(UUID playerUUID, int seconds) {
        flyTimeMap.put(playerUUID, Math.max(0, seconds));
    }

    // Elimina al jugador del mapa (por ejemplo, al desconectarse)
    public void removePlayer(UUID playerUUID) {
        flyTimeMap.remove(playerUUID);
    }
}
