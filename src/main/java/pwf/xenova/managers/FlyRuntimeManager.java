package pwf.xenova.managers;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pwf.xenova.PowerFly;
import pwf.xenova.utils.MessageFormat;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FlyRuntimeManager {

    private final PowerFly plugin;
    private final Map<UUID, BukkitRunnable> flyTimers = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> flyBossBars = new ConcurrentHashMap<>();
    private final Set<UUID> activeSessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> warned10s = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final int INFINITE = -1;

    public FlyRuntimeManager(PowerFly plugin) {
        this.plugin = plugin;
    }

    public boolean hasActiveSession(UUID uuid) {
        return activeSessions.contains(uuid);
    }

    public void addSession(UUID uuid) {
        activeSessions.add(uuid);
    }

    public void removeSession(UUID uuid) {
        activeSessions.remove(uuid);
        warned10s.remove(uuid);
    }

    public void startTimer(Player player, int initialMaxTime) {
        UUID uuid = player.getUniqueId();

        BukkitRunnable timer = new BukkitRunnable() {
            public void run() {
                if (!player.isOnline() || !hasActiveSession(uuid)) {
                    cleanup(player);
                    cancel();
                    return;
                }

                int remaining = plugin.getFlyTimeManager().getRemainingFlyTime(uuid);

                if (remaining != INFINITE) {
                    if (plugin.getFlyTimeOnGroundManager().shouldDecreaseFlyTime(player)) {
                        remaining--;
                        plugin.getFlyTimeManager().setFlyTimeInternal(uuid, remaining);
                    }
                }

                if (remaining == 10 && warned10s.add(uuid)) {
                    player.sendMessage(plugin.getPrefixedMessage("fly-time-warning", "&6⚠ &eThere are &c10s &eof fly remaining!"));
                }

                if (remaining > 0 || remaining == INFINITE) {
                    handleActionBar(player, remaining);
                    if (plugin.getMainConfig().getBoolean("show-bossbar", true)) {
                        updateBossBar(player, remaining, initialMaxTime);
                    }
                } else {
                    plugin.getFlyCommand().endFly(player);
                    cancel();
                    flyTimers.remove(uuid);
                }
            }
        };

        timer.runTaskTimer(plugin, 20L, 20L);
        flyTimers.put(uuid, timer);
    }

    public void stopTimer(Player player) {
        BukkitRunnable timer = flyTimers.remove(player.getUniqueId());
        if (timer != null) timer.cancel();
    }

    public void restartTimer(Player player, int newMaxTime) {
        stopTimer(player);
        removeBossBar(player);
        warned10s.remove(player.getUniqueId());
        if (plugin.getMainConfig().getBoolean("show-bossbar", true)) {
            showBossBar(player, newMaxTime);
        }
        startTimer(player, newMaxTime);
    }

    public void showBossBar(Player player, int maxTime) {
        UUID uuid = player.getUniqueId();
        if (flyBossBars.containsKey(uuid)) return;

        String display = MessageFormat.formatTime(maxTime);
        String raw = plugin.getMessageString("bossbar-fly-time", "&eFly time: &6{fly_time}")
                .replace("{fly_time}", display);

        BarColor color = BarColor.valueOf(plugin.getMainConfig().getString("bossbar-color", "BLUE").toUpperCase());
        BarStyle style = BarStyle.valueOf(plugin.getMainConfig().getString("bossbar-style", "SOLID").toUpperCase());

        BossBar bar = Bukkit.createBossBar(MessageFormat.toConsoleString(MessageFormat.parseMessage(raw)), color, style);
        bar.addPlayer(player);
        bar.setProgress(1.0);

        flyBossBars.put(uuid, bar);
    }

    public void removeBossBar(Player player) {
        BossBar bar = flyBossBars.remove(player.getUniqueId());
        if (bar != null) bar.removeAll();
    }

    private void updateBossBar(Player player, int remaining, int maxTime) {
        BossBar bar = flyBossBars.get(player.getUniqueId());
        if (bar == null) return;

        double progress;
        if (remaining == INFINITE) {
            progress = 1.0;
        } else {
            int currentMax = Math.max(remaining, maxTime);
            progress = currentMax <= 0 ? 0.0 : Math.clamp((double) remaining / currentMax, 0.0, 1.0);
        }

        bar.setProgress(progress);

        String display = MessageFormat.formatTime(remaining);
        String raw = plugin.getMessageString("bossbar-fly-time", "&eFly time: &6{fly_time}")
                .replace("{fly_time}", display);
        bar.setTitle(MessageFormat.toConsoleString(MessageFormat.parseMessage(raw)));
    }

    private void handleActionBar(Player player, int remaining) {
        if (!plugin.getMainConfig().getBoolean("show-actionbar", true)) return;

        boolean onGround = player.getLocation()
                .clone()
                .subtract(0, 0.1, 0)
                .getBlock()
                .getType()
                .isSolid();
        boolean showOnGround = plugin.getMainConfig().getBoolean("show-actionbar-on-ground", false);

        if (!onGround || showOnGround) {
            String display = MessageFormat.formatTime(remaining);
            String raw = plugin.getMessageString("actionbar-fly-time", "&eFly time: &6{fly_time}")
                    .replace("{fly_time}", display);
            player.sendActionBar(MessageFormat.parseMessage(raw));
        }
    }

    public void cleanup(Player player) {
        stopTimer(player);
        removeBossBar(player);
        removeSession(player.getUniqueId());
    }
}