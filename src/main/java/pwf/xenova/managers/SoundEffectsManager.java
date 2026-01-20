package pwf.xenova.managers;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pwf.xenova.PowerFly;
import java.util.*;

public class SoundEffectsManager {

    private final PowerFly plugin;
    private final Map<UUID, BukkitRunnable> activeLoops = new HashMap<>();

    private boolean effectsEnabled;
    private boolean soundsEnabled;

    private final Map<String, Particle> particleMap = new HashMap<>();
    private final Map<String, Sound> soundMap = new HashMap<>();

    // Particle configuration
    private String activationParticle;
    private int activationParticleCount;
    private double activationOffsetX, activationOffsetY, activationOffsetZ;
    private double activationSpeed;

    private String flyingParticle;
    private int flyingParticleCount;
    private double flyingOffsetX, flyingOffsetY, flyingOffsetZ;
    private double flyingSpeed;

    private String deactivationParticle;
    private int deactivationParticleCount;
    private double deactivationOffsetX, deactivationOffsetY, deactivationOffsetZ;
    private double deactivationSpeed;

    // Sound configuration
    private String activationSound;
    private float activationVolume, activationPitch;

    private String deactivationSound;
    private float deactivationVolume, deactivationPitch;

    private String timeEndedSound;
    private float timeEndedVolume, timeEndedPitch;

