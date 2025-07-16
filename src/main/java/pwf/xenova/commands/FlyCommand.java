package pwf.xenova.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.managers.SoundEffectsManager;
import pwf.xenova.managers.CooldownFlyManager;
import pwf.xenova.PowerFly;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlyCommand implements CommandExecutor {

    private final PowerFly plugin;
    private final SoundEffectsManager soundManager;
    private final CooldownFlyManager cooldownManager;
    private final Map<UUID, BukkitRunnable> flyTimers = new HashMap<>();

    public FlyCommand(PowerFly powerFly) {
        this.plugin = PowerFly.getInstance();
        this.soundManager = plugin.getSoundEffectsManager();
        this.cooldownManager = plugin.getCooldownFlyManager();
    }

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String @NotNull[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("powerfly.fly")) {
            player.sendMessage(plugin.getPrefixedMessage("no-permission", "&cYou do not have permission to use this command."));
            return true;
        }

        if (cooldownManager.isOnCooldown(player.getUniqueId())) {
            int secondsLeft = cooldownManager.getRemainingCooldownSeconds(player.getUniqueId());

            String raw = plugin.getMessage("fly-cooldown", "&cYou have used your fly time. Wait {seconds}s to fly again.");
            raw = raw.replace("{seconds}", String.valueOf(secondsLeft));
            String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + raw));
            return true;
        }

        if (!isFlyAllowedInWorld(player)) {
            player.sendMessage(plugin.getPrefixedMessage("fly-not-allowed-in-world", "&cYou can't fly in this world."));
            return true;
        }

        int availableFlyTime = plugin.getFlyTimeManager().getRemainingFlyTime(player.getUniqueId());
        if (availableFlyTime <= 0) {
            player.sendMessage(plugin.getPrefixedMessage("no-fly-time", "&cYou don't have any fly time available."));
            return true;
        }

        if (player.getAllowFlight()) {
            disableFly(player);
        } else {
            enableFly(player);
        }

        return true;
    }

    private boolean isFlyAllowedInWorld(Player player) {
        return plugin.getConfig().getStringList("allowed-worlds").contains(player.getWorld().getName());
    }

    private void disableFly(Player player) {
        stopFlyTimer(player);
        player.setAllowFlight(false);
        player.setFlying(false);
        soundManager.playDeactivationEffects(player);
        player.sendMessage(plugin.getPrefixedMessage("fly-disabled", "&cFly disabled."));
        player.sendActionBar(Component.empty());
    }

    private void enableFly(Player player) {
        stopFlyTimer(player);

        player.setAllowFlight(true);
        player.setFlying(true);
        soundManager.playActivationEffects(player);

        player.sendMessage(plugin.getPrefixedMessage("fly-enabled", "Fly activated."));

        startFlyTimer(player);
    }

    private void startFlyTimer(Player player) {
        BukkitRunnable timer = new BukkitRunnable() {
            long lastSecond = System.currentTimeMillis();

            public void run() {
                if (!player.isOnline() || !player.getAllowFlight()) {
                    cancel();
                    return;
                }

                if (System.currentTimeMillis() - lastSecond >= 1000) {
                    lastSecond = System.currentTimeMillis();

                    UUID uuid = player.getUniqueId();
                    plugin.getFlyTimeManager().delFlyTime(uuid, 1);

                    int remaining = plugin.getFlyTimeManager().getRemainingFlyTime(uuid);
                    String raw = plugin.getMessages().getString("remaining-fly-time", "&eRemaining fly time: &6{time}s");
                    raw = raw.replace("{time}", String.valueOf(remaining));
                    Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
                    player.sendActionBar(message);

                    if (remaining <= 0) {
                        endFly(player);
                    }
                }
            }
        };

        timer.runTaskTimer(plugin, 0L, 20L);
        flyTimers.put(player.getUniqueId(), timer);
    }

    private void endFly(Player player) {
        soundManager.playTimeEndEffects(player);
        disableFly(player);

        UUID uuid = player.getUniqueId();
        if (!cooldownManager.isOnCooldown(uuid)) {
            cooldownManager.startCooldown(uuid);
        }

        String raw = plugin.getMessages().getString("fly-time-ended", "&cFly time has ended.");
        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        player.sendActionBar(message);
    }

    private void stopFlyTimer(Player player) {
        BukkitRunnable timer = flyTimers.remove(player.getUniqueId());
        if (timer != null) {
            timer.cancel();
        }
    }
}
