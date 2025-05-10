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

public class FlyCommand implements CommandExecutor {

    private final PowerFly plugin;
    private final SoundEffectsManager soundEffectsManager;
    private BukkitRunnable flightTask;

    public FlyCommand() {
        this.plugin = PowerFly.getInstance();
        this.soundEffectsManager = new SoundEffectsManager(plugin);
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefixedMessage("console-error", "You must be a player to execute this command."));
            return true;
        }

        if (!isFlightAllowedInWorld(player)) {
            player.sendMessage(plugin.getPrefixedMessage("fly-not-allowed-in-world", "You cannot use fly in this world."));
            return true;
        }

        int flightTime = getPlayerFlightTime(player);
        if (flightTime <= 0) {
            player.sendMessage(plugin.getPrefixedMessage("no-flight-time", "You don't have flight time available."));
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
            return plugin.getConfig().getInt("fly-time", 10);
        }

        var user = plugin.getLuckPerms().getUserManager().getUser(player.getUniqueId());
        if (user == null) return -1;

        String group = user.getPrimaryGroup();
        return plugin.getConfig().getInt("group-times." + group, plugin.getConfig().getInt("fly-time", 10));
    }

    private void disableFlight(Player player) {
        player.setAllowFlight(false);
        player.setFlying(false);
        player.sendMessage(plugin.getPrefixedMessage("fly-disabled", "Flying has been disabled."));

        if (flightTask != null) {
            flightTask.cancel();
            flightTask = null;
        }

        soundEffectsManager.handleFlyEffects(player, false);
        soundEffectsManager.stopFlyingLoop(player);
    }

    private void enableFlight(Player player, int flightTime) {
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage(plugin.getPrefixedMessage("fly-enabled", "Flying has been enabled."));

        soundEffectsManager.handleFlyEffects(player, true);
        soundEffectsManager.startFlyingLoop(player);

        flightTask = new BukkitRunnable() {
            int timeRemaining = flightTime;

            public void run() {
                if (timeRemaining <= 0) {
                    disableFlight(player);
                    player.sendMessage(plugin.getPrefixedMessage("fly-time-ended", "Your flight time has ended."));
                    soundEffectsManager.handleTimeEndedEffects(player);
                    cancel();
                    return;
                }

                if (!player.isFlying()) {
                    player.setAllowFlight(false);
                    soundEffectsManager.stopFlyingLoop(player);
                    cancel();
                    return;
                }

                player.sendActionBar(Component.text("Remaining flight time: " + timeRemaining + " seconds"));
                timeRemaining--;
            }
        };

        flightTask.runTaskTimer(plugin, 0L, 20L);
    }
}
