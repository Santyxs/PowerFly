package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import pwf.xenova.PowerFly;

public class ClaimFlyManager implements Listener {

    private final PowerFly plugin;
    private boolean gpEnabled;
    private boolean townyEnabled;
    private Object townyAPI;

    public ClaimFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        reload();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reload() {
        this.gpEnabled = Bukkit.getPluginManager().isPluginEnabled("GriefPrevention") &&
                plugin.getConfig().getBoolean("enable-griefprevention", true);

        this.townyEnabled = Bukkit.getPluginManager().isPluginEnabled("Towny") &&
                plugin.getConfig().getBoolean("enable-towny", true);

        if (townyEnabled) {
            try {
                Class<?> townyAPIClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
                townyAPI = townyAPIClass.getMethod("getInstance").invoke(null);
            } catch (Exception e) {
                plugin.getLogger().warning("Towny is enabled but API could not be loaded: " + e.getMessage());
                townyEnabled = false;
            }
        }

        plugin.getLogger().info("GriefPrevention: " + (gpEnabled ? "enabled" : "disabled"));
        plugin.getLogger().info("Towny: " + (townyEnabled ? "enabled" : "disabled"));
    }

    private boolean cannotFlyGP(Player player, Location location) {
        if (!gpEnabled) return false;

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
        if (claim == null) return false;

        return !claim.hasExplicitPermission(player, ClaimPermission.Build);
    }

    private boolean cannotFlyTowny(Player player, Location location) {
        if (!townyEnabled || townyAPI == null) return false;

        try {
            Class<?> townyAPIClass = townyAPI.getClass();
            Object townBlock = townyAPIClass.getMethod("getTownBlock", Location.class).invoke(townyAPI, location);

            if (townBlock == null) return false;

            Object town = townBlock.getClass().getMethod("getTown").invoke(townBlock);
            boolean hasResident = (boolean) town.getClass()
                    .getMethod("hasResident", String.class)
                    .invoke(town, player.getName());

            return !hasResident;

        } catch (Exception e) {
            return false;
        }
    }

    public boolean cannotFlyHere(Player player, Location location) {
        return cannotFlyGP(player, location) || cannotFlyTowny(player, location);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isFlying() || !player.getAllowFlight()) return;

        Location to = event.getTo();
        if (cannotFlyHere(player, to)) {
            player.setFlying(false);
            player.setAllowFlight(false);
            sendClaimFlyMessage(player);
        }
    }

    public void sendClaimFlyMessage(Player player) {
        player.sendMessage(plugin.getPrefixedMessage("fly-not-allowed-in-claim", "&cYou cannot fly in this claim."));
    }
}