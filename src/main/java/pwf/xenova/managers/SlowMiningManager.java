package pwf.xenova.managers;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pwf.xenova.PowerFly;

public class SlowMiningManager implements Listener {

    private final PowerFly plugin;

    private boolean enabled;
    private int amplifier;

    public SlowMiningManager(PowerFly plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        enabled   = plugin.getMainConfig().getBoolean("slow-mining.enabled", true);
        amplifier = plugin.getMainConfig().getInt("slow-mining.amplifier", 1);
    }

    public void reload() {
        loadConfig();

        if (!enabled) {
            clearAll();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();

        if (player.isFlying() && player.getGameMode() != GameMode.CREATIVE) {
            if (!player.hasPotionEffect(PotionEffectType.MINING_FATIGUE)) {
                applyFatigue(player);
            }
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (!event.isFlying()) {
            removeFatigue(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeFatigue(event.getPlayer());
    }

    private void applyFatigue(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.MINING_FATIGUE,
                60,
                amplifier,
                false,
                false,
                false
        ));
    }

    private void removeFatigue(Player player) {
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
    }

    private void clearAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            removeFatigue(player);
        }
    }

    public void clearAllOnDisable() {
        clearAll();
    }
}