package pwf.xenova.managers;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlock;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import pwf.xenova.PowerFly;
import pwf.xenova.commands.FlyCommand;

public class ClaimFlyManager implements Listener {

    private final PowerFly plugin;
    private boolean gpEnabled;
    private boolean townyEnabled;
    private boolean onlyFlyInClaims;

    public ClaimFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.gpEnabled = Bukkit.getPluginManager().isPluginEnabled("GriefPrevention") &&
                plugin.getFileManager().getConfig().getBoolean("enable-griefprevention", true);

        this.townyEnabled = Bukkit.getPluginManager().isPluginEnabled("Towny") &&
                plugin.getFileManager().getConfig().getBoolean("enable-towny", true);

        this.onlyFlyInClaims = plugin.getFileManager().getConfig().getBoolean("only-fly-in-claims", false);

        plugin.getLogger().info("GriefPrevention: " + (gpEnabled ? "enabled" : "disabled"));
        plugin.getLogger().info("Towny: " + (townyEnabled ? "enabled" : "disabled"));
        plugin.getLogger().info("Only fly in claims: " + onlyFlyInClaims);
    }

    private boolean isInOwnGPClaim(Player player, Location location) {
        if (!gpEnabled) return false;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
        if (claim == null) return false;
        return claim.hasExplicitPermission(player, ClaimPermission.Build);
    }

    private boolean isInForeignGPClaim(Player player, Location location) {
        if (!gpEnabled) return false;
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
        if (claim == null) return false;
        return !claim.hasExplicitPermission(player, ClaimPermission.Build);
    }

    private boolean isInAnyGPClaim(Location location) {
        if (!gpEnabled) return false;
        return GriefPrevention.instance.dataStore.getClaimAt(location, false, null) != null;
    }

    private boolean isInOwnTownyClaim(Player player, Location location) {
        if (!townyEnabled) return false;
        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
        if (townBlock == null) return false;
        try {
            return townBlock.getTown().hasResident(player.getName());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isInForeignTownyClaim(Player player, Location location) {
        if (!townyEnabled) return false;
        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
        if (townBlock == null) return false;
        try {
            return !townBlock.getTown().hasResident(player.getName());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isInAnyTownyClaim(Location location) {
        if (!townyEnabled) return false;
        return TownyAPI.getInstance().getTownBlock(location) != null;
    }

    public boolean cannotFlyHere(Player player, Location location) {
        if (onlyFlyInClaims) {
            return cannotFlyHereStrict(player, location);
        }
        return isInForeignGPClaim(player, location) || isInForeignTownyClaim(player, location);
    }

    private boolean cannotFlyHereStrict(Player player, Location location) {
        if (!gpEnabled && !townyEnabled) return false;

        boolean allowedByGP    = gpEnabled    && isInOwnGPClaim(player, location);
        boolean allowedByTowny = townyEnabled && isInOwnTownyClaim(player, location);

        return !allowedByGP && !allowedByTowny;
    }

    private boolean isInForeignClaim(Player player, Location location) {
        if (gpEnabled && isInAnyGPClaim(location) && !isInOwnGPClaim(player, location)) return true;
        return townyEnabled && isInAnyTownyClaim(location) && !isInOwnTownyClaim(player, location);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!FlyCommand.hasPluginFlyActive(player.getUniqueId())) return;
        if (!player.isFlying() || !player.getAllowFlight()) return;
        if (player.hasPermission("powerfly.admin")) return;

        Location from = event.getFrom();
        Location to   = event.getTo();

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        if (cannotFlyHere(player, to)) {
            player.setFlying(false);
            player.setAllowFlight(false);
            sendClaimFlyMessage(player, to);
        }
    }

    public void sendClaimFlyMessage(Player player) {
        sendClaimFlyMessage(player, player.getLocation());
    }

    public void sendClaimFlyMessage(Player player, Location location) {
        if (onlyFlyInClaims && isInForeignClaim(player, location)) {
            player.sendMessage(plugin.getPrefixedMessage(
                    "fly-not-allowed-in-claim",
                    "&cYou cannot fly in this claim."));
        } else if (onlyFlyInClaims) {
            player.sendMessage(plugin.getPrefixedMessage(
                    "fly-not-allowed-outside-claim",
                    "&cYou can only fly inside claims."));
        } else {
            player.sendMessage(plugin.getPrefixedMessage(
                    "fly-not-allowed-in-claim",
                    "&cYou cannot fly in this claim."));
        }
    }
}