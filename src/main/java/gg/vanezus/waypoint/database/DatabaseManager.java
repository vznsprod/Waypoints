package gg.vanezus.waypoint.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.vanezus.waypoint.Waypoint;
import gg.vanezus.waypoint.database.models.WaypointModel;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final Waypoint plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Waypoint plugin) { this.plugin = plugin; }

    public void initialize() throws SQLException {
        var dbFile = plugin.getDataFolder().toPath().resolve("waypoints.db").toFile();
        plugin.getDataFolder().mkdirs();
        var cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        cfg.setMaximumPoolSize(1);
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(30000);
        cfg.setIdleTimeout(600000);
        cfg.setMaxLifetime(1800000);
        cfg.addDataSourceProperty("journal_mode", "WAL");
        cfg.addDataSourceProperty("synchronous", "NORMAL");
        cfg.addDataSourceProperty("cache_size", "-8000");
        cfg.addDataSourceProperty("foreign_keys", "true");
        cfg.setPoolName("WaypointPool");
        dataSource = new HikariDataSource(cfg);
        createTables();
        migrateDatabase();
    }

    private void createTables() throws SQLException {
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS waypoints (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "world TEXT NOT NULL," +
                    "x DOUBLE NOT NULL," +
                    "y DOUBLE NOT NULL," +
                    "z DOUBLE NOT NULL," +
                    "banner_color TEXT NOT NULL," +
                    "owner_uuid TEXT NOT NULL," +
                    "radius INTEGER NOT NULL DEFAULT 100," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_world ON waypoints(world)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_owner ON waypoints(owner_uuid)");
        }
    }

    private void migrateDatabase() {
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE waypoints ADD COLUMN owner_uuid TEXT NOT NULL DEFAULT ''");
        } catch (Exception ignored) {}
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE waypoints ADD COLUMN radius INTEGER NOT NULL DEFAULT 100");
        } catch (Exception ignored) {}
    }

    public void saveWaypoint(WaypointModel w) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(
                     "INSERT OR REPLACE INTO waypoints (name,world,x,y,z,banner_color,owner_uuid,radius) VALUES (?,?,?,?,?,?,?,?)")) {
            stmt.setString(1, w.getName());
            stmt.setString(2, w.getLocation().getWorld().getName());
            stmt.setDouble(3, w.getLocation().getX());
            stmt.setDouble(4, w.getLocation().getY());
            stmt.setDouble(5, w.getLocation().getZ());
            stmt.setString(6, w.getBannerColor().name());
            stmt.setString(7, w.getOwnerUuid().toString());
            stmt.setInt(8, w.getRadius());
            stmt.executeUpdate();
        } catch (Exception e) { plugin.getLogger().severe("Failed to save waypoint: " + e.getMessage()); }
    }

    public void updateWaypoint(WaypointModel w) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(
                     "UPDATE waypoints SET name=?, radius=? WHERE world=? AND x=? AND y=? AND z=?")) {
            stmt.setString(1, w.getName());
            stmt.setInt(2, w.getRadius());
            stmt.setString(3, w.getLocation().getWorld().getName());
            stmt.setDouble(4, w.getLocation().getX());
            stmt.setDouble(5, w.getLocation().getY());
            stmt.setDouble(6, w.getLocation().getZ());
            stmt.executeUpdate();
        } catch (Exception e) { plugin.getLogger().severe("Failed to update waypoint: " + e.getMessage()); }
    }

    public void deleteWaypoint(Location location) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM waypoints WHERE world=? AND x=? AND y=? AND z=?")) {
            stmt.setString(1, location.getWorld().getName());
            stmt.setDouble(2, location.getX());
            stmt.setDouble(3, location.getY());
            stmt.setDouble(4, location.getZ());
            stmt.executeUpdate();
        } catch (Exception e) { plugin.getLogger().severe("Failed to delete waypoint: " + e.getMessage()); }
    }

    public List<WaypointModel> getAllWaypoints() {
        var list = new ArrayList<WaypointModel>();
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT * FROM waypoints")) {
            while (rs.next()) {
                var world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) continue;
                var loc = new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
                DyeColor color;
                try { color = DyeColor.valueOf(rs.getString("banner_color")); } catch (Exception e) { color = DyeColor.WHITE; }
                UUID uuid;
                try { uuid = UUID.fromString(rs.getString("owner_uuid")); } catch (Exception e) { uuid = UUID.randomUUID(); }
                int radius = rs.getInt("radius");
                list.add(new WaypointModel(rs.getString("name"), loc, color, uuid, radius));
            }
        } catch (Exception e) { plugin.getLogger().severe("Failed to load waypoints: " + e.getMessage()); }
        return list;
    }

    public int getWaypointCountByPlayer(UUID uuid) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement("SELECT COUNT(*) FROM waypoints WHERE owner_uuid=?")) {
            stmt.setString(1, uuid.toString());
            var rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) { plugin.getLogger().severe("Failed to count: " + e.getMessage()); return 0; }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
