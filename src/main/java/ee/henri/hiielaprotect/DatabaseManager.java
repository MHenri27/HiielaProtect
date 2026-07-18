package ee.henri.hiielaprotect;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final HiielaProtect plugin;
    private Connection connection;

    public DatabaseManager(HiielaProtect plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }

            File databaseFile = new File(dataFolder, "database.db");
            String url = "jdbc:sqlite:" + databaseFile.getPath();

            connection = DriverManager.getConnection(url);

            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS regions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "player_uuid VARCHAR(36)," +
                        "player_name VARCHAR(16)," +
                        "region_name VARCHAR(255))");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to database!");
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getNextRegionNumber(String playerName) {
        int max = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT region_name FROM regions WHERE player_name = ? COLLATE NOCASE")) {
            statement.setString(1, playerName);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String regionName = rs.getString("region_name");
                    if (regionName.toLowerCase().startsWith(playerName.toLowerCase() + "_")) {
                        try {
                            int num = Integer.parseInt(regionName.substring(playerName.length() + 1));
                            if (num > max) {
                                max = num;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return max + 1;
    }

    public void saveRegion(String playerUuid, String playerName, String regionName) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO regions (player_uuid, player_name, region_name) VALUES (?, ?, ?)")) {
            statement.setString(1, playerUuid);
            statement.setString(2, playerName);
            statement.setString(3, regionName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public String getLatestRegion(String playerName) {
        String latest = null;
        int max = 0;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT region_name FROM regions WHERE player_name = ? COLLATE NOCASE")) {
            statement.setString(1, playerName);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String regionName = rs.getString("region_name");
                    if (regionName.toLowerCase().startsWith(playerName.toLowerCase() + "_")) {
                        try {
                            int num = Integer.parseInt(regionName.substring(playerName.length() + 1));
                            if (num > max) {
                                max = num;
                                latest = regionName;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return latest;
    }

    public void deleteRegion(String regionName) {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM regions WHERE region_name = ? COLLATE NOCASE")) {
            statement.setString(1, regionName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean hasRegion(String regionName) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM regions WHERE region_name = ? COLLATE NOCASE")) {
            statement.setString(1, regionName);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public String getPlayerNameByRegion(String regionName) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT player_name FROM regions WHERE region_name = ? COLLATE NOCASE")) {
            statement.setString(1, regionName);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("player_name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
