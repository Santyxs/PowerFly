package pwf.xenova.managers;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pwf.xenova.PowerFly;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatFlyManager implements Listener {

    private final PowerFly plugin;
    private final Map<UUID, Long> combatExpiry = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> combatTimers = new ConcurrentHashMap<>();

    private boolean disableFlyInCombat;
    private String combatType;
    private int combatDuration;

    public CombatFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        this.disableFlyInCombat = plugin.getMainConfig().getBoolean("disable-fly-in-combat", true);
        this.combatType = plugin.getMainConfig().getString("combat-type", "players").toLowerCase();
        this.combatDuration = plugin.getMainConfig().getInt("combat-duration", 10);
    }

    public void reload() {
        loadConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!disableFlyInCombat) return;

        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        if (victim instanceof Player player && shouldEnterCombat(damager)) {
            enterCombat(player);
        }

        if (damager instanceof Player player && shouldEnterCombat(victim)) {
            enterCombat(player);
        }
    }

    private boolean shouldEnterCombat(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;

        return switch (combatType) {
            case "players" -> entity instanceof Player;
            case "mobs" -> !(entity instanceof Player);
            case "all" -> true;
            default -> {
                plugin.getLogger().warning("Invalid combat-type: " + combatType + ". Using 'players' as default.");
                yield entity instanceof Player;
            }
        };
    }

    private void enterCombat(Player player) {
        UUID uuid = player.getUniqueId();

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        cancelTimer(uuid);

        long expireTime = System.currentTimeMillis() + (combatDuration * 1000L);
        combatExpiry.put(uuid, expireTime);

        if (player.isFlying() && plugin.getFlyRuntimeManager().hasActiveSession(uuid)) {
            player.setAllowFlight(false);
            player.setFlying(false);

            plugin.getSoundEffectsManager().playDeactivationEffects(player);
            plugin.getFlyCommand().cleanupFlyData(player);

            player.sendMessage(plugin.getPrefixedMessage("fly-disabled-combat", "&cFly disabled due to combat!"));
        }

        startCombatTimer(player, expireTime);
    }

    private void startCombatTimer(Player player, long expireTime) {
        UUID uuid = player.getUniqueId();

        BukkitRunnable timer = new BukkitRunnable() {
            public void run() {
                if (!player.isOnline()) {
                    cleanupPlayer(uuid);
                    cancel();
                    return;
                }

                if (System.currentTimeMillis() >= expireTime) {
                    cleanupPlayer(uuid);
                    player.sendMessage(plugin.getPrefixedMessage("combat-ended", "&aYou are no longer in combat."));
                    cancel();
                }
            }
        };

        timer.runTaskTimer(plugin, 20L, 20L);
        combatTimers.put(uuid, timer);
    }

    public boolean isInCombat(UUID uuid) {
        if (!disableFlyInCombat) return false;

        Long expireTime = combatExpiry.get(uuid);
        if (expireTime == null) return false;

        return System.currentTimeMillis() < expireTime;
    }

    public boolean isInCombat(Player player) {
        return isInCombat(player.getUniqueId());
    }

    public int getRemainingCombatTime(UUID uuid) {
        if (!isInCombat(uuid)) return 0;

        Long expireTime = combatExpiry.get(uuid);
        if (expireTime == null) return 0;

        long remaining = (expireTime - System.currentTimeMillis()) / 1000;
        return (int) Math.max(0, remaining);
    }

    public void cleanupPlayer(UUID uuid) {
        combatExpiry.remove(uuid);
        cancelTimer(uuid);
    }

    public void cleanup() {
        combatTimers.values().forEach(BukkitRunnable::cancel);
        combatTimers.clear();
        combatExpiry.clear();
    }

    private void cancelTimer(UUID uuid) {
        BukkitRunnable timer = combatTimers.remove(uuid);
        if (timer != null) timer.cancel();
    }
}