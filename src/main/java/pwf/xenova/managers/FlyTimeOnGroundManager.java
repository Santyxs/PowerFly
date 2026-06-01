package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import pwf.xenova.utils.WorldGuardFlags;
import pwf.xenova.PowerFly;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlyTimeOnGroundManager implements Listener {

    private final PowerFly plugin;
    private boolean decreaseOnGround;
    private int minFallBlocks;

    private final Map<UUID, Double> fallStartY = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> wasFalling = new ConcurrentHashMap<>();

    public FlyTimeOnGroundManager(PowerFly plugin) {
        this.plugin = plugin;
        reload();

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {

                if (!player.isOnline() || player.isDead()) continue;
                if (player.isGliding()) continue;

                UUID uuid = player.getUniqueId();

                if (plugin.getMainConfig().getBoolean("no-fall-damage", false)) continue;
                if (WorldGuardFlags.isFallDamageDenied(player)) continue;

                boolean isFalling = player.getFallDistance() > 0;
                boolean justLanded = !isFalling && wasFalling.getOrDefault(uuid, false);

                if (justLanded) {
                    Double startY = fallStartY.remove(uuid);
                    if (startY == null) continue;

                    double currentY = player.getLocation().getY();
                    double calculatedFall = startY - currentY;
                    float vanillaFall = player.getFallDistance();
                    int blocksFallen = (int) Math.round(Math.max(calculatedFall, vanillaFall));

                    checkAndApplyFallDamage(player, blocksFallen);
                }

                wasFalling.put(uuid, isFalling);
            }
        }, 0L, 2L);
    }

    public void reload() {
        this.decreaseOnGround = plugin.getFileManager().getConfig().getBoolean("decrease-flytime-on-ground", false);
        this.minFallBlocks = plugin.getMainConfig().getInt("fall-damage.min-blocks", 4);
    }

    public boolean shouldDecreaseFlyTime(Player player) {
        return player.isFlying() || decreaseOnGround;
    }

    public boolean isDecreaseOnGroundEnabled() {
        return decreaseOnGround;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!plugin.getFlyRuntimeManager().hasActiveSession(uuid)) return;
        if (plugin.getNoFallDamageSet().contains(uuid)) return;
        if (plugin.getMainConfig().getBoolean("no-fall-damage", false)) return;
        if (WorldGuardFlags.isFallDamageDenied(player)) return;
        if (!event.hasChangedPosition()) return;

        double currentY = player.getLocation().getY();

        if (player.isFlying()) {
            fallStartY.put(uuid, currentY);
            wasFalling.put(uuid, false);
            return;
        }

        if (player.isGliding()) {
            fallStartY.remove(uuid);
            wasFalling.put(uuid, false);
            return;
        }

        fallStartY.merge(uuid, currentY, Math::max);

        boolean isFalling = player.getFallDistance() > 0;
        boolean justLanded = !isFalling && wasFalling.getOrDefault(uuid, false);

        wasFalling.put(uuid, isFalling);

        if (justLanded) {
            Double startY = fallStartY.remove(uuid);
            if (startY == null) return;

            double calculatedFall = startY - currentY;
            float vanillaFall = player.getFallDistance();
            int blocksFallen = (int) Math.round(Math.max(calculatedFall, vanillaFall));

            checkAndApplyFallDamage(player, blocksFallen);
        }
    }

    private void checkAndApplyFallDamage(Player player, int blocksFallen) {
        if (blocksFallen < minFallBlocks) return;

        double damage = blocksFallen - 3;

        ItemStack boots = player.getInventory().getBoots();

        if (boots != null && boots.hasItemMeta()) {
            int featherFallingLevel = boots.getEnchantmentLevel(Enchantment.FEATHER_FALLING);

            if (featherFallingLevel > 0) {
                double reduction = featherFallingLevel * 0.12;
                damage = damage * (1.0 - reduction);
            }
        }

        if (damage <= 0) return;

        final double finalDamage = damage;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && !player.isDead()) {
                player.damage(finalDamage);
            }
        });
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        fallStartY.remove(uuid);
        wasFalling.remove(uuid);
        event.getPlayer().setFallDistance(0);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!plugin.getFlyRuntimeManager().hasActiveSession(uuid)) return;

        if (!event.isFlying()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && plugin.getFlyRuntimeManager().hasActiveSession(uuid)) {
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