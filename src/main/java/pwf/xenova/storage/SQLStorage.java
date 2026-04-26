package pwf.xenova.storage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pwf.xenova.PowerFly;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SQLStorage implements StorageInterface {

    private static final String DB_URL = "jdbc:sqlite:%s";
    private static final String TABLE_NAME = "powerfly_players";

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    "uuid TEXT PRIMARY KEY," +
                    "name TEXT," +
                    "time INTEGER DEFAULT 0," +
                    "cooldown INTEGER DEFAULT 0" +
                    ");";

    private static final String QUERY_GET_TIME =
            "SELECT time FROM " + TABLE_NAME + " WHERE uuid = ?;";
    private static final String QUERY_GET_COOLDOWN =
            "SELECT cooldown FROM " + TABLE_NAME + " WHERE uuid = ?;";
    private static final String QUERY_GET_NAME =
            "SELECT name FROM " + TABLE_NAME + " WHERE uuid = ?;";
    private static final String QUERY_LOAD_ALL =
            "SELECT uuid, time FROM " + TABLE_NAME + ";";
    private static final String QUERY_LOAD_ALL_COOLDOWNS =
            "SELECT uuid, cooldown FROM " + TABLE_NAME + " WHERE cooldown > 0;";
    private static final String QUERY_INSERT_IGNORE =
            "INSERT OR IGNORE INTO " + TABLE_NAME + " (uuid, name, time, cooldown) VALUES (?, ?, ?, 0);";
    private static final String QUERY_DELETE =
            "DELETE FROM " + TABLE_NAME + " WHERE uuid = ?;";
    private static final String QUERY_SET_TIME =
            "INSERT INTO " + TABLE_NAME + " (uuid, name, time, cooldown) VALUES (?, ?, ?, 0) " +
                    "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, time = excluded.time;";
    private static final String QUERY_SET_COOLDOWN =
            "INSERT INTO " + TABLE_NAME + " (uuid, name, time, cooldown) VALUES (?, ?, 0, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, cooldown = excluded.cooldown;";
    private static final String QUERY_ADD_TIME =
            "UPDATE " + TABLE_NAME + " SET time = time + ? WHERE uuid = ?;";
    private static final String QUERY_DEL_TIME =
            "UPDATE " + TABLE_NAME + " SET time = MAX(time - ?, 0) WHERE uuid = ?;";
    private static final String QUERY_UPDATE_NAME =
            "UPDATE " + TABLE_NAME + " SET name = ? WHERE uuid = ?;";

    private final PowerFly plugin;
    private Connection connection;

    private final Map<UUID, String> nameCache = new ConcurrentHashMap<>();

    public SQLStorage(PowerFly plugin) {
        this.plugin = plugin;
        initialize();
    }

    public void cachePlayerName(UUID uuid, String name) {
        nameCache.put(uuid, name);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = getConnection().prepareStatement(QUERY_UPDATE_NAME)) {
                ps.setString(1, name);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("cachePlayerName update failed for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void evictPlayerName(UUID uuid) {
        nameCache.remove(uuid);
    }

    private synchronized void initialize() {
        try {
            File databaseFile = new File(plugin.getDataFolder(), "database.db");
            connection = DriverManager.getConnection(String.format(DB_URL, databaseFile.getAbsolutePath()));

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA synchronous=NORMAL;");
            }

            plugin.getLogger().info("Connected to SQL database.");
            createTables();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to SQL: " + e.getMessage());
            connection = null;
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) return connection;

        plugin.getLogger().warning("SQL connection was closed — reconnecting...");
        initialize();

        if (connection == null || connection.isClosed()) {
            throw new SQLException("Failed to reconnect to the database.");
        }

        return connection;
    }

    private void createTables() {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(CREATE_TABLE);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    public int getFlyTime(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement(QUERY_GET_TIME)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("time") : 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("getFlyTime failed for " + uuid + ": " + e.getMessage());
        }
        return 0;
    }

    public void setFlyTime(UUID uuid, int time) {
        try (PreparedStatement ps = getConnection().prepareStatement(QUERY_SET_TIME)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, getPlayerName(uuid));
            ps.setInt(3, time);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("setFlyTime failed for " + uuid + ": " + e.getMessage());
        }
    }

    public void addFlyTime(UUID uuid, int seconds) {
        if (seconds == -1) {
            setFlyTime(uuid, -1);
            return;
        }
        try (PreparedStatement ps = getConnection().prepareStatement(QUERY_ADD_TIME)) {
            ps.setInt(1, seconds);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("addFlyTime failed for " + uuid + ": " + e.getMessage());
        }
    }

    public void delFlyTime(UUID uuid, int seconds) {
        try (PreparedStatement ps = getConnection().prepareStatement(QUERY_DEL_TIME)) {
            ps.setInt(1, seconds);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("delFlyTime failed for " + uuid + ": " + e.getMessage());
        }
    }

    public long getCooldown(UUID uuid) {
        try (PreparedStatement ps = getConnection().prepareStatement(QUERY_GET_COOLDOWN)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("cooldown") : 0L;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("getCooldown failed for " + uuid + ": " + e.getMessage());
        }
        return 0L;
    }

    public void setCooldown(UUID uuid, long cooldownUntil) {
        try (PreparedStatement ps = getConnection().prepareStatement(QUERY_SET_COOLDOWN)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, getPlayerName(uuid));
            ps.setLong(3, cooldownUntil);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("setCooldown failed for " + uuid + ": " + e.getMessage());
        }
    }

    public void removeCooldown(UUID uuid) {
        setCooldown(uuid, 0L);
    }

    public void createPlayerIfNotExists(UUID uuid, String name, int flyTime) {
        nameCache.put(uuid, name);
        try (PreparedStatement ps = getConnection().prepareStatement(QUERY_INSERT_IGNORE)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setInt(3, flyTime);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("createPlayerIfNotExists failed for " + uuid + ": " + e.getMessage());
        }
    }

    public void removePlayer(UUID uuid) {
        nameCache.remove(uuid);
        try (PreparedStatement ps = getConnection().prepareStatement(QUERY_DELETE)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("removePlayer failed for " + uuid + ": " + e.getMessage());
        }
    }

    public Map<UUID, Integer> loadAllFlyTimes() {
        Map<UUID, Integer> map = new HashMap<>();
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(QUERY_LOAD_ALL)) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    map.put(uuid, rs.getInt("time"));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in database, skipping: " + rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("loadAllFlyTimes failed: " + e.getMessage());
        }
        return map;
    }

    public Map<UUID, Long> loadAllCooldowns() {
        Map<UUID, Long> map = new HashMap<>();
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(QUERY_LOAD_ALL_COOLDOWNS)) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    map.put(uuid, rs.getLong("cooldown"));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in cooldowns, skipping.");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("loadAllCooldowns failed: " + e.getMessage());
        }
        return map;
    }

    public void close() {
        nameCache.clear();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("SQL connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close SQL connection: " + e.getMessage());
        }
    }

    private String getPlayerName(UUID uuid) {
        String cached = nameCache.get(uuid);
        if (cached != null) return cached;

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            nameCache.put(uuid, player.getName());
            return player.getName();
        }

        try (PreparedStatement ps = getConnection().prepareStatement(QUERY_GET_NAME)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    if (name != null) nameCache.put(uuid, name);
                    return name != null ? name : "Unknown";
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("getPlayerName failed for " + uuid + ": " + e.getMessage());
        }
        return "Unknown";
    }
}