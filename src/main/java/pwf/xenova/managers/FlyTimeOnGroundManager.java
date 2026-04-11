package pwf.xenova.managers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import pwf.xenova.PowerFly;
import pwf.xenova.commands.FlyCommand;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class FlyTimeOnGroundManager implements Listener {

    private final PowerFly plugin;
    private boolean decreaseOnGround;

    private final Map<UUID, Double> fallStartY = new HashMap<>();
    private final Map<UUID, Boolean> wasFalling = new HashMap<>();

    public FlyTimeOnGroundManager(PowerFly plugin) {
        this.plugin = plugin;
        reload();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : new HashSet<>(fallStartY.keySet())) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    fallStartY.remove(uuid);
                    wasFalling.remove(uuid);
                    continue;
                }

                if (!FlyCommand.hasPluginFlyActive(uuid)) continue;
                if (plugin.getNoFallDamageSet().contains(uuid)) continue;
                if (player.isFlying()) continue;

                boolean isFalling = player.getFallDistance() > 0;
                boolean justLanded = !isFalling && wasFalling.getOrDefault(uuid, false);
                wasFalling.put(uuid, isFalling);

                if (justLanded) {
                    double currentY = player.getLocation().getY();
                    double startY = fallStartY.remove(uuid);
                    double calculatedFall = startY - currentY;

                    if (calculatedFall > 3) {
                        double damage = (calculatedFall - 3) * 0.5;
                        player.damage(damage);
                    }
                }
            }
        }, 0L, 2L);
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!FlyCommand.hasPluginFlyActive(uuid)) return;
        if (plugin.getNoFallDamageSet().contains(uuid)) return;

        if (event.getFrom().getY() == event.getTo().getY() && !player.isFlying()) return;

        double currentY = player.getLocation().getY();

        if (player.isFlying()) {
            fallStartY.put(uuid, currentY);
            wasFalling.put(uuid, false);
            return;
        }

        fallStartY.merge(uuid, currentY, Math::max);

        boolean isFalling = player.getFallDistance() > 0;
        boolean justLanded = !isFalling && wasFalling.getOrDefault(uuid, false);
        wasFalling.put(uuid, isFalling);

        if (justLanded && fallStartY.containsKey(uuid)) {
            double startY = fallStartY.remove(uuid);
            double calculatedFall = startY - currentY;

            if (calculatedFall > 3) {
                double damage = (calculatedFall - 3) * 0.5;
                player.damage(damage);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!FlyCommand.hasPluginFlyActive(uuid)) return;

        if (!event.isFlying()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (FlyCommand.hasPluginFlyActive(uuid) && player.isOnline()) {
                    player.setAllowFlight(true);
                }
            }, 3L);
        } else {
            fallStartY.remove(uuid);
            wasFalling.remove(uuid);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        fallStartY.remove(uuid);
        wasFalling.remove(uuid);
    }
}