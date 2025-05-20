package pwf.xenova.managers;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pwf.xenova.PowerFly;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoundEffectsManager {

    private final PowerFly plugin;
    private final FileConfiguration config;
    private final Map<UUID, BukkitRunnable> activeLoops = new HashMap<>();

    // Mapeo de partículas y sonidos válidos
    private static final Map<String, Particle> PARTICLE_TYPES = new HashMap<>();
    private static final Map<String, Sound> SOUND_TYPES = new HashMap<>();

    static {
        // Partículas soportadas
        PARTICLE_TYPES.put("CLOUD", Particle.CLOUD);
        PARTICLE_TYPES.put("END_ROD", Particle.END_ROD);
        PARTICLE_TYPES.put("SMOKE", Particle.SMOKE);
        PARTICLE_TYPES.put("FLAME", Particle.FLAME);

        // Sonidos soportados
        SOUND_TYPES.put("BLOCK_BEACON_ACTIVATE", Sound.BLOCK_BEACON_ACTIVATE);
        SOUND_TYPES.put("BLOCK_BEACON_DEACTIVATE", Sound.BLOCK_BEACON_DEACTIVATE);
        SOUND_TYPES.put("BLOCK_PORTAL_TRAVEL", Sound.BLOCK_PORTAL_TRAVEL);
        SOUND_TYPES.put("ENTITY_PLAYER_LEVELUP", Sound.ENTITY_PLAYER_LEVELUP);
    }

    public SoundEffectsManager(PowerFly plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void playActivationEffects(Player player) {
        if (!config.getBoolean("enable-effects", false)) return;

        spawnParticle(player, "particles.activation");
        playSound(player, "sounds.activation");
        startFlightLoop(player);
    }

    public void playDeactivationEffects(Player player) {
        stopFlightLoop(player);

        if (!config.getBoolean("enable-effects", false)) return;

        spawnParticle(player, "particles.deactivation");
        playSound(player, "sounds.deactivation");
    }

    public void playTimeEndEffects(Player player) {
        if (!config.getBoolean("enable-sounds", false)) return;

        playSound(player, "sounds.time-ended");

        if (config.getBoolean("enable-effects", false)) {
            spawnParticle(player, "particles.deactivation");
        }
    }

    private void playSound(Player player, String path) {
        if (!config.getBoolean("enable-sounds", false)) return;

        String soundName = config.getString(path + ".type");
        if (soundName == null || !SOUND_TYPES.containsKey(soundName)) {
            plugin.getLogger().warning("Sound not found: " + soundName + " en " + path);
            return;
        }

        player.playSound(player.getLocation(), SOUND_TYPES.get(soundName), 1.0f, 1.0f);
    }

    private void spawnParticle(Player player, String path) {
        String particleName = config.getString(path + ".type");
        if (particleName == null || !PARTICLE_TYPES.containsKey(particleName)) {
            plugin.getLogger().warning("Particle not found: " + particleName + " en " + path);
            return;
        }

        Location loc = player.getLocation();
        if (isOnGround(player)) {
            loc.add(0, 0.5, 0);
        }

        player.getWorld().spawnParticle(
                PARTICLE_TYPES.get(particleName),
                loc,
                10,
                0.5,
                0.0,
                0.5,
                0.0
        );
    }

     // Inicio del bucle de partículas durante el vuelo
    private void startFlightLoop(Player player) {
        UUID playerId = player.getUniqueId();

        stopFlightLoop(player);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.getAllowFlight()) {
                    cancel();
                    activeLoops.remove(playerId);
                    return;
                }
                spawnParticle(player, "particles.flying");
            }
        };

        task.runTaskTimer(plugin, 0L, 5L);
        activeLoops.put(playerId, task);
    }

    private void stopFlightLoop(Player player) {
        BukkitRunnable task = activeLoops.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

     // Limpieza de todos los bucles al reiniciar/desactivar el plugin
    public void cleanupAllLoops() {
        activeLoops.values().forEach(BukkitRunnable::cancel);
        activeLoops.clear();
    }

    private boolean isOnGround(Player player) {
        return !player.getLocation().clone().subtract(0, 0.1, 0).getBlock().isPassable();
    }
}
