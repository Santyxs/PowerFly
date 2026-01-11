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

                plugin.getLogger().info("WorldGuard: enabled.");
            } catch (Exception e) {
                plugin.getLogger().warning("Could not initialize WorldGuard hook: " + e.getMessage());
                this.worldGuardEnabled = false;
            }
        }
    }

    public void loadConfig() {
        blacklistWorlds.clear();
        whitelistWorlds.clear();
        blacklistRegions.clear();
        whitelistRegions.clear();

        List<String> blacklistWorldsConfig = plugin.getConfig().getStringList("blacklist-worlds");
        if (!blacklistWorldsConfig.isEmpty()) {
            blacklistWorlds.addAll(blacklistWorldsConfig);
        }

        List<String> whitelistWorldsConfig = plugin.getConfig().getStringList("whitelist-worlds");
        if (!whitelistWorldsConfig.isEmpty()) {
            whitelistWorlds.addAll(whitelistWorldsConfig);
        }

        if (plugin.getConfig().isConfigurationSection("blacklist-regions")) {
            var section = plugin.getConfig().getConfigurationSection("blacklist-regions");
            if (section != null) {
                for (String worldName : section.getKeys(false)) {
                    List<String> regions = section.getStringList(worldName);
                    if (!regions.isEmpty()) {
                        blacklistRegions.put(worldName, new HashSet<>(regions));
                    }
                }
            }
        }

        if (plugin.getConfig().isConfigurationSection("whitelist-regions")) {
            var section = plugin.getConfig().getConfigurationSection("whitelist-regions");
            if (section != null) {
                for (String worldName : section.getKeys(false)) {
                    List<String> regions = section.getStringList(worldName);
                    if (!regions.isEmpty()) {
                        whitelistRegions.put(worldName, new HashSet<>(regions));
                    }
                }
            }
        }
    }

    public void reload() {
        loadConfig();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!player.isFlying()) {
            return;
        }

        if (player.hasPermission("powerfly.admin")) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
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

    public boolean isFlightBlockedInWorld(Player player) {
        String worldName = player.getWorld().getName();

        if (!whitelistWorlds.isEmpty()) {
            for (String whitelistedWorld : whitelistWorlds) {
                if (matchesPattern(worldName, whitelistedWorld)) {
                    return false;
                }
            }
            return true;
        }

        for (String blacklistedWorld : blacklistWorlds) {
            if (matchesPattern(worldName, blacklistedWorld)) {
                return true;
            }
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
        if (!worldGuardEnabled || regionContainer == null || vectorClass == null || atMethod == null) {
            return false;
        }

        try {
            World world = player.getWorld();
            Location loc = player.getLocation();

            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object adaptedWorld = bukkitAdapterClass.getMethod("adapt", World.class).invoke(null, world);

            Class<?> wgWorldClass = Class.forName("com.sk89q.worldguard.world.World");
            Method getMethod = regionContainer.getClass().getMethod("get", wgWorldClass);
            Object regionManager = getMethod.invoke(regionContainer, adaptedWorld);

            if (regionManager == null) return false;

            Object vector = atMethod.invoke(null, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

            Method applicableRegionsMethod = null;
            for (Method method : regionManager.getClass().getMethods()) {
                if (method.getName().equals("getApplicableRegions") &&
                        method.getParameterCount() == 1) {
                    applicableRegionsMethod = method;
                    break;
                }
            }

            if (applicableRegionsMethod == null) return false;

            Object applicableRegions = applicableRegionsMethod.invoke(regionManager, vector);

            Iterable<?> regionsIterable = (Iterable<?>) applicableRegions;

            Set<String> whitelisted = whitelistRegions.get(world.getName());
            if (whitelisted != null && !whitelisted.isEmpty()) {
                for (Object region : regionsIterable) {
                    String id = (String) region.getClass().getMethod("getId").invoke(region);
                    if (whitelisted.contains(id)) {
                        return false;
                    }
                }
            }

            Set<String> blacklisted = blacklistRegions.get(world.getName());
            if (blacklisted != null && !blacklisted.isEmpty()) {
                for (Object region : regionsIterable) {
                    String id = (String) region.getClass().getMethod("getId").invoke(region);
                    if (blacklisted.contains(id)) {
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error checking regions: " + e.getMessage());
            return false;
        }

        return false;
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

    public void removePlayerCooldown(UUID uuid) {
        messageCooldowns.remove(uuid);
    }

    public List<String> getBlacklistWorlds() {
        return new ArrayList<>(blacklistWorlds);
    }

    public List<String> getWhitelistWorlds() {
        return new ArrayList<>(whitelistWorlds);
    }

    public Map<String, Set<String>> getBlacklistRegions() {
        return new HashMap<>(blacklistRegions);
    }

    public Map<String, Set<String>> getWhitelistRegions() {
        return new HashMap<>(whitelistRegions);
    }
}
