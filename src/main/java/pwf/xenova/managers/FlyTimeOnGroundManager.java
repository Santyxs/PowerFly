package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Waterlogged;
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
    private boolean decreaseInOwnClaims;

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

                if (plugin.getNoFallDamageManager().hasProtection(uuid)) continue;
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
        this.minFallBlocks    = plugin.getMainConfig().getInt("fall-damage.min-blocks", 4);
        this.decreaseInOwnClaims = plugin.getMainConfig().getBoolean("decrease-flytime-in-own-claims", false);

        plugin.getLogger().info("Decrease fly time on ground: " + (isDecreaseOnGroundEnabled() ? "enabled" : "disabled"));
    }

    public boolean shouldDecreaseFlyTime(Player player) {
        if (!player.isFlying() && !decreaseOnGround) return false;

        if (decreaseInOwnClaims) {
            try {
                if (plugin.getClaimFlyManager().isInOwnClaim(player, player.getLocation())) {
                    return false;
                }
            } catch (Throwable ignored) {}
        }

        return true;
    }

    public boolean isDecreaseOnGroundEnabled() {
        return decreaseOnGround;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!plugin.getFlyRuntimeManager().hasActiveSession(uuid)) return;
        if (plugin.getNoFallDamageManager().hasProtection(uuid)) return;
        if (WorldGuardFlags.isFallDamageDenied(player)) return;
        if (!event.hasChangedPosition()) return;

        double currentY = event.getTo().getY();

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
        if (plugin.getNoFallDamageManager().isEnabled()) {
            blocksFallen -= (int) Math.round(plugin.getNoFallDamageManager().getBlocks());
        }

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
            if (!player.isOnline() || player.isDead()) return;

            double adjustedDamage = applyLandingModifiers(player, finalDamage, player.getLocation());
            if (adjustedDamage > 0) {
                player.damage(adjustedDamage);
            }
        });
    }

    private double applyLandingModifiers(Player player, double damage, Location landingLocation) {
        Block feetBlock = landingLocation.getBlock();
        Block belowBlock = feetBlock.getRelative(BlockFace.DOWN);

        if (isWaterLike(feetBlock) || isWaterLike(belowBlock)) return 0;

        Material feetType = feetBlock.getType();
        if (feetType == Material.COBWEB || feetType == Material.POWDER_SNOW || isClimbable(feetType)) {
            return 0;
        }

        Material belowType = belowBlock.getType();

        if (belowType == Material.SLIME_BLOCK) {
            return player.isSneaking() ? damage : 0;
        }

        if (belowType == Material.HAY_BLOCK) {
            return damage * 0.2;
        }

        return damage;
    }

    private boolean isWaterLike(Block block) {
        if (block.getType() == Material.WATER) return true;
        return block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
    }

    private boolean isClimbable(Material material) {
        return switch (material) {
            case LADDER, VINE, WEEPING_VINES, WEEPING_VINES_PLANT,
                 TWISTING_VINES, TWISTING_VINES_PLANT, CAVE_VINES,
                 CAVE_VINES_PLANT, SCAFFOLDING -> true;
            default -> false;
        };
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