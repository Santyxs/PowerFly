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

        var config = plugin.getFileManager().getConfig();

        blacklistWorlds.addAll(config.getStringList("blacklist-worlds"));
        whitelistWorlds.addAll(config.getStringList("whitelist-worlds"));

        var blacklistRegionsSection = config.getSection("blacklist-regions");
        if (blacklistRegionsSection != null) {
            for (Object world : blacklistRegionsSection.getKeys()) {
                String worldName = String.valueOf(world);
                blacklistRegions.put(worldName, new HashSet<>(blacklistRegionsSection.getStringList(worldName)));
            }
        }

        var whitelistRegionsSection = config.getSection("whitelist-regions");
        if (whitelistRegionsSection != null) {
            for (Object world : whitelistRegionsSection.getKeys()) {
                String worldName = String.valueOf(world);
                whitelistRegions.put(worldName, new HashSet<>(whitelistRegionsSection.getStringList(worldName)));
            }
        }
    }

    public void reload() {
        loadConfig();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
                    player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                continue;
            }

            if (FlyCommand.hasPluginFlyActive(player.getUniqueId())) continue;
            if (isFlightBlocked(player)) {
                boolean inRegion = isFlightBlockedInRegion(player);
                disableFlight(player, true, inRegion);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
                player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }

        if (!player.isFlying() && !player.getAllowFlight()) return;
        if (FlyCommand.hasPluginFlyActive(player.getUniqueId())) return;
        if (isFlightBlocked(player)) {
            boolean inRegion = isFlightBlockedInRegion(player);
            disableFlight(player, false, inRegion);
        }
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

    private void disableFlight(Player player, boolean forceMessage, boolean isRegionBlock) {
        UUID uuid = player.getUniqueId();
        boolean shouldNotify = forceMessage || canSendMessage(uuid);

        player.setAllowFlight(false);
        player.setFlying(false);

        if (shouldNotify) plugin.getSoundEffectsManager().playDeactivationEffects(player);

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
}
