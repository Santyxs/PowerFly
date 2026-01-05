package pwf.xenova.managers;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
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

    public void reload() {
        loadTimesFromConfig();
    }

    public void loadTimesFromConfig() {
        groupFlyTimes.clear();

        YamlDocument config = plugin.getFileManager().getConfig();

        if (config.isSection("groups-fly-time")) {

            Section section = config.getSection("groups-fly-time");

            for (String group : section.getRoutesAsStrings(false)) {
                int time = config.getInt("groups-fly-time." + group, config.getInt("fly-time", 10));
                groupFlyTimes.put(group.toLowerCase(), time);
            }
        }
        plugin.getLogger().info("Group fly times loaded: " + groupFlyTimes);
    }

    public int getGroupFlyTime(String group) {
        return groupFlyTimes.getOrDefault(group.toLowerCase(),
                plugin.getFileManager().getConfig().getInt("fly-time", 10));
    }

    public String getPrimaryGroup(UUID uuid) {
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user != null) {
            return user.getPrimaryGroup();
        } else {
            return "default";
        }
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
