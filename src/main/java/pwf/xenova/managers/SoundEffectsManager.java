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

    private final Map<UUID, BukkitRunnable> flyingLoops = new HashMap<>();

    private static final Map<String, Particle> PARTICLES = new HashMap<>();
    private static final Map<String, Sound> SOUNDS = new HashMap<>();

    static {
        // Partículas válidas
        PARTICLES.put("CLOUD", Particle.CLOUD);
        PARTICLES.put("END_ROD", Particle.END_ROD);
        PARTICLES.put("FLAME", Particle.FLAME);
        PARTICLES.put("SMOKE", Particle.SMOKE);

        // Sonidos válidos
        SOUNDS.put("ENTITY_PLAYER_LEVELUP", Sound.ENTITY_PLAYER_LEVELUP);
        SOUNDS.put("ENTITY_ENDER_DRAGON_FLAP", Sound.ENTITY_ENDER_DRAGON_FLAP);
        SOUNDS.put("BLOCK_NOTE_BLOCK_PLING", Sound.BLOCK_NOTE_BLOCK_PLING);
        SOUNDS.put("BLOCK_BEACON_ACTIVATE", Sound.BLOCK_BEACON_ACTIVATE);
        SOUNDS.put("BLOCK_BEACON_DEACTIVATE", Sound.BLOCK_BEACON_DEACTIVATE);
        SOUNDS.put("BLOCK_PORTAL_TRAVEL", Sound.BLOCK_PORTAL_TRAVEL);
        SOUNDS.put("BLOCK_PORTAL_AMBIENT", Sound.BLOCK_PORTAL_AMBIENT);
    }

    public SoundEffectsManager(PowerFly plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void handleEffects(Player player, String path) {
        playSound(player, path);
        spawnParticles(player, path);

        // Detener efectos de vuelo si se desactiva
        if (path.equals("deactivation-effects")) {
            stopFlyingLoop(player);
        }
    }

    public void handleFlyEffects(Player player, boolean activate) {
        String path = activate ? "fly-effects" : "deactivation-effects";
        handleEffects(player, path);
    }

    public void handleTimeEndedEffects(Player player) {
        handleEffects(player, "time-ended-effects");
    }

    private void playSound(Player player, String path) {
        String soundName = config.getString(path + ".sound");
        if (soundName != null && SOUNDS.containsKey(soundName.toUpperCase())) {
            Sound sound = SOUNDS.get(soundName.toUpperCase());
            float volume = (float) config.getDouble(path + ".volume", 1.0);
            float pitch = (float) config.getDouble(path + ".pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private void spawnParticles(Player player, String path) {
        String particleName = config.getString(path + ".particle");
        if (particleName != null && PARTICLES.containsKey(particleName.toUpperCase())) {
            Particle particle = PARTICLES.get(particleName.toUpperCase());
            int count = config.getInt(path + ".particle-count", 10);
            double offset = config.getDouble(path + ".particle-offset", 0.5);
            player.getWorld().spawnParticle(particle, player.getLocation(), count, offset, offset, offset, 0.01);
        }
    }

    public void startFlyingLoop(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancelar si ya había uno
        cancelFlyingLoop(uuid);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.isFlying()) {
                    cancel();
                    flyingLoops.remove(uuid);
                    return;
                }

                String particleName = config.getString("flight-loop-effects.particle", "CLOUD");
                Particle particle = PARTICLES.getOrDefault(particleName.toUpperCase(), Particle.CLOUD);
                int count = config.getInt("flight-loop-effects.particle-count", 3);
                double offset = config.getDouble("flight-loop-effects.particle-offset", 0.2);

                player.getWorld().spawnParticle(particle, player.getLocation(), count, offset, offset, offset, 0.01);
            }
        };

        int intervalTicks = config.getInt("flight-loop-effects.interval", 10); // cada 0.5 segundos por defecto
        flyingLoops.put(uuid, task);
        task.runTaskTimer(plugin, 0L, intervalTicks);
    }

    public void stopFlyingLoop(Player player) {
        cancelFlyingLoop(player.getUniqueId());
    }

    private void cancelFlyingLoop(UUID uuid) {
        if (flyingLoops.containsKey(uuid)) {
            flyingLoops.get(uuid).cancel();
            flyingLoops.remove(uuid);
        }
    }
}
