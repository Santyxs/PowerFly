package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pwf.xenova.commands.FlyCommand;
import pwf.xenova.PowerFly;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatFlyManager implements Listener {

    private final PowerFly plugin;
    private final Map<UUID, Long> combatLog = new HashMap<>();
    private final Map<UUID, BukkitRunnable> combatTimers = new HashMap<>();

    private boolean disableFlyInCombat;
    private String combatType;
    private int combatDuration;

    public CombatFlyManager(PowerFly plugin) {
        this.plugin = plugin;
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void loadConfig() {
        this.disableFlyInCombat = plugin.getConfig().getBoolean("disable-fly-in-combat", true);
        this.combatType = plugin.getConfig().getString("combat-type", "players").toLowerCase();
        this.combatDuration = plugin.getConfig().getInt("combat-duration", 10);
    }

    public void reload() {
        loadConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!disableFlyInCombat) {
            return;
        }

        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        if (victim instanceof Player player) {
            if (shouldEnterCombat(damager)) {
                enterCombat(player);
            }
        }

        if (damager instanceof Player player) {
            if (shouldEnterCombat(victim)) {
                enterCombat(player);
            }
        }
    }

    private boolean shouldEnterCombat(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }

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

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        BukkitRunnable oldTimer = combatTimers.remove(uuid);
        if (oldTimer != null) {
            oldTimer.cancel();
        }

        long expireTime = System.currentTimeMillis() + (combatDuration * 1000L);
        combatLog.put(uuid, expireTime);

        if (player.isFlying() && FlyCommand.hasPluginFlyActive(uuid)) {
            player.setAllowFlight(false);
            player.setFlying(false);

            plugin.getSoundEffectsManager().playDeactivationEffects(player);

            FlyCommand flyCommand = new FlyCommand(plugin);
            flyCommand.cleanupFlyData(player);

            player.sendMessage(plugin.getPrefixedMessage("fly-disabled-combat", "&cFly disabled due to combat!"));
        }

        startCombatTimer(player);
    }

    private void startCombatTimer(Player player) {
        UUID uuid = player.getUniqueId();

        BukkitRunnable timer = new BukkitRunnable() {
            int remaining = combatDuration;

            public void run() {
                if (!player.isOnline()) {
                    combatTimers.remove(uuid);
                    combatLog.remove(uuid);
                    cancel();
                    return;
                }

                if (!isInCombat(uuid)) {
                    combatTimers.remove(uuid);
                    cancel();
                    return;
                }

                remaining--;

                if (remaining <= 0) {
                    combatTimers.remove(uuid);
                    combatLog.remove(uuid);

                    player.sendMessage(plugin.getPrefixedMessage("combat-ended", "&aYou are no longer in combat."));

                    cancel();
                }
            }
        };

        timer.runTaskTimer(plugin, 20L, 20L);
        combatTimers.put(uuid, timer);
    }

    public boolean isInCombat(UUID uuid) {
        if (!disableFlyInCombat) {
            return false;
        }

        Long expireTime = combatLog.get(uuid);
        if (expireTime == null) {
            return false;
        }

        if (System.currentTimeMillis() > expireTime) {
            combatLog.remove(uuid);
            BukkitRunnable timer = combatTimers.remove(uuid);
            if (timer != null) {
                timer.cancel();
            }
            return false;
        }

        return true;
    }

    public boolean isInCombat(Player player) {
        return isInCombat(player.getUniqueId());
    }

    public int getRemainingCombatTime(UUID uuid) {
        if (!isInCombat(uuid)) {
            return 0;
        }

        Long expireTime = combatLog.get(uuid);
        if (expireTime == null) {
            return 0;
        }

        long remaining = (expireTime - System.currentTimeMillis()) / 1000;
        return (int) Math.max(0, remaining);
    }

    public void cleanupPlayer(UUID uuid) {
        combatLog.remove(uuid);
        BukkitRunnable timer = combatTimers.remove(uuid);
        if (timer != null) {
            timer.cancel();
        }
    }

    public void cleanup() {
        for (BukkitRunnable timer : combatTimers.values()) {
            if (timer != null) {
                timer.cancel();
            }
        }
        combatTimers.clear();
        combatLog.clear();
    }
}
