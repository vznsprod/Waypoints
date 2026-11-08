package gg.vanezus.waypoint.managers;

import gg.vanezus.waypoint.Waypoint;
import gg.vanezus.waypoint.database.models.WaypointModel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WaypointManager {
    private final Waypoint plugin;
    private final ConcurrentHashMap<Location, WaypointModel> waypoints = new ConcurrentHashMap<>();
    private final Key layerKey = Key.of("waypoints");

    public WaypointManager(Waypoint plugin) { this.plugin = plugin; }

    public void loadAllWaypoints() {
        waypoints.clear();
        var loaded = plugin.getDatabaseManager().getAllWaypoints();
        loaded.forEach(w -> {
            waypoints.put(w.getLocation(), w);
            addToMap(w);
            plugin.getWaypointHologramManager().createHologram(w);
        });
        plugin.getLogger().info("Loaded " + loaded.size() + " waypoints");
    }

    public void createWaypoint(Component name, Location location, DyeColor bannerColor, Player player, int radius) {
        String plain = PlainTextComponentSerializer.plainText().serialize(name);
        var waypoint = new WaypointModel(plain, location, bannerColor, player.getUniqueId(), radius);
        waypoints.put(location, waypoint);
        plugin.getDatabaseManager().saveWaypoint(waypoint);
        addToMap(waypoint);
        plugin.getWaypointHologramManager().createHologram(waypoint);
        spawnCreationParticles(location, bannerColor);
        player.sendMessage(plugin.getPluginConfig().getWaypointCreatedMessage()
                .replaceText(c -> c.matchLiteral("{name}").replacement(plain)));
    }

    public void editWaypoint(Location location, String newName, int newRadius) {
        var old = waypoints.get(location);
        if (old == null) return;
        var updated = new WaypointModel(newName, location, old.getBannerColor(), old.getOwnerUuid(), newRadius);
        waypoints.put(location, updated);
        plugin.getDatabaseManager().updateWaypoint(updated);
        removeFromMap(old);
        addToMap(updated);
        plugin.getWaypointHologramManager().createHologram(updated);
    }

    public WaypointModel getWaypoint(Location location) { return waypoints.get(location); }

    public void removeWaypoint(Location location, Player player) {
        var waypoint = waypoints.remove(location);
        if (waypoint == null) return;
        plugin.getDatabaseManager().deleteWaypoint(location);
        removeFromMap(waypoint);
        plugin.getWaypointHologramManager().removeHologram(location);
        spawnRemovalParticles(location, waypoint.getBannerColor());
        if (player != null) player.sendMessage(plugin.getPluginConfig().getWaypointRemovedMessage()
                .replaceText(c -> c.matchLiteral("{name}").replacement(waypoint.getName())));
    }

    public void removeWaypoint(Location location) { removeWaypoint(location, null); }
    public boolean hasWaypoint(Location location) { return waypoints.containsKey(location); }
    public Collection<WaypointModel> getAllWaypoints() { return waypoints.values(); }

    public WaypointModel findNearestWaypoint(Location location, int radius) {
        return waypoints.values().stream()
                .filter(w -> w.getLocation().getWorld().equals(location.getWorld()))
                .filter(w -> w.getLocation().distance(location) <= radius)
                .min((a, b) -> Double.compare(
                        a.getLocation().distance(location),
                        b.getLocation().distance(location)))
                .orElse(null);
    }

    private void addToMap(WaypointModel w) {
        var mapWorld = plugin.getSquaremapApi().getWorldIfEnabled(
                WorldIdentifier.parse(w.getLocation().getWorld().getKey().asString())).orElse(null);
        if (mapWorld == null) return;
        var provider = mapWorld.layerRegistry().get(layerKey);
        if (!(provider instanceof SimpleLayerProvider sp)) return;
        var center = Point.of(w.getLocation().getX(), w.getLocation().getZ());
        sp.addMarker(markerKey(w.getLocation()),
                Marker.icon(
                center,
                        Icons.getIcon(w.getBannerColor()),
                        plugin.getPluginConfig().getIconSize()
                ).markerOptions(MarkerOptions.builder().hoverTooltip(w.getName()).build()));

        sp.addMarker(radiusKey(w.getLocation()),
            Marker.circle(center, w.getRadius())
                .markerOptions(MarkerOptions.builder()
                    .strokeColor(getAwtColor(w.getBannerColor()))
                    .strokeOpacity(0.8d)
                    .strokeWeight(2)
                    .fillColor(getAwtColor(w.getBannerColor()))
                    .fillOpacity(0.25d)
                    .build()));
    }

    private void removeFromMap(WaypointModel w) {
        var mapWorld = plugin.getSquaremapApi().getWorldIfEnabled(
                WorldIdentifier.parse(w.getLocation().getWorld().getKey().asString())).orElse(null);
        if (mapWorld == null) return;
        var p = mapWorld.layerRegistry().get(layerKey);
        if (p instanceof SimpleLayerProvider sp) {
            sp.removeMarker(markerKey(w.getLocation()));
            sp.removeMarker(radiusKey(w.getLocation()));
        }
    }

    private Key markerKey(Location loc) {
        return Key.of("wp_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ());
    }

    private Key radiusKey(Location loc) {
        return Key.of("wp_radius_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ());
        }

    private void spawnCreationParticles(Location location, DyeColor bannerColor) {
        var world = location.getWorld();
        var center = location.clone().add(0.5, 1.0, 0.5);
        var tick = new AtomicInteger(0);
        var color = getColor(bannerColor);
        Runnable[] ref = new Runnable[1];
        ref[0] = () -> {
            int t = tick.getAndIncrement();
            if (t >= 20) return;
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i / 8) + (t * 0.5);
                world.spawnParticle(Particle.DUST,
                        center.getX() + Math.cos(angle),
                        center.getY() + (t * 0.08),
                        center.getZ() + Math.sin(angle),
                        2, 0.1, 0.1, 0.1, 0.0,
                        new Particle.DustOptions(color, 1.5f));
            }
            if (t % 10 == 0) world.spawnParticle(Particle.FIREWORK,
                    center.getX(), center.getY() + 1.0, center.getZ(),
                    5, 0.3, 0.3, 0.3, 0.05);
            plugin.getServer().getRegionScheduler()
                    .runDelayed(plugin, location, tt -> ref[0].run(), 2L);
        };
        plugin.getServer().getRegionScheduler().run(plugin, location, t -> ref[0].run());
    }

    private void spawnRemovalParticles(Location location, DyeColor bannerColor) {
        var world = location.getWorld();
        var center = location.clone().add(0.5, 1.0, 0.5);
        var tick = new AtomicInteger(0);
        var color = getColor(bannerColor);
        Runnable[] ref = new Runnable[1];
        ref[0] = () -> {
            int t = tick.getAndIncrement();
            if (t >= 15) return;
            double radius = 1.0 - (t * 0.06);
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i / 8) - (t * 0.5);
                world.spawnParticle(Particle.DUST,
                        center.getX() + radius * Math.cos(angle),
                        center.getY() - (t * 0.05),
                        center.getZ() + radius * Math.sin(angle),
                        1, 0.0, 0.0, 0.0, 0.0,
                        new Particle.DustOptions(color, 1.0f));
            }
            if (t % 5 == 0) world.spawnParticle(Particle.CLOUD,
                    center.getX(), center.getY(), center.getZ(),
                    3, 0.3, 0.3, 0.3, 0.02);
            plugin.getServer().getRegionScheduler()
                    .runDelayed(plugin, location, tt -> ref[0].run(), 2L);
        };
        plugin.getServer().getRegionScheduler().run(plugin, location, t -> ref[0].run());
    }

    private Color getColor(DyeColor d) {
        return switch (d) {
            case WHITE -> Color.fromRGB(255,255,255); case ORANGE -> Color.fromRGB(255,165,0);
            case MAGENTA -> Color.fromRGB(255,0,255); case LIGHT_BLUE -> Color.fromRGB(135,206,235);
            case YELLOW -> Color.fromRGB(255,255,0); case LIME -> Color.fromRGB(0,255,0);
            case PINK -> Color.fromRGB(255,192,203); case GRAY -> Color.fromRGB(128,128,128);
            case LIGHT_GRAY -> Color.fromRGB(211,211,211); case CYAN -> Color.fromRGB(0,255,255);
            case PURPLE -> Color.fromRGB(128,0,128); case BLUE -> Color.fromRGB(0,0,255);
            case BROWN -> Color.fromRGB(139,69,19); case GREEN -> Color.fromRGB(0,128,0);
            case RED -> Color.fromRGB(255,0,0); case BLACK -> Color.fromRGB(0,0,0);
        };
    }

    private java.awt.Color getAwtColor(DyeColor d) {
        var c = getColor(d);
        return new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue());
    }
}
