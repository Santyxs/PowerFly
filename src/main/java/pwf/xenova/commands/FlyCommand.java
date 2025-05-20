package pwf.xenova.commands;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;
import pwf.xenova.managers.SoundEffectsManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlyCommand implements CommandExecutor {

    private final PowerFly plugin;
    private final SoundEffectsManager soundManager;
    private final Map<UUID, BukkitRunnable> flightTimers = new HashMap<>();

    public FlyCommand() {
        this.plugin = PowerFly.getInstance();
        this.soundManager = plugin.getSoundEffectsManager();
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("&cOnly players can use this command.");
            return true;
        }

        if (!isFlightAllowedInWorld(player)) {
            player.sendMessage(plugin.getPrefixedMessage("fly-not-allowed-in-world", "&cYou can't fly in this world."));
            return true;
        }

        int flightTime = getPlayerFlightTime(player);
        if (flightTime <= 0) {
            player.sendMessage(plugin.getPrefixedMessage("no-fly-time", "&cYou don't have any fly time available."));
            return true;
        }

        if (player.getAllowFlight()) {
            disableFlight(player);
        } else {
            enableFlight(player, flightTime);
        }

        return true;
    }

    private boolean isFlightAllowedInWorld(Player player) {
        return plugin.getConfig().getStringList("allowed-worlds").contains(player.getWorld().getName());
    }

    private int getPlayerFlightTime(Player player) {
        if (!plugin.getConfig().getBoolean("use-group-fly-time", false)) {
            return plugin.getConfig().getInt("fly-time", 60);
        }

        var user = plugin.getLuckPerms().getUserManager().getUser(player.getUniqueId());
        if (user == null) return 0;

        String group = user.getPrimaryGroup();
        return plugin.getConfig().getInt("group-times." + group, plugin.getConfig().getInt("fly-time", 60));
    }

    private void disableFlight(Player player) {
        stopFlightTimer(player);
        player.setAllowFlight(false);
        player.setFlying(false);
        soundManager.playDeactivationEffects(player);
        player.sendMessage(plugin.getPrefixedMessage("fly-disabled", "Fly disabled."));
        player.sendActionBar(Component.empty());
    }

    private void enableFlight(Player player, int flightTime) {
        stopFlightTimer(player);

        player.setAllowFlight(true);
        player.setFlying(true);
        soundManager.playActivationEffects(player);

        player.sendMessage(plugin.getPrefixedMessage(
                "fly-enabled",
                "Flight activated by " + flightTime + " seconds."
        ));

        startFlightTimer(player, flightTime);
    }

    private void startFlightTimer(Player player, int flightTime) {
        BukkitRunnable timer = new BukkitRunnable() {
            int timeLeft = flightTime;
            long lastSecond = System.currentTimeMillis();

            public void run() {
                // Verificación básica de estado
                if (!player.isOnline() || !player.getAllowFlight()) {
                    cancelTimer(player);
                    return;
                }

                // Actualizar contador cada SEGUNDO REAL
                if (System.currentTimeMillis() - lastSecond >= 1000) {
                    lastSecond = System.currentTimeMillis();

                    player.sendActionBar(Component.text(
                            "§e" + plugin.getMessage("remaining-flight-time") + "§6" + timeLeft + "s"
                    ));

                    if (timeLeft-- <= 0) {
                        endFlight(player);
                    }
                }
            }
        };

        timer.runTaskTimer(plugin, 0L, 20L);
        flightTimers.put(player.getUniqueId(), timer);
    }

    private void endFlight(Player player) {
        soundManager.playTimeEndEffects(player);
        disableFlight(player);
        player.sendActionBar(Component.text(plugin.getMessage("fly-time-ended")));
    }

    private void stopFlightTimer(Player player) {
        BukkitRunnable timer = flightTimers.remove(player.getUniqueId());
        if (timer != null) {
            timer.cancel();
        }
    }

    private void cancelTimer(Player player) {
        stopFlightTimer(player);
        player.sendActionBar(Component.empty());
    }
}
