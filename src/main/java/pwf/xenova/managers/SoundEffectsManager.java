package pwf.xenova.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pwf.xenova.PowerFly;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SoundEffectsManager {

    private final PowerFly plugin;

    private final Set<UUID> flyingPlayers = ConcurrentHashMap.newKeySet();

    private BukkitTask globalParticleTask;

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
        stopGlobalTask();
        flyingPlayers.clear();

        effectsEnabled = plugin.getMainConfig().getBoolean("enable-effects", true);
        soundsEnabled  = plugin.getMainConfig().getBoolean("enable-sounds", true);

        activationParticle   = loadParticleConfig("particles.activation");
        flyingParticle       = loadParticleConfig("particles.flying");
        deactivationParticle = loadParticleConfig("particles.deactivation");

        activationSound   = loadSoundConfig("sounds.activation");
        deactivationSound = loadSoundConfig("sounds.deactivation");
        timeEndedSound    = loadSoundConfig("sounds.time-ended");

        if (effectsEnabled) {
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.isFlying() && p.getAllowFlight())
                    .map(Player::getUniqueId)
                    .forEach(flyingPlayers::add);

            startGlobalTask();
        }
    }

    public void playActivationEffects(Player player) {
        if (effectsEnabled) {
            spawnParticle(player, activationParticle);
            registerFlying(player.getUniqueId());
        }
        if (soundsEnabled) playSound(player, activationSound);
    }

    public void playDeactivationEffects(Player player) {
        unregisterFlying(player.getUniqueId());
        if (effectsEnabled) spawnParticle(player, deactivationParticle);
        if (soundsEnabled)  playSound(player, deactivationSound);
    }

    public void playTimeEndEffects(Player player) {
        if (effectsEnabled) spawnParticle(player, deactivationParticle);
        if (soundsEnabled)  playSound(player, timeEndedSound);
    }

    public void cleanupAllLoops() {
        stopGlobalTask();
        flyingPlayers.clear();
    }

    private void startGlobalTask() {
        if (globalParticleTask != null) return;

        globalParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!effectsEnabled || flyingParticle == null) return;

            Iterator<UUID> it = flyingPlayers.iterator();
            while (it.hasNext()) {
                UUID uuid = it.next();
                Player player = Bukkit.getPlayer(uuid);

                if (player == null || !player.isOnline() || !player.getAllowFlight()) {
                    it.remove();
                    continue;
                }

                spawnParticle(player, flyingParticle);
            }
        }, 0L, 5L);
    }

    private void stopGlobalTask() {
        if (globalParticleTask != null) {
            globalParticleTask.cancel();
            globalParticleTask = null;
        }
    }

    private void registerFlying(UUID uuid) {
        flyingPlayers.add(uuid);
        if (effectsEnabled && globalParticleTask == null) {
            startGlobalTask();
        }
    }

    private void unregisterFlying(UUID uuid) {
        flyingPlayers.remove(uuid);
    }

    private ParticleConfig loadParticleConfig(String path) {
        Particle type  = resolveParticle(plugin.getMainConfig().getString(path + ".type", "CLOUD"));
        int count      = plugin.getMainConfig().getInt(path + ".count", 5);
        double offsetX = plugin.getMainConfig().getDouble(path + ".offset-x", 0.3);
        double offsetY = plugin.getMainConfig().getDouble(path + ".offset-y", 0.3);
        double offsetZ = plugin.getMainConfig().getDouble(path + ".offset-z", 0.3);
        double speed   = plugin.getMainConfig().getDouble(path + ".speed", 0.05);
        return new ParticleConfig(type, count, offsetX, offsetY, offsetZ, speed);
    }

    private SoundConfig loadSoundConfig(String path) {
        Sound sound  = resolveSound(plugin.getMainConfig().getString(path + ".type", "BLOCK_BEACON_ACTIVATE"));
        float volume = plugin.getMainConfig().getDouble(path + ".volume", 1.0).floatValue();
        float pitch  = plugin.getMainConfig().getDouble(path + ".pitch", 1.0).floatValue();
        return new SoundConfig(sound, volume, pitch);
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

    private boolean isOnGround(Player player) {
        return !player.getLocation().clone().subtract(0, 0.1, 0).getBlock().isPassable();
    }

    private Particle resolveParticle(String name) {
        if (name == null || name.isEmpty()) return null;
        String normalized = name.toLowerCase(Locale.ROOT).trim().replace(" ", "_");

        for (Particle p : Registry.PARTICLE_TYPE) {
            NamespacedKey key = Registry.PARTICLE_TYPE.getKey(p);
            if (key != null && key.getKey().equals(normalized)) return p;
        }
        for (Particle p : Registry.PARTICLE_TYPE) {
            NamespacedKey key = Registry.PARTICLE_TYPE.getKey(p);
            if (key != null && key.getKey().contains(normalized)) return p;
        }

        plugin.getLogger().warning("Unknown particle type: '" + name + "'. Check your config.yml.");
        return null;
    }

    private Sound resolveSound(String name) {
        if (name == null || name.isEmpty()) return null;
        String normalized = name.toLowerCase(Locale.ROOT).trim().replace("_", ".");

        for (Sound s : Registry.SOUNDS) {
            NamespacedKey key = Registry.SOUNDS.getKey(s);
            if (key != null && key.getKey().equals(normalized)) return s;
        }
        for (Sound s : Registry.SOUNDS) {
            NamespacedKey key = Registry.SOUNDS.getKey(s);
            if (key != null && key.getKey().endsWith(normalized)) return s;
        }

        plugin.getLogger().warning("Unknown sound type: '" + name + "'. Check your config.yml.");
        return null;
    }
}