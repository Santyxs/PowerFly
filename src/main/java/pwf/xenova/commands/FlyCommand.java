package pwf.xenova.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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

    private static final Map<UUID, BukkitRunnable> flyTimers = new HashMap<>();
    private static final Map<UUID, BossBar> flyBossBars = new HashMap<>();

    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        List<Player> targets = new ArrayList<>();
        String action = "toggle";

        int nameArgIndex = 0;
        if (args.length > 0 && args[0].equalsIgnoreCase("fly")) {
            nameArgIndex = 1;
        }

        if (args.length > nameArgIndex) {
            String name = args[nameArgIndex];

            if (args.length > nameArgIndex + 1) {
                String act = args[nameArgIndex + 1].toLowerCase();
                if (act.equals("on") || act.equals("off")) action = act;
            }

            if (name.equalsIgnoreCase("all")) {
                if (!sender.hasPermission("powerfly.admin")) {
                    sender.sendMessage(plugin.getPrefixedMessage("no-permission",
                            "&cYou do not have permission to fly all players."));
                    return true;
                }
                targets.addAll(Bukkit.getOnlinePlayers());
            } else {
                Player p = Bukkit.getPlayerExact(name);
                if (p == null) {
                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        if (pl.getName().equalsIgnoreCase(name)) {
                            p = pl;
                            break;
                        }
                    }
                }
                if (p != null) targets.add(p);
            }

            if (targets.isEmpty()) {
                sender.sendMessage(plugin.getPrefixedMessage("player-not-found",
                        "&cPlayer not found or offline."));
                return true;
            }

        } else if (sender instanceof Player player) {
            targets.add(player);
        } else {
            sender.sendMessage(plugin.getPrefixedMessage("no-console",
                    "&cOnly players can use this command."));
            return true;
        }

        for (Player target : targets) {
            if (target.equals(sender) && !target.hasPermission("powerfly.fly")) {
                target.sendMessage(plugin.getPrefixedMessage("no-permission",
                        "&cYou do not have permission to use this command."));
                continue;
            }
            toggleFly(target, sender, action);
        }

        return true;
    }

    private void toggleFly(Player player, CommandSender sender, String action) {
        CooldownFlyManager cooldownManager = plugin.getCooldownFlyManager();
        SoundEffectsManager soundManager = plugin.getSoundEffectsManager();

        int availableFlyTime = plugin.getFlyTimeManager().getRemainingFlyTime(player.getUniqueId());
        if (availableFlyTime <= 0) {
            player.sendMessage(plugin.getPrefixedMessage("no-fly-time",
                    "&cYou don't have any fly time available."));
            return;
        }

        boolean enable;
        if (action.equals("toggle")) {
            enable = !player.getAllowFlight();
        } else enable = action.equals("on");

        if (enable) {
            enableFly(player, soundManager, availableFlyTime);
        } else {
            disableFly(player, soundManager, false);
        }

        if (!player.equals(sender)) {
            sender.sendMessage(plugin.getPrefixedMessage("fly-other",
                    "&aFly toggled for &f" + player.getName()));
        }
    }

    private void disableFly(Player player, SoundEffectsManager soundManager, boolean fromEnd) {
        stopFlyTimer(player);
        removeFlyBar(player);

        if (fromEnd) {
            UUID uuid = player.getUniqueId();
            CooldownFlyManager cooldownManager = plugin.getCooldownFlyManager();
            if (!cooldownManager.isOnCooldown(uuid)) cooldownManager.startCooldown(uuid);

            if (plugin.getConfig().getBoolean("no-fall-damage", true)) {
                plugin.getNoFallDamageSet().add(uuid);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getNoFallDamageSet().remove(uuid);
                    }
                }.runTaskLater(plugin, 40L);
            }

            String raw = plugin.getMessages().getString("fly-time-ended", "&cFly time has ended.");
            Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
            player.sendActionBar(message);
        }

        player.setAllowFlight(false);
        player.setFlying(false);
        soundManager.playDeactivationEffects(player);
        player.sendMessage(plugin.getPrefixedMessage("fly-disabled", "&cFly disabled."));
    }

    private void enableFly(Player player, SoundEffectsManager soundManager, int maxFlyTime) {
        stopFlyTimer(player);

        player.setAllowFlight(true);
        player.setFlying(true);
        soundManager.playActivationEffects(player);
        player.sendMessage(plugin.getPrefixedMessage("fly-enabled", "&aFly activated."));

        if (plugin.getConfig().getBoolean("show-bossbar", true)) {
            showFlyBar(player, maxFlyTime);
        }

        startFlyTimer(player, maxFlyTime);
    }

    private void startFlyTimer(Player player, int maxTime) {
        UUID uuid = player.getUniqueId();
        BukkitRunnable timer = new BukkitRunnable() {
            long lastSecond = System.currentTimeMillis();

            public void run() {
                if (!player.isOnline() || !player.getAllowFlight()) {
                    cancel();
                    return;
                }

                if (System.currentTimeMillis() - lastSecond >= 1000) {
                    lastSecond = System.currentTimeMillis();

                    plugin.getFlyTimeManager().delFlyTime(uuid, 1);
                    int remaining = plugin.getFlyTimeManager().getRemainingFlyTime(uuid);

                    if (plugin.getConfig().getBoolean("show-actionbar", true)) {
                        String raw = plugin.getConfig().getString("actionbar-fly-time", "&eFly time: &6{time}s")
                                .replace("{time}", String.valueOf(remaining));
                        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
                        player.sendActionBar(message);
                    }

                    if (plugin.getConfig().getBoolean("show-bossbar", true)) {
                        String rawBar = plugin.getConfig().getString("bossbar-fly-time", "&eFly time: &6{time}s")
                                .replace("{time}", String.valueOf(remaining));
                        updateFlyBar(player, remaining, maxTime, rawBar);
                    }

                    if (remaining <= 0) {
                        endFly(player);
                    }
                }
            }
        };

        timer.runTaskTimer(plugin, 0L, 20L);
        flyTimers.put(uuid, timer);
    }

    private void endFly(Player player) {
        plugin.getSoundEffectsManager().playTimeEndEffects(player);

        if (plugin.getConfig().getBoolean("no-fall-damage", true)) {
            UUID uuid = player.getUniqueId();
            plugin.getNoFallDamageSet().add(uuid);

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getNoFallDamageSet().remove(uuid);
                }
            }.runTaskLater(plugin, 200L);
        }

        disableFly(player, plugin.getSoundEffectsManager(), true);
    }

    private void stopFlyTimer(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitRunnable timer = flyTimers.remove(uuid);
        if (timer != null) timer.cancel();
    }

    private void showFlyBar(Player player, int maxTime) {
        UUID uuid = player.getUniqueId();
        if (flyBossBars.containsKey(uuid)) return;

        String raw = plugin.getConfig().getString("bossbar-fly-time", "&eFly time: &6{time}s")
                .replace("{time}", String.valueOf(maxTime));

        String colorName = plugin.getConfig().getString("bossbar-color", "BLUE").toUpperCase();
        String styleName = plugin.getConfig().getString("bossbar-style", "SOLID").toUpperCase();

        BarColor color = BarColor.valueOf(colorName);
        BarStyle style = BarStyle.valueOf(styleName);

        String title = LegacyComponentSerializer.legacySection().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(raw)
        );

        BossBar bar = Bukkit.createBossBar(title, color, style);
        bar.addPlayer(player);
        bar.setProgress(1.0);
        flyBossBars.put(uuid, bar);
    }

    private void updateFlyBar(Player player, int remainingTime, int maxTime, String rawMessage) {
        BossBar bar = flyBossBars.get(player.getUniqueId());
        if (bar != null) {
            double progress = Math.max(0, (double) remainingTime / maxTime);
            bar.setProgress(progress);

            String title = LegacyComponentSerializer.legacySection().serialize(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(rawMessage)
            );
            bar.setTitle(title);
        }
    }

    private void removeFlyBar(Player player) {
        UUID uuid = player.getUniqueId();
        BossBar bar = flyBossBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
    }

    public void resumeFly(Player player, int remainingTime) {
        enableFly(player, plugin.getSoundEffectsManager(), remainingTime);
    }
}
