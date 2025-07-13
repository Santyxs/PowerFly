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

    public void loadTimesFromConfig() {
        groupFlyTimes.clear();
        FileConfiguration config = plugin.getConfig();
        if (config.isConfigurationSection("groups-fly-time:s")) {
            for (String group : Objects.requireNonNull(config.getConfigurationSection("groups-fly-time:s")).getKeys(false)) {
                int time = config.getInt("groups-fly-time:s." + group, config.getInt("fly-time", 10));
                groupFlyTimes.put(group.toLowerCase(), time);
            }
        }
        plugin.getLogger().info("Group fly times loaded: " + groupFlyTimes);
    }

    public int getFlyTimeForGroup(String group) {
        return groupFlyTimes.getOrDefault(group.toLowerCase(), plugin.getConfig().getInt("fly-time", 10));
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
