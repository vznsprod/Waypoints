package gg.vanezus.waypoint.database.models;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import java.util.UUID;

public final class WaypointModel {
    private final String name;
    private final Location location;
    private final DyeColor bannerColor;
    private final UUID ownerUuid;
    private final int radius;

    public WaypointModel(String name, Location location, DyeColor bannerColor, UUID ownerUuid, int radius) {
        this.name = name;
        this.location = location;
        this.bannerColor = bannerColor;
        this.ownerUuid = ownerUuid;
        this.radius = radius;
    }

    public String getName() { return name; }
    public Location getLocation() { return location; }
    public DyeColor getBannerColor() { return bannerColor; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public int getRadius() { return radius; }
}
