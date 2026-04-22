package pwf.xenova.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pwf.xenova.PowerFly;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEffectsManager {

    private final PowerFly plugin;
    private final Map<UUID, BukkitRunnable> activeLoops = new ConcurrentHashMap<>();

    private boolean effectsEnabled;
    private boolean soundsEnabled;

    private record ParticleConfig(Particle type, int count,
                                  double offsetX, double offsetY, double offsetZ,
                                  double speed) {}

    private record SoundConfig(Sound type, float volume, float pitch) {}

    private ParticleConfig activationParticle;
    private ParticleConfig flyingParticle;
    private ParticleConfig deactivationParticle;

    private SoundConfig activationSound;
    private SoundConfig deactivationSound;
    private SoundConfig timeEndedSound;

    public SoundEffectsManager(PowerFly plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        cleanupAllLoops();

        effectsEnabled = plugin.getConfig().getBoolean("enable-effects", false);
        soundsEnabled  = plugin.getConfig().getBoolean("enable-sounds", false);

        activationParticle   = loadParticleConfig("particles.activation");
        flyingParticle       = loadParticleConfig("particles.flying");
        deactivationParticle = loadParticleConfig("particles.deactivation");

        activationSound   = loadSoundConfig("sounds.activation");
        deactivationSound = loadSoundConfig("sounds.deactivation");
        timeEndedSound    = loadSoundConfig("sounds.time-ended");

        if (effectsEnabled) {
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.isFlying() && p.getAllowFlight())
                    .forEach(this::startFlightLoop);
        }
    }

    private ParticleConfig loadParticleConfig(String path) {
        Particle type  = resolveParticle(plugin.getConfig().getString(path + ".type", "CLOUD"));
        int count      = plugin.getConfig().getInt(path + ".count", 5);
        double offsetX = plugin.getConfig().getDouble(path + ".offset-x", 0.3);
        double offsetY = plugin.getConfig().getDouble(path + ".offset-y", 0.3);
        double offsetZ = plugin.getConfig().getDouble(path + ".offset-z", 0.3);
        double speed   = plugin.getConfig().getDouble(path + ".speed", 0.05);
        return new ParticleConfig(type, count, offsetX, offsetY, offsetZ, speed);
    }

    private SoundConfig loadSoundConfig(String path) {
        Sound sound  = resolveSound(plugin.getConfig().getString(path + ".type", "BLOCK_BEACON_ACTIVATE"));
        float volume = (float) plugin.getConfig().getDouble(path + ".volume", 1.0);
        float pitch  = (float) plugin.getConfig().getDouble(path + ".pitch", 1.0);
        return new SoundConfig(sound, volume, pitch);
    }

    public void playActivationEffects(Player player) {
        if (effectsEnabled) spawnParticle(player, activationParticle);
        if (soundsEnabled)  playSound(player, activationSound);
        if (effectsEnabled) startFlightLoop(player);
    }

    public void playDeactivationEffects(Player player) {
        stopFlightLoop(player);
        if (effectsEnabled) spawnParticle(player, deactivationParticle);
        if (soundsEnabled)  playSound(player, deactivationSound);
    }

    public void playTimeEndEffects(Player player) {
        if (effectsEnabled) spawnParticle(player, deactivationParticle);
        if (soundsEnabled)  playSound(player, timeEndedSound);
    }

    public void cleanupAllLoops() {
        activeLoops.values().forEach(BukkitRunnable::cancel);
        activeLoops.clear();
    }

    private void playSound(Player player, SoundConfig config) {
        if (!soundsEnabled || config == null || config.type() == null) return;
        player.playSound(player.getLocation(), config.type(), config.volume(), config.pitch());
    }

    private void spawnParticle(Player player, ParticleConfig config) {
        if (!effectsEnabled || config == null || config.type() == null) return;
        Location loc = player.getLocation();
        if (isOnGround(player)) loc.add(0, 0.5, 0);
        player.getWorld().spawnParticle(config.type(), loc, config.count(),
                config.offsetX(), config.offsetY(), config.offsetZ(), config.speed());
    }

    private void startFlightLoop(Player player) {
        UUID uuid = player.getUniqueId();
        stopFlightLoop(player);

        BukkitRunnable task = new BukkitRunnable() {
            public void run() {
                if (!player.isOnline() || !player.getAllowFlight()) {
                    cancel();
                    activeLoops.remove(uuid);
                    return;
                }
                spawnParticle(player, flyingParticle);
            }
        };

        task.runTaskTimer(plugin, 0L, 5L);
        activeLoops.put(uuid, task);
    }

    private void stopFlightLoop(Player player) {
        BukkitRunnable task = activeLoops.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private boolean isOnGround(Player player) {
        return !player.getLocation().clone().subtract(0, 0.1, 0).getBlock().isPassable();
    }

    private Particle resolveParticle(String name) {
        if (name == null || name.isEmpty()) return null;
        String clean = name.toLowerCase(Locale.ROOT).replace("_", ".");
        return Registry.PARTICLE_TYPE.stream()
                .filter(p -> {
                    NamespacedKey key = Registry.PARTICLE_TYPE.getKey(p);
                    return key != null && (key.getKey().replace("_", ".").equals(clean)
                            || key.getKey().endsWith(clean.replace(".", "")));
                })
                .findFirst()
                .orElseGet(() -> {
                    plugin.getLogger().warning("Unknown particle type: " + name);
                    return null;
                });
    }

    private Sound resolveSound(String name) {
        if (name == null || name.isEmpty()) return null;
        String clean = name.toLowerCase(Locale.ROOT).replace("_", ".");
        return Registry.SOUNDS.stream()
                .filter(s -> {
                    NamespacedKey key = Registry.SOUNDS.getKey(s);
                    return key != null && (key.getKey().replace("_", ".").equals(clean)
                            || key.getKey().endsWith(clean));
                })
                .findFirst()
                .orElseGet(() -> {
                    plugin.getLogger().warning("Unknown sound type: " + name);
                    return null;
                });
    }
}