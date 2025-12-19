package pwf.xenova.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import pwf.xenova.commands.FlyCommand;
import pwf.xenova.utils.MessageFormat;
import pwf.xenova.PowerFly;

import java.util.*;

public class ControlFlyManager implements Listener {

    private final PowerFly plugin;
    private final List<String> blacklistWorlds = new ArrayList<>();
    private final List<String> whitelistWorlds = new ArrayList<>();
    private final Map<String, Set<String>> blacklistRegions = new HashMap<>();
    private final Map<String, Set<String>> whitelistRegions = new HashMap<>();
    private final Map<UUID, Long> messageCooldowns = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 3000;

    public ControlFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadConfig() {
        blacklistWorlds.clear();
        whitelistWorlds.clear();
        blacklistRegions.clear();
        whitelistRegions.clear();

        blacklistWorlds.addAll(plugin.getConfig().getStringList("blacklist-worlds"));
        whitelistWorlds.addAll(plugin.getConfig().getStringList("whitelist-worlds"));

        var blacklistRegionsSection = plugin.getConfig().getConfigurationSection("blacklist-regions");
        if (blacklistRegionsSection != null) {
            for (String world : blacklistRegionsSection.getKeys(false)) {
                blacklistRegions.put(world, new HashSet<>(blacklistRegionsSection.getStringList(world)));
            }
        }

        var whitelistRegionsSection = plugin.getConfig().getConfigurationSection("whitelist-regions");
        if (whitelistRegionsSection != null) {
            for (String world : whitelistRegionsSection.getKeys(false)) {
                whitelistRegions.put(world, new HashSet<>(whitelistRegionsSection.getStringList(world)));
            }
        }
    }

    public void reload() {
        loadConfig();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (FlyCommand.hasPluginFlyActive(player.getUniqueId())) continue;
            if (isFlightBlocked(player)) disableFlight(player, true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isFlying() && !player.getAllowFlight()) return;
        if (FlyCommand.hasPluginFlyActive(player.getUniqueId())) return;
        if (isFlightBlocked(player)) disableFlight(player, false);
    }

    public boolean isFlightBlocked(Player player) {
        World world = player.getWorld();

        if (isWorldWhitelisted(world)) return false;
        if (isWorldBlacklisted(world)) return true;

        return isFlightBlockedInRegion(player);
    }

    private boolean isWorldWhitelisted(World world) {
        String worldName = world.getName();
        for (String pattern : whitelistWorlds) {
            if (matchesPattern(worldName, pattern)) return true;
        }
        return false;
    }

    private boolean isWorldBlacklisted(World world) {
        String worldName = world.getName();
        for (String pattern : blacklistWorlds) {
            if (matchesPattern(worldName, pattern)) return true;
        }
        return false;
    }

    private boolean matchesPattern(String worldName, String pattern) {
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", ".*");
            return worldName.matches(regex);
        }
        return worldName.equals(pattern);
    }

    public boolean isFlightBlockedInRegion(Player player) {
        World world = player.getWorld();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) return false;

        var vector = com.sk89q.worldedit.math.BlockVector3.at(
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ()
        );

        ApplicableRegionSet regions = manager.getApplicableRegions(vector);

        Set<String> whitelisted = whitelistRegions.get(world.getName());
        if (whitelisted != null) {
            for (ProtectedRegion region : regions) {
                if (whitelisted.contains(region.getId())) return false;
            }
        }

        Set<String> blacklisted = blacklistRegions.get(world.getName());
        if (blacklisted != null) {
            for (ProtectedRegion region : regions) {
                if (blacklisted.contains(region.getId())) return true;
            }
        }

        return false;
    }

    private void disableFlight(Player player, boolean forceMessage) {
        UUID uuid = player.getUniqueId();
        boolean shouldNotify = forceMessage || canSendMessage(uuid);

        player.setAllowFlight(false);
        player.setFlying(false);

        if (shouldNotify) plugin.getSoundEffectsManager().playDeactivationEffects(player);

        FlyCommand flyCommand = new FlyCommand(plugin);
        flyCommand.cleanupFlyData(player);

        if (shouldNotify) {
            String message = plugin.getMessages().getString("blacklist-worlds", "&cYou cannot fly in this world or region.");
            String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
            player.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
            messageCooldowns.put(uuid, System.currentTimeMillis());
        }
    }

    private boolean canSendMessage(UUID uuid) {
        Long lastMessage = messageCooldowns.get(uuid);
        return lastMessage == null || (System.currentTimeMillis() - lastMessage) >= MESSAGE_COOLDOWN_MS;
    }

    public void removePlayerCooldown(UUID uuid) {
        messageCooldowns.remove(uuid);
    }
}
