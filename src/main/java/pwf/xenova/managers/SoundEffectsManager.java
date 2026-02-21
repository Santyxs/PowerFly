package pwf.xenova.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pwf.xenova.PowerFly;
import java.lang.reflect.Method;
import java.util.*;

public class SoundEffectsManager {

    private final PowerFly plugin;
    private final Map<UUID, BukkitRunnable> activeLoops = new HashMap<>();

    private boolean effectsEnabled;
    private boolean soundsEnabled;

    private String activationParticle, flyingParticle, deactivationParticle;
    private int activationParticleCount, flyingParticleCount, deactivationParticleCount;
    private double activationOffsetX, activationOffsetY, activationOffsetZ, activationSpeed;
    private double flyingOffsetX, flyingOffsetY, flyingOffsetZ, flyingSpeed;
    private double deactivationOffsetX, deactivationOffsetY, deactivationOffsetZ, deactivationSpeed;

    private String activationSound, deactivationSound, timeEndedSound;
    private float activationVolume, activationPitch, deactivationVolume, deactivationPitch, timeEndedVolume, timeEndedPitch;

    public SoundEffectsManager(PowerFly plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        cleanupAllLoops();

        effectsEnabled = plugin.getConfig().getBoolean("enable-effects", false);
        soundsEnabled = plugin.getConfig().getBoolean("enable-sounds", false);

        activationParticle = plugin.getConfig().getString("particles.activation.type", "CLOUD");
        activationParticleCount = plugin.getConfig().getInt("particles.activation.count", 20);
        activationOffsetX = plugin.getConfig().getDouble("particles.activation.offset-x", 0.5);
        activationOffsetY = plugin.getConfig().getDouble("particles.activation.offset-y", 0.5);
        activationOffsetZ = plugin.getConfig().getDouble("particles.activation.offset-z", 0.5);
        activationSpeed = plugin.getConfig().getDouble("particles.activation.speed", 0.1);

        flyingParticle = plugin.getConfig().getString("particles.flying.type", "END_ROD");
        flyingParticleCount = plugin.getConfig().getInt("particles.flying.count", 3);
        flyingOffsetX = plugin.getConfig().getDouble("particles.flying.offset-x", 0.3);
        flyingOffsetY = plugin.getConfig().getDouble("particles.flying.offset-y", 0.1);
        flyingOffsetZ = plugin.getConfig().getDouble("particles.flying.offset-z", 0.3);
        flyingSpeed = plugin.getConfig().getDouble("particles.flying.speed", 0.05);

        deactivationParticle = plugin.getConfig().getString("particles.deactivation.type", "SMOKE");
        deactivationParticleCount = plugin.getConfig().getInt("particles.deactivation.count", 15);
        deactivationOffsetX = plugin.getConfig().getDouble("particles.deactivation.offset-x", 0.4);
        deactivationOffsetY = plugin.getConfig().getDouble("particles.deactivation.offset-y", 0.4);
        deactivationOffsetZ = plugin.getConfig().getDouble("particles.deactivation.offset-z", 0.4);
        deactivationSpeed = plugin.getConfig().getDouble("particles.deactivation.speed", 0.08);

        activationSound = plugin.getConfig().getString("sounds.activation.type", "BLOCK_BEACON_ACTIVATE");
        activationVolume = (float) plugin.getConfig().getDouble("sounds.activation.volume", 1.0);
        activationPitch = (float) plugin.getConfig().getDouble("sounds.activation.pitch", 1.0);

        deactivationSound = plugin.getConfig().getString("sounds.deactivation.type", "BLOCK_BEACON_DEACTIVATE");
        deactivationVolume = (float) plugin.getConfig().getDouble("sounds.deactivation.volume", 1.0);
        deactivationPitch = (float) plugin.getConfig().getDouble("sounds.deactivation.pitch", 1.0);

        timeEndedSound = plugin.getConfig().getString("sounds.time-ended.type", "BLOCK_PORTAL_TRAVEL");
        timeEndedVolume = (float) plugin.getConfig().getDouble("sounds.time-ended.volume", 0.8);
        timeEndedPitch = (float) plugin.getConfig().getDouble("sounds.time-ended.pitch", 1.2);
    }

