package gg.vanezus.waypoint.managers;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import gg.vanezus.waypoint.Waypoint;
import gg.vanezus.waypoint.database.models.WaypointModel;
import org.bukkit.Location;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class WaypointHologramManager {

    private final Waypoint plugin;
    private final ConcurrentHashMap<Location, String> holograms = new ConcurrentHashMap<>();

    public WaypointHologramManager(Waypoint plugin) {
        this.plugin = plugin;
    }

    public void createHologram(WaypointModel waypoint) {
        Location base = waypoint.getLocation();
        removeHologram(base);

        String id = buildId(base);
        Location spawnLoc = base.clone().add(0.5, plugin.getPluginConfig().getHologramOffset(), 0.5);

        Hologram existing = DHAPI.getHologram(id);
        if (existing != null) DHAPI.removeHologram(id);

        DHAPI.createHologram(id, spawnLoc, false, List.of(
                "&f" + waypoint.getName()
        ));

        holograms.put(base, id);
    }

    public void removeHologram(Location location) {
        String id = holograms.remove(location);
        if (id == null) return;
        if (DHAPI.getHologram(id) != null) {
            DHAPI.removeHologram(id);
        }
    }

    public void removeAllHolograms() {
        holograms.forEach((loc, id) -> {
            if (DHAPI.getHologram(id) != null) DHAPI.removeHologram(id);
        });
        holograms.clear();
    }

    private String buildId(Location loc) {
        return "wp_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }
}
