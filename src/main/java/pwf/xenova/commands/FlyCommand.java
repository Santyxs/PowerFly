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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pwf.xenova.PowerFly;
import pwf.xenova.managers.*;

import java.util.*;

public record FlyCommand(PowerFly plugin) implements CommandExecutor {

    private static final Map<UUID, BukkitRunnable> FLY_TIMERS = new HashMap<>();
    private static final Map<UUID, BossBar> FLY_BOSSBARS = new HashMap<>();
    private static final String PERMISSION_ADMIN = "powerfly.admin";
    private static final String PERMISSION_FLY = "powerfly.fly";

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        List<Player> targets = getTargets(sender, args);
        if (targets.isEmpty()) return true;

        String action = parseAction(args);
        boolean isAll = args.length > 0 && args[0].equalsIgnoreCase("all");
        int activatedCount = 0;
        int deactivatedCount = 0;

        for (Player target : targets) {
            if (target.equals(sender) && !target.hasPermission(PERMISSION_FLY)) {
                sendMessage(target, "no-permission", "&cYou do not have permission to use this command.");
                continue;
            }

            boolean wasEnabled = toggleFly(target, sender, action);
            if (wasEnabled) activatedCount++;
            else deactivatedCount++;
        }

        if (!targets.isEmpty() && args.length > 0) {
            if (isAll) {
                if (activatedCount > 0) sendMessage(sender, "fly-enabled-all", "&aFly activated for all players.");
                if (deactivatedCount > 0) sendMessage(sender, "fly-disabled-all", "&cFly disabled for all players.");
            } else if (targets.size() == 1) {
                Player target = targets.getFirst();
                boolean isEnabled = target.getAllowFlight();
                String messageKey = isEnabled ? "fly-enabled-target" : "fly-disabled-target";
                String defaultMsg = isEnabled
                        ? "&aFly activated for {player}"
                        : "&cFly disabled for {player}";
                String msg = plugin.getMessages().getString(messageKey, defaultMsg)
                        .replace("{player}", target.getName());
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(plugin.getConfig().getString("prefix", "&7[&ePower&fFly&7] &r") + msg));
            }
        }

        return true;
    }

    private List<Player> getTargets(CommandSender sender, String[] args) {
        List<Player> targets = new ArrayList<>();
        if (args.length == 0 && sender instanceof Player player) {
            targets.add(player);
            return targets;
        }

        String name = args.length > 0 ? args[0] : "";
        if (name.equalsIgnoreCase("all")) {
            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                sendMessage(sender, "no-permission", "&cYou do not have permission to fly all players.");
                return targets;
            }
            targets.addAll(Bukkit.getOnlinePlayers());
            return targets;
        }

        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().equalsIgnoreCase(name)) {
                    player = p;
                    break;
                }
            }
        }
        if (player != null) targets.add(player);
        else sendMessage(sender, "player-not-found", "&cPlayer not found or offline.");

        return targets;
    }

    private String parseAction(String[] args) {
        if (args.length < 2) return "toggle";
        String act = args[1].toLowerCase();
        return act.equals("on") || act.equals("off") ? act : "toggle";
    }

    private boolean toggleFly(Player player, CommandSender sender, String action) {
        int remainingTime = plugin.getFlyTimeManager().getRemainingFlyTime(player.getUniqueId());
        if (remainingTime <= 0) {
            sendMessage(player, "no-fly-time", "&cYou don't have any fly time available.");
            return false;
        }

        boolean enable = action.equals("toggle") ? !player.getAllowFlight() : action.equals("on");
        if (enable) enableFly(player, remainingTime);
        else disableFly(player, false);
        return enable;
    }

    private void enableFly(Player player, int maxTime) {
        UUID uuid = player.getUniqueId();

        stopFlyTimer(player);
        removeFlyBar(player);

        player.setAllowFlight(true);
        player.setFlying(true);
        plugin.getSoundEffectsManager().playActivationEffects(player);
        sendMessage(player, "fly-enabled", "&aFly activated.");

        if (plugin.getConfig().getBoolean("show-bossbar", true)) showFlyBar(player, maxTime);
        startFlyTimer(player, maxTime);
    }

    private void disableFly(Player player, boolean fromEnd) {
        stopFlyTimer(player);
        removeFlyBar(player);

        if (fromEnd) handleFlyEnd(player);

        player.setAllowFlight(false);
        player.setFlying(false);
        plugin.getSoundEffectsManager().playDeactivationEffects(player);
        sendMessage(player, "fly-disabled", "&cFly disabled.");
    }

    private void handleFlyEnd(Player player) {
        UUID uuid = player.getUniqueId();
        if (!plugin.getCooldownFlyManager().isOnCooldown(uuid)) plugin.getCooldownFlyManager().startCooldown(uuid);

        if (plugin.getConfig().getBoolean("no-fall-damage", true)) {
            plugin.getNoFallDamageSet().add(uuid);
            new BukkitRunnable() {
                public void run() { plugin.getNoFallDamageSet().remove(uuid); }
            }.runTaskLater(plugin, 40L);
        }

        String raw = plugin.getMessages().getString("fly-time-ended", "&cFly time has ended.");
        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        player.sendActionBar(message);
    }

    private void startFlyTimer(Player player, int maxTime) {
        UUID uuid = player.getUniqueId();

        if (plugin.getConfig().getBoolean("show-actionbar", true)) {
            String raw = plugin.getMessages().getString("actionbar-fly-time", "&eFly time: &6{time}s")
                    .replace("{time}", String.valueOf(maxTime));
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(raw));
        }

        BukkitRunnable timer = new BukkitRunnable() {
            public void run() {
                if (!player.isOnline() || !player.getAllowFlight()) {
                    cancel();
                    return;
                }

                int remaining = plugin.getFlyTimeManager().getRemainingFlyTime(uuid);

                if (remaining <= 0) {
                    endFly(player);
                    return;
                }

                if (plugin.getConfig().getBoolean("show-actionbar", true)) {
                    String raw = plugin.getMessages().getString("actionbar-fly-time", "&eFly time: &6{time}s")
                            .replace("{time}", String.valueOf(remaining));
                    player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(raw));
                }

                if (plugin.getConfig().getBoolean("show-bossbar", true)) {
                    String rawBar = plugin.getMessages().getString("bossbar-fly-time", "&eFly time: &6{time}s")
                            .replace("{time}", String.valueOf(remaining));
                    updateFlyBar(player, remaining, maxTime, rawBar);
                }
            }
        };

        timer.runTaskTimer(plugin, 20L, 20L);
        FLY_TIMERS.put(uuid, timer);
    }

    private void endFly(Player player) {
        plugin.getSoundEffectsManager().playTimeEndEffects(player);
        disableFly(player, true);

        if (plugin.getConfig().getBoolean("no-fall-damage", true)) {
            UUID uuid = player.getUniqueId();
            plugin.getNoFallDamageSet().add(uuid);
            new BukkitRunnable() {
                public void run() { plugin.getNoFallDamageSet().remove(uuid); }
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

    private void updateFlyBar(Player player, int remainingTime, int maxTime, String rawMessage) {
        BossBar bar = FLY_BOSSBARS.get(player.getUniqueId());
        if (bar != null) {
            bar.setProgress(Math.max(0, (double) remainingTime / maxTime));
            bar.setTitle(serialize(rawMessage));
        }
    }

    private void removeFlyBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = FLY_BOSSBARS.remove(uuid);
        if (bar != null) bar.removeAll();
    }

    private Component deserialize(String raw) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    private String serialize(String raw) {
        return LegacyComponentSerializer.legacySection().serialize(deserialize(raw));
    }

    private void sendMessage(CommandSender sender, String path, String fallback) {
        sender.sendMessage(plugin.getPrefixedMessage(path, fallback));
    }

    public void resumeFly(Player player, int remainingTime) {
        enableFly(player, remainingTime);
    }
}
