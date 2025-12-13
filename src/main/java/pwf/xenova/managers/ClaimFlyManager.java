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
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import pwf.xenova.PowerFly;

public class ClaimFlyManager implements Listener {

    private final PowerFly plugin;
    private boolean gpEnabled;
    private boolean townyEnabled;

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

        plugin.getLogger().info("ClaimFlyManager reloaded. (GP: " + gpEnabled + ", Towny: " + townyEnabled + ")");
    }

    private boolean cannotFlyGP(Player player, Location location) {
        if (!gpEnabled) return false;

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
        if (claim == null) return false;

        return !claim.hasExplicitPermission(player, ClaimPermission.Build);
    }

    private boolean cannotFlyTowny(Player player, Location location) {
        if (!townyEnabled) return false;

        TownyAPI api = TownyAPI.getInstance();
        TownBlock townBlock = api.getTownBlock(location);
        if (townBlock == null) return false;

        try {
            Town town = townBlock.getTown();
            return !town.hasResident(player.getName());
        } catch (NotRegisteredException e) {
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
        player.sendMessage(plugin.getPrefixedMessage("fly-not-allowed-in-claim", "&cYou cannot fly in this claim."
        ));
    }
}