package gg.vanezus.waypoint.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

public class PluginConfig {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final int territoryRadius;
    private final int minDistance;
    private final int maxWaypointsPerPlayer;
    private final int maxRadius;
    private final int defaultRadius;
    private final Map<String, Integer> perGroupLimits;
    private final Map<String, Integer> perGroupRadius;
    private final double hologramOffset;
    private final int iconSize;
    private final Component territoryEnterMessage;
    private final Component territoryLeaveMessage;
    private final Component waypointCreatedMessage;
    private final Component waypointRemovedMessage;
    private final Component invalidItemMessage;
    private final Component tooCloseMessage;
    private final Component maxWaypointsMessage;
    private final Component reloadSuccessMessage;

    public PluginConfig(FileConfiguration cfg) {
        territoryRadius      = cfg.getInt("territory.radius", 100);
        minDistance          = cfg.getInt("territory.min-distance", 100);

        maxWaypointsPerPlayer = cfg.getInt("waypoints.max-per-player", 3);
        maxRadius            = cfg.getInt("waypoints.max-radius", 50);
        defaultRadius        = cfg.getInt("waypoints.default-radius", 50);

        perGroupLimits = new LinkedHashMap<>();
        var section = cfg.getConfigurationSection("waypoints.per-group-limits");
        if (section != null) {
            for (String perm : section.getKeys(false)) {
                perGroupLimits.put(perm, section.getInt(perm));
            }
        }

        perGroupRadius = new LinkedHashMap<>();
        var radiusSection = cfg.getConfigurationSection("waypoints.per-group-radius");
        if (radiusSection != null) {
            for (String perm : radiusSection.getKeys(false)) {
                perGroupRadius.put(perm, radiusSection.getInt(perm));
            }
        }

        hologramOffset = cfg.getDouble("hologram.offset", 2.5);
        iconSize       = cfg.getInt("icon.size", 16);

        territoryEnterMessage  = parse(cfg, "messages.territory-enter",  "<gold>Вы вошли на территорию: <yellow>{name}");
        territoryLeaveMessage  = parse(cfg, "messages.territory-leave",  "<gray>Вы покинули территорию: <dark_gray>{name}");
        waypointCreatedMessage = parse(cfg, "messages.waypoint-created", "<green>Точка <yellow>{name} <green>создана!");
        waypointRemovedMessage = parse(cfg, "messages.waypoint-removed", "<red>Точка <yellow>{name} <red>удалена!");
        invalidItemMessage     = parse(cfg, "messages.invalid-item",     "<red>Используйте компас!");
        tooCloseMessage        = parse(cfg, "messages.too-close",        "<red>Слишком близко! Минимум: {distance} блоков");
        maxWaypointsMessage    = parse(cfg, "messages.max-waypoints",    "<red>Лимит точек! Максимум: {max}");
        reloadSuccessMessage   = parse(cfg, "messages.reload-success",   "<green>Конфиг перезагружен!");
    }

    private static Component parse(FileConfiguration cfg, String path, String fallback) {
        String raw = cfg.getString(path, fallback);
        return MM.deserialize(raw != null ? raw : fallback);
    }

    public int getTerritoryRadius()       { return territoryRadius; }
    public int getMinDistance()           { return minDistance; }
    public int getMaxWaypointsPerPlayer() { return maxWaypointsPerPlayer; }
    public int getMaxRadius()             { return maxRadius; }
    public int getDefaultRadius()         { return defaultRadius; }

    public int getMaxWaypointsForPlayer(Player player) {
        int max = maxWaypointsPerPlayer;
        for (var entry : perGroupLimits.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                max = Math.max(max, entry.getValue());
            }
        }
        if (player.hasPermission("waypoints.limit.5")) {
            max = Math.max(max, 5);
        }
        return max;
    }

    public int getMaxRadiusForPlayer(Player player) {
        int radius = maxRadius;
        for (var entry : perGroupRadius.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                radius = Math.max(radius, entry.getValue());
            }
        }
        if (player.hasPermission("waypoints.limit.5")) {
            radius = Math.max(radius, 75);
        }
        return radius;
    }

    public double getHologramOffset() { return hologramOffset; }
    public int getIconSize()          { return iconSize; }
    public Component getTerritoryEnterMessage()  { return territoryEnterMessage; }
    public Component getTerritoryLeaveMessage()  { return territoryLeaveMessage; }
    public Component getWaypointCreatedMessage() { return waypointCreatedMessage; }
    public Component getWaypointRemovedMessage() { return waypointRemovedMessage; }
    public Component getInvalidItemMessage()     { return invalidItemMessage; }
    public Component getTooCloseMessage()        { return tooCloseMessage; }
    public Component getMaxWaypointsMessage()    { return maxWaypointsMessage; }
    public Component getReloadSuccessMessage()   { return reloadSuccessMessage; }
}
