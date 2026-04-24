package pwf.xenova.managers;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import pwf.xenova.PowerFly;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GroupFlyTimeManager {

    private final PowerFly plugin;
    private final LuckPerms luckPerms;
    private final Map<String, Integer> groupFlyTimes = new HashMap<>();
    private int defaultFlyTime;

    public GroupFlyTimeManager(PowerFly plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        loadTimesFromConfig();
    }

    public void reload() {
        loadTimesFromConfig();
    }

    public void loadTimesFromConfig() {
        groupFlyTimes.clear();

        defaultFlyTime = plugin.getMainConfig().getInt("global-fly-time", 100);

        if (plugin.getMainConfig().isSection("groups-fly-time")) {
            Section section = plugin.getMainConfig().getSection("groups-fly-time");

            for (String group : section.getRoutesAsStrings(false)) {
                int time = plugin.getMainConfig().getInt("groups-fly-time." + group, defaultFlyTime);
                groupFlyTimes.put(group.toLowerCase(), time);
            }
        }

        plugin.getLogger().info("Loaded fly times for " + groupFlyTimes.size() + " groups.");
    }

    public int getGroupFlyTime(String group) {
        return groupFlyTimes.getOrDefault(group.toLowerCase(), defaultFlyTime);
    }

    public String getPrimaryGroup(UUID uuid) {
        if (luckPerms == null) return "default";

        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) {
            plugin.getLogger().fine("LuckPerms user not cached for UUID " + uuid + ", falling back to 'default'.");
            return "default";
        }

        return user.getPrimaryGroup();
    }
}