    public SoundEffectsManager(PowerFly plugin) {
        this.plugin = plugin;

        // ────────── PARTICLES ──────────

        particleMap.put("CLOUD", Particle.CLOUD);
        particleMap.put("END_ROD", Particle.END_ROD);
        particleMap.put("SMOKE", Particle.SMOKE);
        particleMap.put("LARGE_SMOKE", Particle.LARGE_SMOKE);
        particleMap.put("FLAME", Particle.FLAME);
        particleMap.put("HEART", Particle.HEART);
        particleMap.put("NOTE", Particle.NOTE);
        particleMap.put("DAMAGE_INDICATOR", Particle.DAMAGE_INDICATOR);
        particleMap.put("SWEEP_ATTACK", Particle.SWEEP_ATTACK);
        particleMap.put("FALLING_DUST", Particle.FALLING_DUST);
        particleMap.put("SNEEZE", Particle.SNEEZE);
        particleMap.put("SONIC_BOOM", Particle.SONIC_BOOM);
        particleMap.put("BUBBLE", Particle.BUBBLE);
        particleMap.put("BUBBLE_COLUMN_UP", Particle.BUBBLE_COLUMN_UP);
        particleMap.put("CURRENT_DOWN", Particle.CURRENT_DOWN);
        particleMap.put("SPLASH", Particle.SPLASH);
        particleMap.put("FISHING", Particle.FISHING);
        particleMap.put("SQUID_INK", Particle.SQUID_INK);
        particleMap.put("FALLING_OBSIDIAN_TEAR", Particle.FALLING_OBSIDIAN_TEAR);
        particleMap.put("DRIPPING_OBSIDIAN_TEAR", Particle.DRIPPING_OBSIDIAN_TEAR);
        particleMap.put("ELECTRIC_SPARK", Particle.ELECTRIC_SPARK);
        particleMap.put("ASH", Particle.ASH);
        particleMap.put("WITCH", Particle.WITCH);
        particleMap.put("CRIMSON_SPORE", Particle.CRIMSON_SPORE);
        particleMap.put("WARPED_SPORE", Particle.WARPED_SPORE);
        particleMap.put("DRIP_LAVA", Particle.DRIPPING_LAVA);
        particleMap.put("DRIP_WATER", Particle.DRIPPING_WATER);
        particleMap.put("LAVA", Particle.LAVA);
        particleMap.put("OMINOUS_SPAWNING", Particle.OMINOUS_SPAWNING);
        particleMap.put("TOTEM", Particle.TOTEM_OF_UNDYING);
        particleMap.put("FLASH", Particle.FLASH);
        particleMap.put("FIREWORK", Particle.FIREWORK);
        particleMap.put("CAMPFIRE_COSY_SMOKE", Particle.CAMPFIRE_COSY_SMOKE);
        particleMap.put("CAMPFIRE_SIGNAL_SMOKE", Particle.CAMPFIRE_SIGNAL_SMOKE);
        particleMap.put("DRAGON_BREATH", Particle.DRAGON_BREATH);
        particleMap.put("FALLING_LAVA", Particle.FALLING_LAVA);
        particleMap.put("FALLING_WATER", Particle.FALLING_WATER);
        particleMap.put("SOUL_FIRE_FLAME", Particle.SOUL_FIRE_FLAME);
        particleMap.put("SOUL", Particle.SOUL);
        particleMap.put("SCULK_SOUL", Particle.SCULK_SOUL);
        particleMap.put("NAUTILUS", Particle.NAUTILUS);
        particleMap.put("DRIP_HONEY", Particle.DRIPPING_HONEY);
        particleMap.put("FALLING_HONEY", Particle.FALLING_HONEY);
        particleMap.put("SMALL_FLAME", Particle.SMALL_FLAME);
        particleMap.put("TRIAL_SPAWNER_DETECTION", Particle.TRIAL_SPAWNER_DETECTION);
        particleMap.put("VAULT_CONNECTION", Particle.VAULT_CONNECTION);
        particleMap.put("TRIAL_SPAWNER_DETECTION_OMINOUS", Particle.TRIAL_SPAWNER_DETECTION_OMINOUS);
        particleMap.put("WHITE_SMOKE", Particle.WHITE_SMOKE);
        particleMap.put("INFESTED", Particle.INFESTED);
        particleMap.put("RAID_OMEN", Particle.RAID_OMEN);
        particleMap.put("TRIAL_OMEN", Particle.TRIAL_OMEN);
        particleMap.put("SPORE_BLOSSOM_AIR", Particle.SPORE_BLOSSOM_AIR);
        particleMap.put("SCULK_CHARGE", Particle.SCULK_CHARGE);
        particleMap.put("SCULK_CHARGE_POP", Particle.SCULK_CHARGE_POP);
        particleMap.put("SHRIEK", Particle.SHRIEK);
        particleMap.put("FALLING_NECTAR", Particle.FALLING_NECTAR);
        particleMap.put("FALLING_DRIPSTONE_LAVA", Particle.FALLING_DRIPSTONE_LAVA);
        particleMap.put("FALLING_DRIPSTONE_WATER", Particle.FALLING_DRIPSTONE_WATER);
        particleMap.put("DRIPPING_LAVA", Particle.DRIPPING_LAVA);
        particleMap.put("DRIPPING_WATER", Particle.DRIPPING_WATER);
        particleMap.put("FALLING_SPORE_BLOSSOM", Particle.FALLING_SPORE_BLOSSOM);

        // ────────── SOUNDS ──────────

        soundMap.put("BLOCK_BEACON_ACTIVATE", Sound.BLOCK_BEACON_ACTIVATE);
        soundMap.put("BLOCK_BEACON_DEACTIVATE", Sound.BLOCK_BEACON_DEACTIVATE);
        soundMap.put("BLOCK_PORTAL_TRAVEL", Sound.BLOCK_PORTAL_TRAVEL);
        soundMap.put("ENTITY_PLAYER_LEVELUP", Sound.ENTITY_PLAYER_LEVELUP);
        soundMap.put("ENTITY_EXPERIENCE_ORB_PICKUP", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        soundMap.put("ENTITY_ARROW_HIT", Sound.ENTITY_ARROW_HIT);
        soundMap.put("ENTITY_FIREWORK_ROCKET_LAUNCH", Sound.ENTITY_FIREWORK_ROCKET_LAUNCH);
        soundMap.put("ENTITY_WITHER_SPAWN", Sound.ENTITY_WITHER_SPAWN);
        soundMap.put("ENTITY_ENDER_DRAGON_GROWL", Sound.ENTITY_ENDER_DRAGON_GROWL);
        soundMap.put("BLOCK_ANVIL_USE", Sound.BLOCK_ANVIL_USE);
        soundMap.put("BLOCK_AMETHYST_BLOCK_CHIME", Sound.BLOCK_AMETHYST_BLOCK_CHIME);
        soundMap.put("BLOCK_NOTE_BLOCK_HARP", Sound.BLOCK_NOTE_BLOCK_HARP);
        soundMap.put("ENTITY_CAT_PURR", Sound.ENTITY_CAT_PURR);
        soundMap.put("ENTITY_VILLAGER_YES", Sound.ENTITY_VILLAGER_YES);
        soundMap.put("ENTITY_VILLAGER_NO", Sound.ENTITY_VILLAGER_NO);
        soundMap.put("ENTITY_PLAYER_BURP", Sound.ENTITY_PLAYER_BURP);
        soundMap.put("ENTITY_FIREWORK_ROCKET_BLAST", Sound.ENTITY_FIREWORK_ROCKET_BLAST);
        soundMap.put("BLOCK_GLASS_BREAK", Sound.BLOCK_GLASS_BREAK);
        soundMap.put("ENTITY_PLAYER_HURT", Sound.ENTITY_PLAYER_HURT);
        soundMap.put("ENTITY_PLAYER_DEATH", Sound.ENTITY_PLAYER_DEATH);
        soundMap.put("ENTITY_ITEM_PICKUP", Sound.ENTITY_ITEM_PICKUP);
        soundMap.put("BLOCK_FIRE_EXTINGUISH", Sound.BLOCK_FIRE_EXTINGUISH);
        soundMap.put("ENTITY_BAT_TAKEOFF", Sound.ENTITY_BAT_TAKEOFF);
        soundMap.put("ENTITY_CAT_HISS", Sound.ENTITY_CAT_HISS);
        soundMap.put("ENTITY_CREEPER_PRIMED", Sound.ENTITY_CREEPER_PRIMED);
        soundMap.put("ENTITY_ENDER_DRAGON_HURT", Sound.ENTITY_ENDER_DRAGON_HURT);
        soundMap.put("ENTITY_LIGHTNING_BOLT_IMPACT", Sound.ENTITY_LIGHTNING_BOLT_IMPACT);
        soundMap.put("ENTITY_SNOWBALL_THROW", Sound.ENTITY_SNOWBALL_THROW);
        soundMap.put("BLOCK_LAVA_EXTINGUISH", Sound.BLOCK_LAVA_EXTINGUISH);
        soundMap.put("ENTITY_SHULKER_OPEN", Sound.ENTITY_SHULKER_OPEN);
        soundMap.put("ENTITY_SHULKER_CLOSE", Sound.ENTITY_SHULKER_CLOSE);
        soundMap.put("ENTITY_PLAYER_ATTACK_SWEEP", Sound.ENTITY_PLAYER_ATTACK_SWEEP);
        soundMap.put("ENTITY_WARDEN_SNIFF", Sound.ENTITY_WARDEN_SNIFF);
        soundMap.put("BLOCK_ANVIL_BREAK", Sound.BLOCK_ANVIL_BREAK);
        soundMap.put("BLOCK_ANVIL_FALL", Sound.BLOCK_ANVIL_FALL);
        soundMap.put("BLOCK_ANVIL_LAND", Sound.BLOCK_ANVIL_LAND);
        soundMap.put("BLOCK_ANVIL_PLACE", Sound.BLOCK_ANVIL_PLACE);
        soundMap.put("BLOCK_CHEST_OPEN", Sound.BLOCK_CHEST_OPEN);
        soundMap.put("BLOCK_CHEST_CLOSE", Sound.BLOCK_CHEST_CLOSE);
        soundMap.put("ENTITY_EVOKER_CAST_SPELL", Sound.ENTITY_EVOKER_CAST_SPELL);
        soundMap.put("ENTITY_EVOKER_FANGS_ATTACK", Sound.ENTITY_EVOKER_FANGS_ATTACK);
        soundMap.put("ENTITY_EVOKER_PREPARE_ATTACK", Sound.ENTITY_EVOKER_PREPARE_ATTACK);
        soundMap.put("ENTITY_EVOKER_PREPARE_SUMMON", Sound.ENTITY_EVOKER_PREPARE_SUMMON);
        soundMap.put("ENTITY_EVOKER_PREPARE_WOLOLO", Sound.ENTITY_EVOKER_PREPARE_WOLOLO);
        soundMap.put("ENTITY_IRON_GOLEM_ATTACK", Sound.ENTITY_IRON_GOLEM_ATTACK);
        soundMap.put("ENTITY_IRON_GOLEM_HURT", Sound.ENTITY_IRON_GOLEM_HURT);
        soundMap.put("ENTITY_IRON_GOLEM_DEATH", Sound.ENTITY_IRON_GOLEM_DEATH);
        soundMap.put("ENTITY_BLAZE_AMBIENT", Sound.ENTITY_BLAZE_AMBIENT);
        soundMap.put("ENTITY_BLAZE_BURN", Sound.ENTITY_BLAZE_BURN);
        soundMap.put("ENTITY_BLAZE_DEATH", Sound.ENTITY_BLAZE_DEATH);
        soundMap.put("ENTITY_BLAZE_HURT", Sound.ENTITY_BLAZE_HURT);
        soundMap.put("ENTITY_COW_AMBIENT", Sound.ENTITY_COW_AMBIENT);
        soundMap.put("ENTITY_COW_DEATH", Sound.ENTITY_COW_DEATH);
        soundMap.put("ENTITY_COW_HURT", Sound.ENTITY_COW_HURT);
        soundMap.put("ENTITY_DOLPHIN_AMBIENT", Sound.ENTITY_DOLPHIN_AMBIENT);
        soundMap.put("ENTITY_DOLPHIN_DEATH", Sound.ENTITY_DOLPHIN_DEATH);
        soundMap.put("ENTITY_DOLPHIN_HURT", Sound.ENTITY_DOLPHIN_HURT);
        soundMap.put("ENTITY_DOLPHIN_PLAY", Sound.ENTITY_DOLPHIN_PLAY);
        soundMap.put("AMBIENT_CAVE", Sound.AMBIENT_CAVE);
        soundMap.put("AMBIENT_UNDERWATER_ENTER", Sound.AMBIENT_UNDERWATER_ENTER);
        soundMap.put("AMBIENT_UNDERWATER_EXIT", Sound.AMBIENT_UNDERWATER_EXIT);
        soundMap.put("AMBIENT_UNDERWATER_LOOP", Sound.AMBIENT_UNDERWATER_LOOP);
        soundMap.put("AMBIENT_UNDERWATER_LOOP_ADDITIONS", Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS);
        soundMap.put("AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE", Sound.AMBIENT_UNDERWATER_LOOP_ADDITIONS_RARE);
        soundMap.put("BLOCK_BEACON_AMBIENT", Sound.BLOCK_BEACON_AMBIENT);
        soundMap.put("BLOCK_BREWING_STAND_BREW", Sound.BLOCK_BREWING_STAND_BREW);

        reload();
    }

    public void reload() {
        cleanupAllLoops();

        effectsEnabled = plugin.getConfig().getBoolean("enable-effects", false);
        soundsEnabled = plugin.getConfig().getBoolean("enable-sounds", false);

        // Load activation particle settings
        activationParticle = plugin.getConfig().getString("particles.activation.type", "CLOUD");
        activationParticleCount = plugin.getConfig().getInt("particles.activation.count", 20);
        activationOffsetX = plugin.getConfig().getDouble("particles.activation.offset-x", 0.5);
        activationOffsetY = plugin.getConfig().getDouble("particles.activation.offset-y", 0.5);
        activationOffsetZ = plugin.getConfig().getDouble("particles.activation.offset-z", 0.5);
        activationSpeed = plugin.getConfig().getDouble("particles.activation.speed", 0.1);

        // Load flying particle settings
        flyingParticle = plugin.getConfig().getString("particles.flying.type", "END_ROD");
        flyingParticleCount = plugin.getConfig().getInt("particles.flying.count", 3);
        flyingOffsetX = plugin.getConfig().getDouble("particles.flying.offset-x", 0.3);
        flyingOffsetY = plugin.getConfig().getDouble("particles.flying.offset-y", 0.1);
        flyingOffsetZ = plugin.getConfig().getDouble("particles.flying.offset-z", 0.3);
        flyingSpeed = plugin.getConfig().getDouble("particles.flying.speed", 0.05);

        // Load deactivation particle settings
        deactivationParticle = plugin.getConfig().getString("particles.deactivation.type", "SMOKE");
        deactivationParticleCount = plugin.getConfig().getInt("particles.deactivation.count", 15);
        deactivationOffsetX = plugin.getConfig().getDouble("particles.deactivation.offset-x", 0.4);
        deactivationOffsetY = plugin.getConfig().getDouble("particles.deactivation.offset-y", 0.4);
        deactivationOffsetZ = plugin.getConfig().getDouble("particles.deactivation.offset-z", 0.4);
        deactivationSpeed = plugin.getConfig().getDouble("particles.deactivation.speed", 0.08);

        // Load activation sound settings
        activationSound = plugin.getConfig().getString("sounds.activation.type", "BLOCK_BEACON_ACTIVATE");
        activationVolume = (float) plugin.getConfig().getDouble("sounds.activation.volume", 1.0);
        activationPitch = (float) plugin.getConfig().getDouble("sounds.activation.pitch", 1.0);

        // Load deactivation sound settings
        deactivationSound = plugin.getConfig().getString("sounds.deactivation.type", "BLOCK_BEACON_DEACTIVATE");
        deactivationVolume = (float) plugin.getConfig().getDouble("sounds.deactivation.volume", 1.0);
        deactivationPitch = (float) plugin.getConfig().getDouble("sounds.deactivation.pitch", 1.0);

        // Load time ended sound settings
        timeEndedSound = plugin.getConfig().getString("sounds.time-ended.type", "BLOCK_PORTAL_TRAVEL");
        timeEndedVolume = (float) plugin.getConfig().getDouble("sounds.time-ended.volume", 0.8);
        timeEndedPitch = (float) plugin.getConfig().getDouble("sounds.time-ended.pitch", 1.2);
    }

    public void playActivationEffects(Player player) {
        if (effectsEnabled) spawnParticle(player, activationParticle, activationParticleCount,
                activationOffsetX, activationOffsetY, activationOffsetZ, activationSpeed);
        if (soundsEnabled) playSound(player, activationSound, activationVolume, activationPitch);
        if (effectsEnabled) startFlightLoop(player);
    }

    public void playDeactivationEffects(Player player) {
        stopFlightLoop(player);
        if (effectsEnabled) spawnParticle(player, deactivationParticle, deactivationParticleCount,
                deactivationOffsetX, deactivationOffsetY, deactivationOffsetZ, deactivationSpeed);
        if (soundsEnabled) playSound(player, deactivationSound, deactivationVolume, deactivationPitch);
    }

    public void playTimeEndEffects(Player player) {
        if (effectsEnabled) spawnParticle(player, deactivationParticle, deactivationParticleCount,
                deactivationOffsetX, deactivationOffsetY, deactivationOffsetZ, deactivationSpeed);
        if (soundsEnabled) playSound(player, timeEndedSound, timeEndedVolume, timeEndedPitch);
    }

    private void playSound(Player player, String soundName, float volume, float pitch) {
        if (!soundMap.containsKey(soundName)) {
            plugin.getLogger().warning("Sound not found: " + soundName);
            return;
        }
        player.playSound(player.getLocation(), soundMap.get(soundName), volume, pitch);
    }

    private void spawnParticle(Player player, String particleName, int count,
                               double offsetX, double offsetY, double offsetZ, double speed) {
        if (!particleMap.containsKey(particleName)) {
            plugin.getLogger().warning("Particle not found: " + particleName);
            return;
        }
        Location loc = player.getLocation();
        if (isOnGround(player)) loc.add(0, 0.5, 0);
        player.getWorld().spawnParticle(particleMap.get(particleName), loc, count,
                offsetX, offsetY, offsetZ, speed);
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
                if (effectsEnabled) spawnParticle(player, flyingParticle, flyingParticleCount,
                        flyingOffsetX, flyingOffsetY, flyingOffsetZ, flyingSpeed);
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
