package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import pwf.xenova.PowerFly;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SlowMiningManager implements Listener {

    private final PowerFly plugin;

    private final Set<UUID> playersMiningSlow = new HashSet<>();

    private boolean enabled;
    private int amplifier;

    private BukkitTask task;

    public SlowMiningManager(PowerFly plugin) {
        this.plugin = plugin;
        loadConfig();
        start();
    }

    private void loadConfig() {
        enabled = plugin.getConfig().getBoolean("slow-mining.enabled", true);
        amplifier = plugin.getConfig().getInt("slow-mining.amplifier", 1);
    }

    public void reload() {
        loadConfig();

        if (!enabled) {
            stop();
            clearAll();
            return;
        }

        if (task == null) {
            start();
        }
    }

    private void start() {
        if (task != null) return;

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();

                if (player.isFlying()
                        && player.getGameMode() != GameMode.CREATIVE
                        && isPlayerMining(player)) {

                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.MINING_FATIGUE,
                            40,
                            amplifier,
                            false,
                            false,
                            false
                    ));

                } else {
                    playersMiningSlow.remove(uuid);
                }
            }
        }, 0L, 10L);
    }

    private void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();

        if (player.isFlying() && player.getGameMode() != GameMode.CREATIVE) {
            playersMiningSlow.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playersMiningSlow.remove(event.getPlayer().getUniqueId());
    }

    private boolean isPlayerMining(Player player) {
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            return false;
        }

        return playersMiningSlow.contains(player.getUniqueId());
    }

    public void removePlayer(UUID uuid) {
        playersMiningSlow.remove(uuid);
    }

    private void clearAll() {
        playersMiningSlow.clear();
    }

    public void shutdown() {
        stop();
        clearAll();
    }
}
