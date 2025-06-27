package pwf.xenova.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pwf.xenova.PowerFly;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlyTimeManager {

    private final PowerFly plugin;
    private final Map<UUID, Integer> flyTimeMap = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public FlyTimeManager(PowerFly plugin) {
        this.plugin = plugin;
        load();
    }

    // Carga los datos del archivo database.yml
    private void load() {
        file = new File(plugin.getDataFolder(), "database.yml");

        // Si el archivo no existe, se crea
        if (!file.exists()) {
            try {
                boolean created = file.createNewFile();
                if (!created) {
                    plugin.getLogger().warning("Could not create database.yml (unknown reason)");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create database.yml: " + e.getMessage());
                return;
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        // Se carga los datos en memoria
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int time = config.getInt(key);
                flyTimeMap.put(uuid, time);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in database.yml: " + key);
            }
        }
    }

    // Guarda los datos actuales en database.yml
    public void save() {
        for (Map.Entry<UUID, Integer> entry : flyTimeMap.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save database.yml: " + e.getMessage());
        }
    }

    // Devuelve el tiempo de vuelo restante de un jugador
    public int getRemainingFlyTime(UUID playerUUID) {
        return flyTimeMap.getOrDefault(playerUUID, 0);
    }

    // Añade tiempo de vuelo a un jugador y lo guarda
    public void addFlyTime(UUID playerUUID, int seconds) {
        int current = flyTimeMap.getOrDefault(playerUUID, 0);
        flyTimeMap.put(playerUUID, current + seconds);
        save();
    }

    // Resta tiempo de vuelo a un jugador y lo guarda
    public void delFlyTime(UUID playerUUID, int seconds) {
        int current = flyTimeMap.getOrDefault(playerUUID, 0);
        int updated = Math.max(0, current - seconds);
        flyTimeMap.put(playerUUID, updated);
        save();
    }

    // Establece un valor específico de tiempo de vuelo
    public void setFlyTime(UUID playerUUID, int seconds) {
        flyTimeMap.put(playerUUID, Math.max(0, seconds));
        save();
    }

    // Elimina los datos de vuelo de un jugador del mapa y del archivo
    public void removePlayer(UUID playerUUID) {
        flyTimeMap.remove(playerUUID);
        config.set(playerUUID.toString(), null);
        save();
    }

    public void reload() {
        flyTimeMap.clear();
        load();
    }
}
