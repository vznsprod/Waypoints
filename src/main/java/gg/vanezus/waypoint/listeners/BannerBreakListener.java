package gg.vanezus.waypoint.listeners;

import gg.vanezus.waypoint.Waypoint;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

public class BannerBreakListener implements Listener {

    private final Waypoint plugin;

    public BannerBreakListener(Waypoint plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBannerBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getBlock().getState() instanceof Banner)) return;
        var location = event.getBlock().getLocation();
        if (plugin.getWaypointManager().hasWaypoint(location))
            plugin.getWaypointManager().removeWaypoint(location, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSupportBlockBreak(BlockBreakEvent event) {
        Block above = event.getBlock().getRelative(BlockFace.UP);
        if (!(above.getState() instanceof Banner)) return;
        var location = above.getLocation();
        if (plugin.getWaypointManager().hasWaypoint(location))
            plugin.getWaypointManager().removeWaypoint(location, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (affectsWaypoint(event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (affectsWaypoint(event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> isWaypointBlock(block));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isWaypointBlock(block));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (!(event.getBlock().getState() instanceof Banner)) return;
        var location = event.getBlock().getLocation();
        if (plugin.getWaypointManager().hasWaypoint(location)) {
            event.setCancelled(true);
        }
    }

    private boolean affectsWaypoint(List<Block> blocks, BlockFace direction) {
        for (Block block : blocks) {
            if (block.getState() instanceof Banner
                    && plugin.getWaypointManager().hasWaypoint(block.getLocation())) {
                return true;
            }
            Block above = block.getRelative(BlockFace.UP);
            if (above.getState() instanceof Banner
                    && plugin.getWaypointManager().hasWaypoint(above.getLocation())) {
                return true;
            }
        }
        return false;
    }

    private boolean isWaypointBlock(Block block) {
        if (block.getState() instanceof Banner
                && plugin.getWaypointManager().hasWaypoint(block.getLocation())) {
            return true;
        }
        Block above = block.getRelative(BlockFace.UP);
        if (above.getState() instanceof Banner
                && plugin.getWaypointManager().hasWaypoint(above.getLocation())) {
            return true;
        }
        return false;
    }
}
