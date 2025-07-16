package pwf.xenova.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.configuration.file.FileConfiguration;
import pwf.xenova.PowerFly;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GroupFlyTimeManager {

    private final PowerFly plugin;
    private final LuckPerms luckPerms;
    private final Map<String, Integer> groupFlyTimes = new HashMap<>();

    public GroupFlyTimeManager(PowerFly plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        loadTimesFromConfig();
    }

    // Carga los tiempos desde config.yml
    public void loadTimesFromConfig() {
        groupFlyTimes.clear();
        FileConfiguration config = plugin.getConfig();
        if (config.isConfigurationSection("groups-fly-time")) {
            for (String group : Objects.requireNonNull(config.getConfigurationSection("groups-fly-time")).getKeys(false)) {
                int time = config.getInt("groups-fly-time." + group, config.getInt("fly-time", 10));
                groupFlyTimes.put(group.toLowerCase(), time);
            }
        }
        plugin.getLogger().info("Group fly times loaded: " + groupFlyTimes);
    }

    // Devuelve el tiempo de vuelo asociado a un grupo
    public int getGroupFlyTime(String group) {
        return groupFlyTimes.getOrDefault(group.toLowerCase(), plugin.getConfig().getInt("fly-time", 10));
    }

    // Devuelve el grupo primario de un jugador por UUID
    public String getPrimaryGroup(UUID uuid) {
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user != null) {
            return user.getPrimaryGroup();
        } else {
            plugin.getLogger().warning("Could not get primary group for UUID: " + uuid + " (user not loaded)");
            return "default";
        }
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
