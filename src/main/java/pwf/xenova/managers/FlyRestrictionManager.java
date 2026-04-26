package pwf.xenova.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pwf.xenova.PowerFly;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FlyRestrictionManager implements Listener {

    private final PowerFly plugin;
    private final Set<String> blacklistWorlds = ConcurrentHashMap.newKeySet();
    private final Set<String> whitelistWorlds = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> blacklistRegions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> whitelistRegions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> messageCooldowns = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 3000;

    private boolean worldGuardEnabled;

    public FlyRestrictionManager(PowerFly plugin) {
        this.plugin = plugin;
        loadConfig();
        setupWorldGuard();
    }

    private void setupWorldGuard() {
        this.worldGuardEnabled = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        if (worldGuardEnabled) {
            plugin.getLogger().info("WorldGuard: Hooked successfully.");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (!hasMovedBlock(from, to)) return;

        Player player = event.getPlayer();

        if (!plugin.getFlyRuntimeManager().hasActiveSession(player.getUniqueId()) || player.hasPermission("powerfly.admin")) return;

        if (isFlightBlockedInWorld(player)) {
            disableFlight(player, false);
            return;
        }

        if (isFlightBlockedInRegion(player)) {
            disableFlight(player, true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        messageCooldowns.remove(event.getPlayer().getUniqueId());
    }

    public boolean isFlightBlockedInRegion(Player player) {
        if (!worldGuardEnabled) return false;

        World world = player.getWorld();
        Location loc = player.getLocation();

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
        if (regionManager == null) return false;

        var applicableRegions = regionManager.getApplicableRegions(
                BukkitAdapter.asBlockVector(loc)
        );

        Set<String> whitelisted = whitelistRegions.get(world.getName());
        Set<String> blacklisted = blacklistRegions.get(world.getName());

        for (ProtectedRegion region : applicableRegions) {
            String id = region.getId();
            if (whitelisted != null && whitelisted.contains(id)) return false;
            if (blacklisted != null && blacklisted.contains(id)) return true;
        }

        return false;
    }

    public boolean isFlightBlockedInWorld(Player player) {
        String worldName = player.getWorld().getName();

        if (!whitelistWorlds.isEmpty()) {
            for (String whitelisted : whitelistWorlds) {
                if (matchesPattern(worldName, whitelisted)) return false;
            }
            return true;
        }

        for (String blacklisted : blacklistWorlds) {
            if (matchesPattern(worldName, blacklisted)) return true;
        }

        return false;
    }

    private boolean matchesPattern(String worldName, String pattern) {
        if (worldName == null || pattern == null) return false;
        if (pattern.contains("*")) {
            return worldName.matches(pattern.replace("*", ".*"));
        }
        return worldName.equalsIgnoreCase(pattern);
    }

    public void loadConfig() {
        blacklistWorlds.clear();
        whitelistWorlds.clear();
        blacklistRegions.clear();
        whitelistRegions.clear();

        List<String> blacklistWorldsConfig = plugin.getMainConfig().getStringList("blacklist-worlds");
        if (!blacklistWorldsConfig.isEmpty()) {
            blacklistWorlds.addAll(blacklistWorldsConfig);
            plugin.getLogger().info("Loaded " + blacklistWorlds.size() + " blacklisted worlds: " + blacklistWorlds);
        }

        List<String> whitelistWorldsConfig = plugin.getMainConfig().getStringList("whitelist-worlds");
        if (!whitelistWorldsConfig.isEmpty()) {
            whitelistWorlds.addAll(whitelistWorldsConfig);
            plugin.getLogger().info("Loaded " + whitelistWorlds.size() + " whitelisted worlds: " + whitelistWorlds);
        }

        processRegionConfig("blacklist-regions", blacklistRegions);
        processRegionConfig("whitelist-regions", whitelistRegions);
    }

    private void processRegionConfig(String path, Map<String, Set<String>> targetMap) {
        if (!plugin.getMainConfig().isSection(path)) return;

        Section section = plugin.getMainConfig().getSection(path);
        if (section == null) return;

        for (String worldName : section.getRoutesAsStrings(false)) {
            List<String> regions = section.getStringList(worldName);
            if (!regions.isEmpty()) {
                targetMap.put(worldName, new HashSet<>(regions));
                plugin.getLogger().info("Loaded regions for world '" + worldName + "': " + regions);
            }
        }
    }

    public void reload() {
        plugin.getLogger().info("Reloading FlyRestrictionManager...");
        loadConfig();

        int playersChecked = 0;
        int playersDisabled = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isFlying() || player.hasPermission("powerfly.admin")) continue;

            playersChecked++;

            if (isFlightBlockedInWorld(player)) {
                disableFlight(player, false);
                playersDisabled++;
            } else if (isFlightBlockedInRegion(player)) {
                disableFlight(player, true);
                playersDisabled++;
            }
        }

        plugin.getLogger().info("FlyRestrictionManager reload complete. Checked " + playersChecked + " flying players, disabled " + playersDisabled + " players.");
    }

    private boolean hasMovedBlock(Location from, Location to) {
        return from.getBlockX() != to.getBlockX() ||
                from.getBlockY() != to.getBlockY() ||
                from.getBlockZ() != to.getBlockZ();
    }

    private void disableFlight(Player player, boolean isRegionBlock) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        boolean shouldNotify = canSendMessage(uuid);

        player.setAllowFlight(false);
        player.setFlying(false);

        if (shouldNotify) {
            plugin.getSoundEffectsManager().playDeactivationEffects(player);
        }

        plugin.getFlyRuntimeManager().cleanup(player);

        if (shouldNotify) {
            sendBlockMessage(player, isRegionBlock);
            messageCooldowns.put(uuid, System.currentTimeMillis());
        }
    }

    private void sendBlockMessage(Player player, boolean isRegionBlock) {
        if (player == null) return;
        String messageKey = isRegionBlock ? "fly-not-allowed-in-region" : "fly-not-allowed-in-world";
        String fallbackMessage = isRegionBlock ? "&cYou cannot fly in this region." : "&cYou cannot fly in this world.";
        player.sendMessage(plugin.getPrefixedMessage(messageKey, fallbackMessage));
    }

    private boolean canSendMessage(UUID uuid) {
        if (uuid == null) return false;
        Long lastMessage = messageCooldowns.get(uuid);
        return lastMessage == null || (System.currentTimeMillis() - lastMessage) >= MESSAGE_COOLDOWN_MS;
    }
}