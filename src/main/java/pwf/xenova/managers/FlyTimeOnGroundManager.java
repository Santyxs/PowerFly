package pwf.xenova.managers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import pwf.xenova.PowerFly;
import pwf.xenova.commands.FlyCommand;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FlyTimeOnGroundManager implements Listener {

    private final PowerFly plugin;
    private boolean decreaseOnGround;
    private final Set<UUID> wasFlying = new HashSet<>();

    public FlyTimeOnGroundManager(PowerFly plugin) {
        this.plugin = plugin;
        reload();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void reload() {
        this.decreaseOnGround = plugin.getConfig().getBoolean("decrease-flytime-on-ground", false);
    }

    public boolean shouldDecreaseFlyTime(Player player) {
        if (decreaseOnGround) return true;
        return player.isFlying();
    }

    public boolean isDecreaseOnGroundEnabled() {
        return decreaseOnGround;
    }

    public void setDecreaseOnGround(boolean enabled) {
        this.decreaseOnGround = enabled;
    }

    public void removeWasFlying(UUID uuid) {
        wasFlying.remove(uuid);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!FlyCommand.hasPluginFlyActive(uuid)) return;
        if (plugin.getNoFallDamageSet().contains(uuid)) return;

        if (player.isFlying()) {
            wasFlying.add(uuid);
            return;
        }

        // Aterriza después de haber volado: desactivamos el vuelo completamente
        if (wasFlying.contains(uuid) && player.getFallDistance() == 0) {
            wasFlying.remove(uuid);
            plugin.getFlyCommand().disableFly(player, false);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!FlyCommand.hasPluginFlyActive(uuid)) return;

        // El jugador hace doble salto para volver a volar
        if (event.isFlying()) {
            event.setCancelled(true);
            player.setAllowFlight(true);
            player.setFlying(true);
            wasFlying.add(uuid);
        }
    }
}