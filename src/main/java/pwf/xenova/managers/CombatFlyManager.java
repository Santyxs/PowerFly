package pwf.xenova.managers;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import pwf.xenova.PowerFly;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatFlyManager implements Listener {

    private final PowerFly plugin;
    private final Map<UUID, Long> combatLog = new HashMap<>();

    public CombatFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        if (!plugin.getConfig().getBoolean("disable-fly-in-combat", true)) return;

        String combatType = plugin.getConfig().getString("combat-type", "players").toLowerCase();
        Entity damager = event.getDamager();

        boolean pvp = damager instanceof Player;
        boolean pve = damager instanceof LivingEntity && !(damager instanceof Player);

        if (combatType.equals("players") && !pvp) return;
        if (combatType.equals("mobs") && !pve) return;

        long combatDurationMillis = plugin.getConfig().getLong("combat-duration", 7000);
        long expireTime = System.currentTimeMillis() + combatDurationMillis;

        combatLog.put(victim.getUniqueId(), expireTime);
        disableFly(victim);

        if (damager instanceof Player attacker) {
            combatLog.put(attacker.getUniqueId(), expireTime);
            disableFly(attacker);
        }
    }

    private void disableFly(Player player) {
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    public boolean isInCombat(Player player) {
        UUID uuid = player.getUniqueId();
        Long expireTime = combatLog.get(uuid);
        if (expireTime == null) return false;

        if (System.currentTimeMillis() > expireTime) {
            combatLog.remove(uuid);

            if (plugin.getFlyTimeManager().getRemainingFlyTime(uuid) > 0) {
                player.setAllowFlight(true);
            }

            return false;
        }

        return true;
    }

    public void cleanupPlayer(UUID uuid) {
        combatLog.remove(uuid);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!player.isFlying()) return;
        if (!isInCombat(player)) return;

        player.setFlying(false);
    }
}
