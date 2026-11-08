package gg.vanezus.waypoint.managers;

import gg.vanezus.waypoint.Waypoint;
import gg.vanezus.waypoint.database.models.WaypointModel;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TerritoryManager {

    private final Waypoint plugin;
    private final ConcurrentHashMap<UUID, String> playerTerritories = new ConcurrentHashMap<>();

    public TerritoryManager(Waypoint plugin) { this.plugin = plugin; }

    public void checkTerritory(Player player, Location location) {
        String current = findNearest(location);
        String previous = playerTerritories.get(player.getUniqueId());

        boolean changed = current == null ? previous != null : !current.equals(previous);
        if (!changed) return;

        if (previous != null) {
            player.sendActionBar(plugin.getPluginConfig().getTerritoryLeaveMessage()
                    .replaceText(c -> c.matchLiteral("{name}").replacement(previous)));
        }

        if (current != null) {
            player.sendActionBar(plugin.getPluginConfig().getTerritoryEnterMessage()
                    .replaceText(c -> c.matchLiteral("{name}").replacement(current)));
            playerTerritories.put(player.getUniqueId(), current);
        } else {
            playerTerritories.remove(player.getUniqueId());
        }
    }

    private String findNearest(Location location) {
        return plugin.getWaypointManager().getAllWaypoints().stream()
                .filter(w -> w.getLocation().getWorld().equals(location.getWorld()))
                .filter(w -> w.getLocation().distance(location) <= w.getRadius())
                .min((a, b) -> Double.compare(
                        a.getLocation().distance(location),
                        b.getLocation().distance(location)
                ))
                .map(WaypointModel::getName)
                .orElse(null);
    }

    public void removePlayer(UUID uuid) { playerTerritories.remove(uuid); }
    public void shutdown() { playerTerritories.clear(); }
}
