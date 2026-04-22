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

    public ClaimFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.gpEnabled = Bukkit.getPluginManager().isPluginEnabled("GriefPrevention") &&
                plugin.getConfig().getBoolean("enable-griefprevention", true);

        this.townyEnabled = Bukkit.getPluginManager().isPluginEnabled("Towny") &&
                plugin.getConfig().getBoolean("enable-towny", true);

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
        if (!townyEnabled) return false;

        TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
        if (townBlock == null) return false;

        try {
            return !townBlock.getTown().hasResident(player.getName());
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

        if (!FlyCommand.hasPluginFlyActive(player.getUniqueId())) return;
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