package pwf.xenova.managers;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import pwf.xenova.PowerFly;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NoFallDamageManager implements Listener {

    private final PowerFly plugin;

    private boolean enabled;
    private double blocks;

    private final Map<UUID, Double> remainingBlocks = new ConcurrentHashMap<>();

    public NoFallDamageManager(PowerFly plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        enabled = plugin.getMainConfig().getBoolean("no-fall-damage.enabled", false);
        blocks = plugin.getMainConfig().getInt("no-fall-damage.blocks", 10);
    }

    public void reload() {
        loadConfig();
        if (!enabled) {
            remainingBlocks.clear();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void grantProtection(UUID uuid) {
        if (!enabled || blocks <= 0) return;
        remainingBlocks.put(uuid, blocks);
    }

    public boolean hasProtection(UUID uuid) {
        Double remaining = remainingBlocks.get(uuid);
        return remaining != null && remaining > 0;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        UUID uuid = player.getUniqueId();
        Double protectedBlocks = remainingBlocks.get(uuid);

        if (protectedBlocks != null) {
            float fallDistance = player.getFallDistance();
            if (fallDistance <= protectedBlocks) {
                event.setCancelled(true);
                double left = protectedBlocks - fallDistance;
                if (left <= 0) remainingBlocks.remove(uuid);
                else remainingBlocks.put(uuid, left);
            } else {
                remainingBlocks.remove(uuid);
                double excessBlocks = fallDistance - protectedBlocks;
                double damage = calculateFallDamage(player, excessBlocks);
                if (damage <= 0) event.setCancelled(true);
                else event.setDamage(damage);
            }
            return;
        }

        if (plugin.getFlyRuntimeManager().hasActiveSession(uuid)) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        remainingBlocks.remove(event.getPlayer().getUniqueId());
    }

    public void clearAllOnDisable() {
        remainingBlocks.clear();
    }

    private double calculateFallDamage(Player player, double blocksFallen) {
        if (blocksFallen <= 3) return 0;

        double damage = blocksFallen - 3;
        ItemStack boots = player.getInventory().getBoots();

        if (boots != null && boots.hasItemMeta()) {
            int featherFallingLevel = boots.getEnchantmentLevel(Enchantment.FEATHER_FALLING);
            if (featherFallingLevel > 0) {
                damage *= (1.0 - featherFallingLevel * 0.12);
            }
        }

        return Math.max(0, damage);
    }
}