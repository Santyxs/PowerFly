package pwf.xenova.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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

    private static final Map<UUID, BukkitRunnable> FLY_TIMERS = new HashMap<>();
    private static final Map<UUID, BossBar> FLY_BOSSBARS = new HashMap<>();
    private static final Set<UUID> PLUGIN_FLY_ACTIVE = new HashSet<>();
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

            if (args.length > 0) {
                String targetName = args[0];
                boolean isSelf = targetName.equalsIgnoreCase(player.getName());

                if (!isSelf) {
                    if (!player.hasPermission("powerfly.fly.others") && !player.hasPermission("powerfly.admin") && !player.isOp()) {
                        sendMessage(sender, "no-permission-others", "&cYou do not have permission to toggle fly for others.");
                        return true;
                    }
                }
            }
        }

        List<Player> targets = getTargets(sender, args);

        if (targets.isEmpty()) {
            if (args.length > 0 && !args[0].equalsIgnoreCase("all")) {
                sendMessage(sender, "player-not-found", "&cPlayer not found.");
                return true;
            }
            sendMessage(sender, "no-player-specified", "&cYou must specify a player name.");
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
                boolean isSameSender = sender instanceof Player senderPlayer && senderPlayer.equals(player);

                if (isSameSender) {
                    sendMessage(player, "fly-mode-error", "&cYou cannot use this command in Creative or Spectator mode.");
                } else {
                    String message = plugin.getMessageString("fly-mode-error-target", "&c{player} cannot use fly because they are in Creative or Spectator mode.")
                            .replace("{player}", player.getName());
                    String prefix = plugin.getFileManager().getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
                    sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
                }
            }
            return false;
        }

        UUID uuid = player.getUniqueId();
        int remaining = plugin.getFlyTimeManager().getRemainingFlyTime(uuid);

        if (enable && remaining <= 0 && remaining != INFINITE_FLY_TIME) {
            boolean isSameSender = sender instanceof Player senderPlayer && senderPlayer.equals(player);
            if (isAllCommand) return false;

            if (!isSameSender) {
                String message = plugin.getMessageString("no-player-fly-time", "&c{player} has no fly time remaining.")
                        .replace("{player}", player.getName());
                String prefix = plugin.getFileManager().getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
                sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
                return false;
            }

            String cooldownFormatted = plugin.getCooldownFlyManager().getRemainingCooldownFormatted(uuid);
            String message = plugin.getMessageString("fly-cooldown", "&cYou have used your fly time, wait &f{cooldown_time} &cto fly again.")
                    .replace("{cooldown_time}", cooldownFormatted);
            String prefix = plugin.getFileManager().getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
            player.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
            return false;
        }

        if (enable) {
            enableFly(player, remaining, sender);
        } else {
            disableFly(player, false);
        }

        return true;
    }

    private void enableFly(Player player, int maxTime, CommandSender sender) {
        UUID uuid = player.getUniqueId();

        if (plugin.getClaimFlyManager().cannotFlyHere(player, player.getLocation())) {
            player.sendMessage(plugin.getPrefixedMessage("fly-not-allowed-in-claim", "&cYou cannot fly in this claim or town."));
            return;
        }

        if (plugin.getControlFlyManager().isFlightBlockedInWorld(player)) {
            sendMessage(player, "blacklist-worlds", "&cYou cannot fly in this world.");
            return;
        }

        if (plugin.getControlFlyManager().isFlightBlockedInRegion(player)) {
            sendMessage(player, "fly-not-allowed-in-region", "&cYou cannot fly in this region.");
            return;
        }

        if (plugin.getCombatFlyManager().isInCombat(player)) {
            sendMessage(player, "fly-in-combat", "&cYou cannot fly while in combat.");
            return;
        }

        stopFlyTimer(player);
        removeFlyBar(player);

        player.setAllowFlight(true);
        player.setFlying(true);
        PLUGIN_FLY_ACTIVE.add(uuid);

        plugin.getSoundEffectsManager().playActivationEffects(player);
        sendMessage(player, "fly-enabled", "&aFly activated.");

        if (plugin.getFileManager().getConfig().getBoolean("show-bossbar", true)) showFlyBar(player, maxTime);
        startFlyTimer(player, maxTime);
    }

    private void disableFly(Player player, boolean fromEnd) {
        UUID uuid = player.getUniqueId();
        stopFlyTimer(player);
        removeFlyBar(player);
        PLUGIN_FLY_ACTIVE.remove(uuid);

        if (fromEnd) handleFlyEnd(player);

        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        plugin.getSoundEffectsManager().playDeactivationEffects(player);
        sendMessage(player, "fly-disabled", "&cFly disabled.");
    }

    private void handleFlyEnd(Player player) {
        UUID uuid = player.getUniqueId();
        if (!plugin.getCooldownFlyManager().isOnCooldown(uuid)) {
            plugin.getCooldownFlyManager().startCooldown(uuid);
        }

        if (plugin.getFileManager().getConfig().getBoolean("no-fall-damage", true)) {
            plugin.getNoFallDamageSet().add(uuid);
            new BukkitRunnable() {
                public void run() {
                    plugin.getNoFallDamageSet().remove(uuid);
                }
            }.runTaskLater(plugin, 40L);
        }

        String raw = plugin.getMessageString("fly-time-ended", "&cFly time has ended.");
        player.sendActionBar(deserialize(raw));
    }

    private void startFlyTimer(Player player, int maxTime) {
        UUID uuid = player.getUniqueId();

        BukkitRunnable timer = new BukkitRunnable() {
            int remaining = maxTime;

            public void run() {
                if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                    plugin.getSoundEffectsManager().playDeactivationEffects(player);

                    cleanupFlyData(player);

                    sendMessage(player, "fly-disabled", "&cFly disabled.");

                    cancel();
                    return;
                }

                if (!player.isOnline() || !player.getAllowFlight()) {
                    cleanupFlyData(player);
                    cancel();
                    return;
                }

                if (remaining != INFINITE_FLY_TIME) {
                    remaining--;
                    plugin.getFlyTimeManager().setFlyTime(uuid, remaining);
                }

                if (remaining == 10) {
                    sendMessage(player, "fly-time-warning", "&6⚠ &eThere are &c10s &eof fly remaining!");
                }

                if (remaining > 0 || remaining == INFINITE_FLY_TIME) {
                    if (plugin.getFileManager().getConfig().getBoolean("show-actionbar", true))
                        sendFlyTimeActionBar(player, remaining);

                    if (plugin.getFileManager().getConfig().getBoolean("show-bossbar", true))
                        updateFlyBar(player, remaining, maxTime);
                } else {
                    endFly(player);
                    cancel();
                    FLY_TIMERS.remove(uuid);
                }
            }
        };

        timer.runTaskTimer(plugin, 20L, 20L);
        FLY_TIMERS.put(uuid, timer);
    }

    private void endFly(Player player) {
        disableFly(player, true);
        plugin.getSoundEffectsManager().playTimeEndEffects(player);

        if (plugin.getFileManager().getConfig().getBoolean("no-fall-damage", true)) {
            UUID uuid = player.getUniqueId();
            plugin.getNoFallDamageSet().add(uuid);
            new BukkitRunnable() {
                public void run() {
                    plugin.getNoFallDamageSet().remove(uuid);
                }
            }.runTaskLater(plugin, 200L);
        }
    }

    private void stopFlyTimer(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitRunnable timer = FLY_TIMERS.remove(uuid);
        if (timer != null) timer.cancel();
    }

    public void showFlyBar(Player player, int maxTime) {
        UUID uuid = player.getUniqueId();
        if (FLY_BOSSBARS.containsKey(uuid)) return;

        String display = plugin.getFlyTimeManager().formatTime(maxTime);
        String raw = plugin.getMessageString("bossbar-fly-time", "&eFly time: &6{fly_time}")
                .replace("{fly_time}", display);

        BarColor color = BarColor.valueOf(plugin.getFileManager().getConfig().getString("bossbar-color", "BLUE").toUpperCase());
        BarStyle style = BarStyle.valueOf(plugin.getFileManager().getConfig().getString("bossbar-style", "SOLID").toUpperCase());

        BossBar bar = Bukkit.createBossBar(serialize(raw), color, style);
        bar.addPlayer(player);
        bar.setProgress(1.0);

        FLY_BOSSBARS.put(uuid, bar);
    }

    private void updateFlyBar(Player player, int remaining, int maxTime) {
        BossBar bar = FLY_BOSSBARS.get(player.getUniqueId());
        if (bar == null) return;

        double progress = (remaining == INFINITE_FLY_TIME) ? 1.0 : Math.max(0, (double) remaining / maxTime);
        bar.setProgress(progress);

        String display = plugin.getFlyTimeManager().formatTime(remaining);
        String raw = plugin.getMessageString("bossbar-fly-time", "&eFly time: &6{fly_time}").replace("{fly_time}", display);
        bar.setTitle(serialize(raw));
    }

    private void removeFlyBar(Player player) {
        BossBar bar = FLY_BOSSBARS.remove(player.getUniqueId());
        if (bar != null) bar.removeAll();
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
        if (target != null) {
            targets.add(target);
        }
        return targets;
    }

    private String parseAction(String[] args) {
        if (args.length < 2) return "toggle";
        String act = args[1].toLowerCase();
        return (act.equals("on") || act.equals("off")) ? act : "toggle";
    }

    private void sendMessage(CommandSender sender, String path, String fallback) {
        sender.sendMessage(plugin.getPrefixedMessage(path, fallback));
    }

    private void sendTargetFeedback(CommandSender sender, Player target) {
        boolean enabled = target.getAllowFlight();
        String messageKey = enabled ? "fly-enabled-target" : "fly-disabled-target";
        String fallback = enabled ? "&aFly activated for &e{player}" : "&cFly disabled for &e{player}";
        String message = plugin.getMessageString(messageKey, fallback).replace("{player}", target.getName());
        String prefix = plugin.getFileManager().getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
    }

    private void sendFlyTimeActionBar(Player player, int time) {
        String display = plugin.getFlyTimeManager().formatTime(time);
        String raw = plugin.getMessageString("actionbar-fly-time", "&eFly time: &6{fly_time}")
                .replace("{fly_time}", display);
        player.sendActionBar(deserialize(raw));
    }

    private Component deserialize(String raw) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    private String serialize(String raw) {
        return LegacyComponentSerializer.legacySection().serialize(deserialize(raw));
    }

    public void cleanupFlyData(Player player) {
        stopFlyTimer(player);
        removeFlyBar(player);
        PLUGIN_FLY_ACTIVE.remove(player.getUniqueId());
    }

    public void cancelFlyTimer(UUID uuid) {
        BukkitRunnable timer = FLY_TIMERS.remove(uuid);
        if (timer != null) timer.cancel();
    }

    public void cancelFlyTimer(Player player) {
        cancelFlyTimer(player.getUniqueId());
    }

    public boolean isTimerActive(UUID uuid) {
        return FLY_TIMERS.containsKey(uuid);
    }

    public void restartFlyTimer(Player player, int newMaxTime) {
        stopFlyTimer(player);
        removeFlyBar(player);
        if (plugin.getFileManager().getConfig().getBoolean("show-bossbar", true)) {
            showFlyBar(player, newMaxTime);
        }
        startFlyTimer(player, newMaxTime);
    }

    public static boolean hasPluginFlyActive(UUID uuid) {
        return PLUGIN_FLY_ACTIVE.contains(uuid);
    }
}