    public void playActivationEffects(Player player) {
        if (effectsEnabled) spawnParticle(player, activationParticle, activationParticleCount, activationOffsetX, activationOffsetY, activationOffsetZ, activationSpeed);
        if (soundsEnabled) playSound(player, activationSound, activationVolume, activationPitch);
        if (effectsEnabled) startFlightLoop(player);
    }

    public void playDeactivationEffects(Player player) {
        stopFlightLoop(player);
        if (effectsEnabled) spawnParticle(player, deactivationParticle, deactivationParticleCount, deactivationOffsetX, deactivationOffsetY, deactivationOffsetZ, deactivationSpeed);
        if (soundsEnabled) playSound(player, deactivationSound, deactivationVolume, deactivationPitch);
    }

    public void playTimeEndEffects(Player player) {
        if (effectsEnabled) spawnParticle(player, deactivationParticle, deactivationParticleCount, deactivationOffsetX, deactivationOffsetY, deactivationOffsetZ, deactivationSpeed);
        if (soundsEnabled) playSound(player, timeEndedSound, timeEndedVolume, timeEndedPitch);
    }

    private void playSound(Player player, String soundName, float volume, float pitch) {
        if (!soundsEnabled || soundName == null || soundName.isEmpty()) return;

        String cleanName = soundName.toLowerCase(Locale.ROOT).replace("_", ".");

        try {
            Method valuesMethod = Sound.class.getMethod("values");
            Object[] sounds = (Object[]) valuesMethod.invoke(null);
            Method nameMethod = sounds[0].getClass().getMethod("name");

            for (Object obj : sounds) {
                String enumName = ((String) nameMethod.invoke(obj)).toLowerCase(Locale.ROOT).replace("_", ".");
                if (enumName.equals(cleanName) || enumName.endsWith(cleanName)) {
                    player.playSound(player.getLocation(), (Sound) obj, volume, pitch);
                    return;
                }
            }
        } catch (Exception e) {
            try {
                Registry.SOUNDS.stream()
                        .filter(s -> {
                            NamespacedKey key = Registry.SOUNDS.getKey(s);
                            return key != null && (key.getKey().equals(cleanName) || key.getKey().endsWith(cleanName));
                        })
                        .findFirst()
                        .ifPresent(sound -> player.playSound(player.getLocation(), sound, volume, pitch));
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not play sound: " + soundName);
            }
        }
    }

    private void spawnParticle(Player player, String particleName, int count, double offsetX, double offsetY, double offsetZ, double speed) {
        if (!effectsEnabled || particleName == null || particleName.isEmpty()) return;

        try {
            Registry.class.getField("PARTICLE_TYPE");
        } catch (NoSuchFieldException e) {
            return;
        }

        String cleanName = particleName.toLowerCase(Locale.ROOT).replace("_", ".");

        Particle particle = Registry.PARTICLE_TYPE.stream()
                .filter(p -> {
                    NamespacedKey key = Registry.PARTICLE_TYPE.getKey(p);
                    return key != null && (key.getKey().replace("_", ".").equals(cleanName) || key.getKey().endsWith(cleanName.replace(".", "")));
                })
                .findFirst().orElse(null);

        if (particle != null) {
            Location loc = player.getLocation();
            if (isOnGround(player)) loc.add(0, 0.5, 0);
            player.getWorld().spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, speed);
        }
    }

    private void startFlightLoop(Player player) {
        UUID playerId = player.getUniqueId();
        stopFlightLoop(player);

        BukkitRunnable task = new BukkitRunnable() {
            public void run() {
                if (!player.isOnline() || !player.getAllowFlight()) {
                    cancel();
                    activeLoops.remove(playerId);
                    return;
                }
                spawnParticle(player, flyingParticle, flyingParticleCount, flyingOffsetX, flyingOffsetY, flyingOffsetZ, flyingSpeed);
            }
        };

        task.runTaskTimer(plugin, 0L, 5L);
        activeLoops.put(playerId, task);
    }

    private void stopFlightLoop(Player player) {
        BukkitRunnable task = activeLoops.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    public void cleanupAllLoops() {
        activeLoops.values().forEach(BukkitRunnable::cancel);
        activeLoops.clear();
    }

    private boolean isOnGround(Player player) {
        return !player.getLocation().clone().subtract(0, 0.1, 0).getBlock().isPassable();
    }
}