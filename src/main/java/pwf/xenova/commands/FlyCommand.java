package pwf.xenova.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import pwf.xenova.utils.MessageFormat;
import pwf.xenova.PowerFly;

import java.util.*;

public record FlyCommand(PowerFly plugin) implements CommandExecutor {

    private static final int INFINITE_FLY_TIME = -1;

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String @NonNull [] args) {

        if (sender instanceof Player player) {
            if (!player.hasPermission("powerfly.fly") && !player.hasPermission("powerfly.admin") && !player.isOp()) {
                sendMessage(sender, "no-permission", "&cYou do not have permission to use this command.");
                return true;
            }

            if (args.length > 0 && !args[0].equalsIgnoreCase(player.getName())) {
                if (!player.hasPermission("powerfly.fly.others") && !player.hasPermission("powerfly.admin") && !player.isOp()) {
                    sendMessage(sender, "no-permission-others", "&cYou do not have permission to toggle fly for others.");
                    return true;
                }
            }
        }

        List<Player> targets = getTargets(sender, args);

        if (targets.isEmpty()) {
            if (args.length > 0 && !args[0].equalsIgnoreCase("all")) {
                sendMessage(sender, "player-not-found", "&cPlayer not found.");
            } else {
                sendMessage(sender, "no-player-specified", "&cYou must specify a player name.");
            }
            return true;
        }

        String action = parseAction(args);
        boolean isAll = args.length > 0 && args[0].equalsIgnoreCase("all");

        int activatedCount = 0;
        int deactivatedCount = 0;
        boolean singleTargetSuccess = false;

        for (Player target : targets) {
            boolean enable = action.equals("toggle") ? !target.getAllowFlight() : action.equals("on");
            boolean result = toggleFly(target, enable, sender, isAll);
            if (result) {
                if (target.getAllowFlight()) activatedCount++;
                else deactivatedCount++;
                singleTargetSuccess = true;
            }
        }

        if (isAll) {
            if (activatedCount > 0) sendMessage(sender, "fly-enabled-all", "&aFly activated for all players.");
            if (deactivatedCount > 0) sendMessage(sender, "fly-disabled-all", "&cFly disabled for all players.");
            if (activatedCount == 0 && deactivatedCount == 0) {
                sendMessage(sender, "no-players-with-fly-time", "&cNo players have fly time remaining.");
            }
        } else if (targets.size() == 1 && singleTargetSuccess) {
            Player target = targets.getFirst();
            boolean isSenderTarget = sender instanceof Player p && p.equals(target);
            if (!isSenderTarget) sendTargetFeedback(sender, target);
        }

        return true;
    }

    public boolean toggleFly(Player player, boolean enable, CommandSender sender, boolean isAllCommand) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            if (!isAllCommand) {
                boolean isSameSender = sender instanceof Player p && p.equals(player);
                if (isSameSender) {
                    sendMessage(player, "fly-mode-error", "&cYou cannot use this command in Creative or Spectator mode.");
                } else {
                    String msg = plugin.getMessageString("fly-mode-error-target", "&c{player} cannot use fly because they are in Creative or Spectator mode.")
                            .replace("{player}", player.getName());
                    sendRaw(sender, msg);
                }
            }
            return false;
        }

        UUID uuid = player.getUniqueId();
        int remaining = plugin.getFlyTimeManager().getRemainingFlyTime(uuid);

        if (enable && remaining <= 0 && remaining != INFINITE_FLY_TIME) {
            if (isAllCommand) return false;

            boolean isSameSender = sender instanceof Player p && p.equals(player);
            if (!isSameSender) {
                String msg = plugin.getMessageString("no-player-fly-time", "&c{player} has no fly time remaining.")
                        .replace("{player}", player.getName());
                sendRaw(sender, msg);
                return false;
            }

            String cooldown = plugin.getCooldownFlyManager().getRemainingCooldownFormatted(uuid);
            String msg = plugin.getMessageString("fly-cooldown", "&cYou have used your fly time, wait &f{cooldown_time} &cto fly again.")
                    .replace("{cooldown_time}", cooldown);
            sendRaw(player, msg);
            return false;
        }

        if (enable) enableFly(player, remaining, sender);
        else disableFly(player, false);

        return true;
    }

    private void enableFly(Player player, int maxTime, CommandSender sender) {
        if (plugin.getClaimFlyManager().cannotFlyHere(player, player.getLocation())) {
            plugin.getClaimFlyManager().sendClaimFlyMessage(player);
            return;
        }

        if (plugin.getFlyRestrictionManager().isFlightBlockedInWorld(player)) {
            sendMessage(player, "fly-not-allowed-in-world", "&cYou cannot fly in this world.");
            if (!sender.equals(player)) {
                String msg = plugin.getMessageString("fly-not-allowed-in-world-target", "&c{player} cannot fly in that world.")
                        .replace("{player}", player.getName());
                sendRaw(sender, msg);
            }
            return;
        }

        if (plugin.getFlyRestrictionManager().isFlightBlockedInRegion(player)) {
            sendMessage(player, "fly-not-allowed-in-region", "&cYou cannot fly in this region.");
            if (!sender.equals(player)) {
                String msg = plugin.getMessageString("fly-not-allowed-in-region-target", "&c{player} cannot fly in that region.")
                        .replace("{player}", player.getName());
                sendRaw(sender, msg);
            }
            return;
        }

        if (plugin.getCombatFlyManager().isInCombat(player)) {
            sendMessage(player, "fly-in-combat", "&cYou cannot fly while in combat.");
            return;
        }

        plugin.getFlyRuntimeManager().cleanup(player);

        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFallDistance(0);
        plugin.getFlyRuntimeManager().addSession(player.getUniqueId());

        plugin.getSoundEffectsManager().playActivationEffects(player);
        sendMessage(player, "fly-enabled", "&aFly activated.");

        if (plugin.getMainConfig().getBoolean("show-bossbar", true)) {
            plugin.getFlyRuntimeManager().showBossBar(player, maxTime);
        }
        plugin.getFlyRuntimeManager().startTimer(player, maxTime);
    }

    public void disableFly(Player player, boolean fromEnd) {
        plugin.getFlyRuntimeManager().cleanup(player);

        if (fromEnd) handleFlyEnd(player);

        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        plugin.getSoundEffectsManager().playDeactivationEffects(player);
        sendMessage(player, "fly-disabled", "&cFly disabled.");
    }

    public void endFly(Player player) {
        disableFly(player, true);
        plugin.getSoundEffectsManager().playTimeEndEffects(player);
    }

    private void handleFlyEnd(Player player) {
        UUID uuid = player.getUniqueId();
        if (plugin.getCooldownFlyManager().isNotOnCooldown(uuid)) {
            plugin.getCooldownFlyManager().startCooldown(uuid);
        }

        if (plugin.getMainConfig().getBoolean("no-fall-damage", false)) {
            plugin.getNoFallDamageSet().add(uuid);
            new BukkitRunnable() {
                public void run() { plugin.getNoFallDamageSet().remove(uuid); }
            }.runTaskLater(plugin, 200L);
        }

        player.sendActionBar(MessageFormat.parseMessage(
                plugin.getMessageString("fly-time-ended", "&cFly time has ended.")));
    }

    public void restartFlyTimer(Player player, int newMaxTime) {
        plugin.getFlyRuntimeManager().restartTimer(player, newMaxTime);
    }

    public void cleanupFlyData(Player player) {
        plugin.getFlyRuntimeManager().cleanup(player);
    }

    public static boolean hasPluginFlyActive(UUID uuid) {
        return PowerFly.getInstance().getFlyRuntimeManager().hasActiveSession(uuid);
    }

    private List<Player> getTargets(CommandSender sender, String[] args) {
        List<Player> targets = new ArrayList<>();
        if (args.length == 0) {
            if (sender instanceof Player player) targets.add(player);
            return targets;
        }
        String name = args[0];
        if (name.equalsIgnoreCase("all")) {
            targets.addAll(Bukkit.getOnlinePlayers());
            return targets;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target != null) targets.add(target);
        return targets;
    }

    private String parseAction(String[] args) {
        if (args.length < 2) return "toggle";
        String act = args[1].toLowerCase();
        return (act.equals("on") || act.equals("off")) ? act : "toggle";
    }

    private void sendMessage(CommandSender sender, String key, String fallback) {
        sender.sendMessage(plugin.getPrefixedMessage(key, fallback));
    }

    private void sendRaw(CommandSender sender, String message) {
        String prefix = plugin.getMainConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
    }

    private void sendTargetFeedback(CommandSender sender, Player target) {
        boolean enabled = target.getAllowFlight();
        String key = enabled ? "fly-enabled-target" : "fly-disabled-target";
        String fallback = enabled ? "&aFly activated for &e{player}" : "&cFly disabled for &e{player}";
        String msg = plugin.getMessageString(key, fallback).replace("{player}", target.getName());
        sendRaw(sender, msg);
    }
}