package pwf.xenova.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.configuration.file.FileConfiguration;
import pwf.xenova.PowerFly;
import java.util.*;

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
        if (config.isConfigurationSection("groups-fly-time")) {
            for (String group : Objects.requireNonNull(config.getConfigurationSection("groups-fly-time")).getKeys(false)) {
                int time = config.getInt("groups-fly-time." + group, config.getInt("fly-time", 10));
                groupFlyTimes.put(group.toLowerCase(), time);
            }
        }
        plugin.getLogger().info("Group fly times loaded: " + groupFlyTimes);
    }

    public int getGroupFlyTime(String group) {
        return groupFlyTimes.getOrDefault(group.toLowerCase(), plugin.getConfig().getInt("fly-time", 10));
    }

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
