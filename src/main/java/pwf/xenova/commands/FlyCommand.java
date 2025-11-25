package pwf.xenova.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.utils.MessageFormat;
import pwf.xenova.PowerFly;

import java.util.*;

public record FlyCommand(PowerFly plugin) implements CommandExecutor {

    private static final Map<UUID, BukkitRunnable> FLY_TIMERS = new HashMap<>();
    private static final Map<UUID, BossBar> FLY_BOSSBARS = new HashMap<>();
    private static final Set<UUID> PLUGIN_FLY_ACTIVE = new HashSet<>();

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!(sender instanceof ConsoleCommandSender)) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("powerfly.fly") && !player.hasPermission("powerfly.admin") && !player.isOp()) {
                    sendMessage(sender, "no-permission", "&cYou do not have permission to use this command.");
                    return true;
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
                boolean wasEnabled = target.getAllowFlight();
                if (wasEnabled) activatedCount++;
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
            boolean isSenderTarget = sender instanceof Player player && player.equals(target);
            if (!isSenderTarget) sendTargetFeedback(sender, target);
        }

        return true;
    }

    private boolean toggleFly(Player player, boolean enable, CommandSender sender, boolean isAllCommand) {
        UUID uuid = player.getUniqueId();
        int remaining = plugin.getFlyTimeManager().getRemainingFlyTime(uuid);

        if (enable && remaining <= 0) {
            boolean isSameSender = sender instanceof Player senderPlayer && senderPlayer.equals(player);

            if (isAllCommand) {
                return false;
            }

            if (!isSameSender) {
                String message = plugin.getMessageString("no-player-fly-time", "&c{player} has no fly time remaining.")
                        .replace("{player}", player.getName());
                String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
                sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
                return false;
            }

            int cooldownSeconds = plugin.getCooldownFlyManager().getRemainingCooldownSeconds(uuid);
            String message = plugin.getMessageString("fly-cooldown", "&cYou have used your fly time, wait &f{seconds}s &cto fly again.")
                    .replace("{seconds}", String.valueOf(cooldownSeconds));
            String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
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

        if (plugin.getControlFlyManager().isFlightBlockedInWorld(player.getWorld())) {
            sendMessage(player, "blacklist-worlds", "&cYou cannot fly in this world.");
            boolean isSameSender = sender instanceof Player senderPlayer && senderPlayer.equals(player);
            if (!isSameSender) {
                String message = plugin.getMessageString("blacklist-worlds-target", "&c{player} cannot fly in this world.")
                        .replace("{player}", player.getName());
                String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
                sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
            }
            return;
        }

        if (plugin.getControlFlyManager().isFlightBlockedInRegion(player)) {
            sendMessage(player, "fly-not-allowed-in-region", "&cYou cannot fly in this region.");
            boolean isSameSender = sender instanceof Player senderPlayer && senderPlayer.equals(player);
            if (!isSameSender) {
                String message = plugin.getMessageString("fly-not-allowed-in-region-target", "&c{player} cannot fly in this region.")
                        .replace("{player}", player.getName());
                String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
                sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
            }
            return;
        }

        if (plugin.getCombatFlyManager().isInCombat(player)) {
            sendMessage(player, "fly-in-combat", "&cYou cannot fly while in combat.");
            boolean isSameSender = sender instanceof Player senderPlayer && senderPlayer.equals(player);
            if (!isSameSender) {
                String message = plugin.getMessageString("fly-in-combat-target", "&c{player} cannot fly while in combat.")
                        .replace("{player}", player.getName());
                String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
                sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
            }
            return;
        }

        stopFlyTimer(player);
        removeFlyBar(player);

        player.setAllowFlight(true);
        player.setFlying(true);
        PLUGIN_FLY_ACTIVE.add(uuid);

        plugin.getSoundEffectsManager().playActivationEffects(player);
        sendMessage(player, "fly-enabled", "&aFly activated.");

        if (plugin.getConfig().getBoolean("show-bossbar", true)) showFlyBar(player, maxTime);
        startFlyTimer(player, maxTime);
    }

    private void disableFly(Player player, boolean fromEnd) {
        UUID uuid = player.getUniqueId();
        stopFlyTimer(player);
        removeFlyBar(player);
        PLUGIN_FLY_ACTIVE.remove(uuid);

        if (fromEnd) handleFlyEnd(player);

        player.setAllowFlight(false);
        player.setFlying(false);

        plugin.getSoundEffectsManager().playDeactivationEffects(player);
        sendMessage(player, "fly-disabled", "&cFly disabled.");
    }

    private void handleFlyEnd(Player player) {
        UUID uuid = player.getUniqueId();
        if (!plugin.getCooldownFlyManager().isOnCooldown(uuid)) {
            plugin.getCooldownFlyManager().startCooldown(uuid);
        }

        if (plugin.getConfig().getBoolean("no-fall-damage", true)) {
            plugin.getNoFallDamageSet().add(uuid);
            new BukkitRunnable() {
                public void run() {
                    plugin.getNoFallDamageSet().remove(uuid);
                }
            }.runTaskLater(plugin, 40L);
        }

        String raw = plugin.getMessages().getString("fly-time-ended", "&cFly time has ended.");
        player.sendActionBar(deserialize(raw));
    }

    private void startFlyTimer(Player player, int maxTime) {
        UUID uuid = player.getUniqueId();

        BukkitRunnable timer = new BukkitRunnable() {
            int remaining = maxTime;

            public void run() {
                if (player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
                        player.getGameMode() == org.bukkit.GameMode.SPECTATOR ||
                        !player.isOnline() || !player.getAllowFlight()) {
                    cancel();
                    FLY_TIMERS.remove(uuid);
                    removeFlyBar(player);
                    return;
                }

                remaining--;
                plugin.getFlyTimeManager().setFlyTime(uuid, remaining);

                if (remaining > 0) {
                    if (plugin.getConfig().getBoolean("show-actionbar", true))
                        sendFlyTimeActionBar(player, remaining);

                    if (plugin.getConfig().getBoolean("show-bossbar", true))
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

        if (plugin.getConfig().getBoolean("no-fall-damage", true)) {
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

    private void showFlyBar(Player player, int maxTime) {
        UUID uuid = player.getUniqueId();
        if (FLY_BOSSBARS.containsKey(uuid)) return;

        String raw = plugin.getMessages().getString("bossbar-fly-time", "&eFly time: &6{time}s")
                .replace("{time}", String.valueOf(maxTime));
        BarColor color = BarColor.valueOf(plugin.getConfig().getString("bossbar-color", "BLUE").toUpperCase());
        BarStyle style = BarStyle.valueOf(plugin.getConfig().getString("bossbar-style", "SOLID").toUpperCase());

        BossBar bar = Bukkit.createBossBar(serialize(raw), color, style);
        bar.addPlayer(player);
        bar.setProgress(1.0);

        FLY_BOSSBARS.put(uuid, bar);
    }

    private void updateFlyBar(Player player, int remaining, int maxTime) {
        BossBar bar = FLY_BOSSBARS.get(player.getUniqueId());
        if (bar == null) return;

        bar.setProgress(Math.max(0, (double) remaining / maxTime));
        String raw = plugin.getMessages().getString("bossbar-fly-time", "&eFly time: &6{time}s")
                .replace("{time}", String.valueOf(remaining));
        bar.setTitle(serialize(raw));
    }

    private void removeFlyBar(Player player) {
        BossBar bar = FLY_BOSSBARS.remove(player.getUniqueId());
        if (bar != null) bar.removeAll();
    }

    private List<Player> getTargets(CommandSender sender, String[] args) {
        List<Player> targets = new ArrayList<>();

        if (args.length == 0) {
            if (sender instanceof ConsoleCommandSender) {
                return targets;
            }
            if (sender instanceof Player player) {
                targets.add(player);
            }
            return targets;
        }

        String name = args[0];

        if (name.equalsIgnoreCase("all")) {
            if (sender instanceof ConsoleCommandSender ||
                    sender.hasPermission("powerfly.admin") ||
                    sender.isOp()) {
                targets.addAll(Bukkit.getOnlinePlayers());
            }
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
        String prefix = plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r");
        sender.sendMessage(MessageFormat.parseMessageWithPrefix(prefix, message));
    }

    private void sendFlyTimeActionBar(Player player, int time) {
        String raw = plugin.getMessages().getString("actionbar-fly-time", "&eFly time: &6{time}s")
                .replace("{time}", String.valueOf(time));
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

    public static boolean hasPluginFlyActive(UUID uuid) {
        return PLUGIN_FLY_ACTIVE.contains(uuid);
    }
}