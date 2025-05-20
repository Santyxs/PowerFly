package pwf.xenova.managers;

import net.luckperms.api.LuckPerms;
import org.bukkit.configuration.file.FileConfiguration;
import pwf.xenova.PowerFly;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GroupFlyTimeManager {

    private final PowerFly plugin;
    private final LuckPerms luckPerms;
    private final Map<String, Integer> groupFlyTimes = new HashMap<>();

    public GroupFlyTimeManager(PowerFly plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        loadTimesFromConfig();
    }

    // Tiempos de carga desde config.yml
    public void loadTimesFromConfig() {
        groupFlyTimes.clear();
        FileConfiguration config = plugin.getConfig();
        if (config.isConfigurationSection("group-times")) {
            for (String group : Objects.requireNonNull(config.getConfigurationSection("group-times")).getKeys(false)) {
                int time = config.getInt("group-times." + group, config.getInt("fly-time", 10));
                groupFlyTimes.put(group.toLowerCase(), time);
            }
        }
        plugin.getLogger().info("Group fly times loaded: " + groupFlyTimes);
    }

    // Tiempo de vuelo para un grupo
    public int getFlyTimeForGroup(String group) {
        return groupFlyTimes.getOrDefault(group.toLowerCase(), plugin.getConfig().getInt("fly-time", 10));
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
