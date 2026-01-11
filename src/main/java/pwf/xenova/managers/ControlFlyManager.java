package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import pwf.xenova.commands.FlyCommand;
import pwf.xenova.PowerFly;

import java.lang.reflect.Method;
import java.util.*;

public class ControlFlyManager implements Listener {

    private final PowerFly plugin;
    private final List<String> blacklistWorlds = new ArrayList<>();
    private final List<String> whitelistWorlds = new ArrayList<>();
    private final Map<String, Set<String>> blacklistRegions = new HashMap<>();
    private final Map<String, Set<String>> whitelistRegions = new HashMap<>();
    private final Map<UUID, Long> messageCooldowns = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 3000;

    private boolean worldGuardEnabled;
    private Object regionContainer;
    private Class<?> vectorClass;
    private Method atMethod;
    private Method adaptMethod;
    private Method getRegionManagerMethod;
    private Method getApplicableRegionsMethod;
    private Method getIdMethod;

    public ControlFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        loadConfig();
        setupWorldGuard();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void setupWorldGuard() {
        this.worldGuardEnabled = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        if (worldGuardEnabled) {
            try {
                Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                Object wgInstance = wgClass.getMethod("getInstance").invoke(null);
                Object platform = wgClass.getMethod("getPlatform").invoke(wgInstance);
                regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

                vectorClass = Class.forName("com.sk89q.worldedit.math.BlockVector3");
                atMethod = vectorClass.getMethod("at", int.class, int.class, int.class);

                Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                adaptMethod = bukkitAdapterClass.getMethod("adapt", World.class);

                Class<?> wgWorldClass = Class.forName("com.sk89q.worldguard.world.World");
                getRegionManagerMethod = regionContainer.getClass().getMethod("get", wgWorldClass);

                plugin.getLogger().info("WorldGuard: Hooked successfully.");
            } catch (Exception e) {
                plugin.getLogger().warning("Could not initialize WorldGuard hook: " + e.getMessage());
                this.worldGuardEnabled = false;
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isFlying() || player.hasPermission("powerfly.admin")) {
            return;
        }

        if (isFlightBlockedInWorld(player)) {
            disableFlight(player, false);
            return;
        }

        if (isFlightBlockedInRegion(player)) {
            disableFlight(player, true);
        }
    }

    public boolean isFlightBlockedInRegion(Player player) {
        if (!worldGuardEnabled || regionContainer == null || atMethod == null || adaptMethod == null) {
            return false;
        }

        try {
            World world = player.getWorld();
            Location loc = player.getLocation();

            Object adaptedWorld = adaptMethod.invoke(null, world);
            Object regionManager = getRegionManagerMethod.invoke(regionContainer, adaptedWorld);
            if (regionManager == null) return false;

            Object vector = atMethod.invoke(null, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

            if (getApplicableRegionsMethod == null) {
                getApplicableRegionsMethod = regionManager.getClass().getMethod("getApplicableRegions", vectorClass);
            }

            Object applicableRegions = getApplicableRegionsMethod.invoke(regionManager, vector);
            Iterable<?> regionsIterable = (Iterable<?>) applicableRegions;

            Set<String> whitelisted = whitelistRegions.get(world.getName());
            Set<String> blacklisted = blacklistRegions.get(world.getName());

            for (Object region : regionsIterable) {
                if (getIdMethod == null) {
                    getIdMethod = region.getClass().getMethod("getId");
                }
                String id = (String) getIdMethod.invoke(region);

                if (whitelisted != null && whitelisted.contains(id)) return false;
                if (blacklisted != null && blacklisted.contains(id)) return true;
            }

        } catch (Exception ignored) {
        }
        return false;
    }

    public void loadConfig() {
        blacklistWorlds.clear();
        whitelistWorlds.clear();
        blacklistRegions.clear();
        whitelistRegions.clear();

        List<String> blacklistWorldsConfig = plugin.getConfig().getStringList("blacklist-worlds");
        if (!blacklistWorldsConfig.isEmpty()) blacklistWorlds.addAll(blacklistWorldsConfig);

        List<String> whitelistWorldsConfig = plugin.getConfig().getStringList("whitelist-worlds");
        if (!whitelistWorldsConfig.isEmpty()) whitelistWorlds.addAll(whitelistWorldsConfig);

        processRegionConfig("blacklist-regions", blacklistRegions);
        processRegionConfig("whitelist-regions", whitelistRegions);
    }

    private void processRegionConfig(String path, Map<String, Set<String>> targetMap) {
        if (plugin.getConfig().isConfigurationSection(path)) {
            var section = plugin.getConfig().getConfigurationSection(path);
            if (section != null) {
                for (String worldName : section.getKeys(false)) {
                    List<String> regions = section.getStringList(worldName);
                    if (!regions.isEmpty()) targetMap.put(worldName, new HashSet<>(regions));
                }
            }
        }
    }

    public void reload() { loadConfig(); }

    public boolean isFlightBlockedInWorld(Player player) {
        String worldName = player.getWorld().getName();
        if (!whitelistWorlds.isEmpty()) {
            for (String whitelistedWorld : whitelistWorlds) {
                if (matchesPattern(worldName, whitelistedWorld)) return false;
            }
            return true;
        }
        for (String blacklistedWorld : blacklistWorlds) {
            if (matchesPattern(worldName, blacklistedWorld)) return true;
        }
        return false;
    }

    private boolean matchesPattern(String worldName, String pattern) {
        if (pattern.contains("*")) {
            return worldName.matches(pattern.replace("*", ".*"));
        }
        return worldName.equalsIgnoreCase(pattern);
    }

    private void disableFlight(Player player, boolean isRegionBlock) {
        UUID uuid = player.getUniqueId();
        boolean shouldNotify = canSendMessage(uuid);

        player.setAllowFlight(false);
        player.setFlying(false);

        if (shouldNotify) {
            plugin.getSoundEffectsManager().playDeactivationEffects(player);
        }

        FlyCommand flyCommand = new FlyCommand(plugin);
        flyCommand.cleanupFlyData(player);

        if (shouldNotify) {
            sendBlockMessage(player, isRegionBlock);
            messageCooldowns.put(uuid, System.currentTimeMillis());
        }
    }

    private void sendBlockMessage(Player player, boolean isRegionBlock) {
        String messageKey;
        String fallbackMessage;

        if (isRegionBlock) {
            messageKey = "fly-not-allowed-in-region";
            fallbackMessage = "&cYou cannot fly in this region.";
        } else {
            messageKey = "fly-not-allowed-in-world";
            fallbackMessage = "&cYou cannot fly in this world.";
        }

        player.sendMessage(plugin.getPrefixedMessage(messageKey, fallbackMessage));
    }

    private boolean canSendMessage(UUID uuid) {
        Long lastMessage = messageCooldowns.get(uuid);
        return lastMessage == null || (System.currentTimeMillis() - lastMessage) >= MESSAGE_COOLDOWN_MS;
    }
}
