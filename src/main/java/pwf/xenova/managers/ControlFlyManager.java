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
    private final Set<String> blacklistWorlds = new HashSet<>();
    private final Map<String, Set<String>> blacklistRegions = new HashMap<>();
    private final Map<UUID, Long> messageCooldowns = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 3000;

    public ControlFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadConfig() {
        blacklistWorlds.clear();
        blacklistRegions.clear();

        List<String> worlds = plugin.getConfig().getStringList("blacklist-worlds");
        blacklistWorlds.addAll(worlds);

        var regionsSection = plugin.getConfig().getConfigurationSection("blacklist-regions");
        if (regionsSection != null) {
            for (String world : regionsSection.getKeys(false)) {
                blacklistRegions.put(world, new HashSet<>(regionsSection.getStringList(world)));
            }
        }
    }

    public void reload() {
        loadConfig();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (FlyCommand.hasPluginFlyActive(player.getUniqueId())) continue;

            if (isFlightBlockedInWorld(player.getWorld())) {
                disableFlight(player, "blacklist-worlds", "&cYou cannot fly in this world.", true);
            } else if (isFlightBlockedInRegion(player)) {
                disableFlight(player, "fly-not-allowed-in-region", "&cYou cannot fly in this region.", true);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!player.isFlying() && !player.getAllowFlight()) return;

        if (FlyCommand.hasPluginFlyActive(uuid)) return;

        if (isFlightBlockedInWorld(player.getWorld())) {
            disableFlight(player, "blacklist-worlds", "&cYou cannot fly in this world.", false);
            return;
        }

        if (isFlightBlockedInRegion(player)) {
            disableFlight(player, "fly-not-allowed-in-region", "&cYou cannot fly in this region.", false);
        }
    }

    private void disableFlight(Player player, String messageKey, String defaultMessage, boolean forceMessage) {
        UUID uuid = player.getUniqueId();

        boolean shouldNotify = forceMessage || canSendMessage(uuid);

        player.setAllowFlight(false);
        player.setFlying(false);

        if (shouldNotify) {
            plugin.getSoundEffectsManager().playDeactivationEffects(player);
        }

        FlyCommand flyCommand = new FlyCommand(plugin);
        flyCommand.cleanupFlyData(player);

        if (shouldNotify) {
            String message = plugin.getMessages().getString(messageKey, defaultMessage);
            String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
            player.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
            messageCooldowns.put(uuid, System.currentTimeMillis());
        }
    }

    private boolean canSendMessage(UUID uuid) {
        Long lastMessage = messageCooldowns.get(uuid);
        if (lastMessage == null) return true;
        return (System.currentTimeMillis() - lastMessage) >= MESSAGE_COOLDOWN_MS;
    }

    public boolean isFlightBlockedInWorld(World world) {
        return blacklistWorlds.contains(world.getName());
    }

    public boolean isFlightBlockedInRegion(Player player) {
        World world = player.getWorld();
        Set<String> blockedRegions = blacklistRegions.get(world.getName());
        if (blockedRegions == null || blockedRegions.isEmpty()) return false;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) return false;

        var vector = com.sk89q.worldedit.math.BlockVector3.at(
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ()
        );

        ApplicableRegionSet regions = manager.getApplicableRegions(vector);
        for (ProtectedRegion region : regions) {
            if (blockedRegions.contains(region.getId())) return true;
        }
        return false;
    }

    public void removePlayerCooldown(UUID uuid) {
        messageCooldowns.remove(uuid);
    }
}