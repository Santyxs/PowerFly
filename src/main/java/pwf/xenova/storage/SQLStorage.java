package pwf.xenova.storage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pwf.xenova.PowerFly;
import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SQLStorage implements StorageInterface {

    private final PowerFly plugin;
    private Connection connection;
    private final File databaseFile;

    public SQLStorage(PowerFly plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "database.db");
        initialize();
    }

    private void initialize() {
        try {
            if (!databaseFile.exists()) {
                if (!databaseFile.createNewFile()) {
                    plugin.getLogger().severe("Could not create SQL database file");
                    return;
                }
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            plugin.getLogger().info("Connected to SQL database.");
            createTables();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to SQL: " + e.getMessage());
        }
    }

    private void createTables() {
        String createFlyTimeTable = "CREATE TABLE IF NOT EXISTS powerfly_players (" +
                "uuid TEXT PRIMARY KEY," +
                "name TEXT," +
                "time INTEGER DEFAULT 0," +
                "cooldown INTEGER DEFAULT 0" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createFlyTimeTable);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    @Override
    public int getFlyTime(UUID uuid) {
        String query = "SELECT time FROM powerfly_players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("time");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting fly time: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void setFlyTime(UUID uuid, int time) {
        String query = "INSERT OR REPLACE INTO powerfly_players (uuid, name, time, cooldown) " +
                "VALUES (?, ?, ?, (SELECT cooldown FROM powerfly_players WHERE uuid = ? LIMIT 1))";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            String playerName = getPlayerName(uuid);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setInt(3, time);
            stmt.setString(4, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error setting fly time: " + e.getMessage());
        }
    }

    public void addFlyTime(UUID uuid, int seconds) {
        if (seconds == -1) {
            setFlyTime(uuid, -1);
            return;
        }
        int current = getFlyTime(uuid);
        if (current == -1) return;
        setFlyTime(uuid, current + seconds);
    }

    public void delFlyTime(UUID uuid, int seconds) {
        int current = getFlyTime(uuid);
        if (current == -1) return;
        setFlyTime(uuid, Math.max(0, current - seconds));
    }

    public long getCooldown(UUID uuid) {
        String query = "SELECT cooldown FROM powerfly_players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("cooldown");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting cooldown: " + e.getMessage());
        }
        return 0;
    }

    public void setCooldown(UUID uuid, long cooldownUntil) {
        String query = "INSERT OR REPLACE INTO powerfly_players (uuid, name, time, cooldown) " +
                "VALUES (?, ?, (SELECT time FROM powerfly_players WHERE uuid = ? LIMIT 1), ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            String playerName = getPlayerName(uuid);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, uuid.toString());
            stmt.setLong(4, cooldownUntil);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error setting cooldown: " + e.getMessage());
        }
    }

    public void removeCooldown(UUID uuid) {
        setCooldown(uuid, 0);
    }

    @Override
    public void createPlayerIfNotExists(UUID uuid, String name, int flyTime) {
        String query = "INSERT OR IGNORE INTO powerfly_players (uuid, name, time, cooldown) VALUES (?, ?, ?, 0)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, name);
            stmt.setInt(3, flyTime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating player: " + e.getMessage());
        }
    }

    @Override
    public void removePlayer(UUID uuid) {
        String query = "DELETE FROM powerfly_players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error removing player: " + e.getMessage());
        }
    }

    @Override
    public Map<UUID, Integer> loadAllFlyTimes() {
        Map<UUID, Integer> map = new HashMap<>();
        String query = "SELECT uuid, time FROM powerfly_players";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                int time = rs.getInt("time");
                map.put(uuid, time);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading fly times: " + e.getMessage());
        }
        return map;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("SQL connection closed.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close connection: " + e.getMessage());
        }
    }

    private String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        return "Unknown";
    }
}