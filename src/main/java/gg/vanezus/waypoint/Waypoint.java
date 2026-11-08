package gg.vanezus.waypoint;

import gg.vanezus.waypoint.commands.ReloadCommand;
import gg.vanezus.waypoint.config.PluginConfig;
import gg.vanezus.waypoint.database.DatabaseManager;
import gg.vanezus.waypoint.listeners.BannerBreakListener;
import gg.vanezus.waypoint.listeners.BannerInteractListener;
import gg.vanezus.waypoint.listeners.PlayerMoveListener;
import gg.vanezus.waypoint.managers.Icons;
import gg.vanezus.waypoint.managers.TerritoryManager;
import gg.vanezus.waypoint.managers.WaypointHologramManager;
import gg.vanezus.waypoint.managers.WaypointManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

import java.util.List;
import java.util.Optional;

public final class Waypoint extends JavaPlugin implements Listener {

    private PluginConfig pluginConfig;
    private DatabaseManager databaseManager;
    private WaypointManager waypointManager;
    private WaypointHologramManager waypointHologramManager;
    private TerritoryManager territoryManager;
    private Squaremap squaremapApi;
    private volatile boolean waypointsLoaded = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            squaremapApi = SquaremapProvider.get();


            pluginConfig = new PluginConfig(getConfig());
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            initializeSquaremapLayers();
            Icons.initialize(this);
            waypointHologramManager = new WaypointHologramManager(this);
            waypointManager = new WaypointManager(this);
            territoryManager = new TerritoryManager(this);
            registerListeners();
            registerCommands();

            getServer().getGlobalRegionScheduler().runDelayed(this, task -> {
                if (!waypointsLoaded) {
                    waypointManager.loadAllWaypoints();
                    waypointsLoaded = true;
                }
            }, 40L);

            getLogger().info("enabled");
        } catch (Exception e) {
            getLogger().severe("err: " + e.getMessage());
            e.printStackTrace();
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        if (waypointHologramManager != null) waypointHologramManager.removeAllHolograms();
        if (territoryManager != null) territoryManager.shutdown();
        if (databaseManager != null) databaseManager.close();

        try {
            squaremapApi.mapWorlds().forEach(mapWorld -> {
                try { mapWorld.layerRegistry().unregister(Key.of("waypoints")); } catch (Exception ignored) {}
            });
            Icons.unregisterAll();
        } catch (Exception e) {}

        getLogger().info("disabled");
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        var world = event.getWorld();
        try {
            squaremapApi.getWorldIfEnabled(
                    WorldIdentifier.parse(world.getKey().asString())
            ).ifPresent(mapWorld -> {
                if (!mapWorld.layerRegistry().hasEntry(Key.of("waypoints"))) {
                    var provider = SimpleLayerProvider.builder("Waypoints")
                            .showControls(true).defaultHidden(false).build();
                    mapWorld.layerRegistry().register(Key.of("waypoints"), provider);
                }
            });
        } catch (Exception ignored) {}

        if (!waypointsLoaded) {
            getServer().getGlobalRegionScheduler().runDelayed(this, task -> {
                if (!waypointsLoaded) {
                    waypointManager.loadAllWaypoints();
                    waypointsLoaded = true;
                }
            }, 20L);
        }
    }

    private void initializeSquaremapLayers() {
        squaremapApi.mapWorlds().forEach(mapWorld -> {
            var provider = SimpleLayerProvider.builder("Waypoints")
                    .showControls(true).defaultHidden(false).build();
            mapWorld.layerRegistry().register(Key.of("waypoints"), provider);
        });
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        pm.registerEvents(new BannerInteractListener(this), this);
        pm.registerEvents(new BannerBreakListener(this), this);
        pm.registerEvents(new PlayerMoveListener(this), this);
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var commands = event.registrar();
            commands.register(getPluginMeta(), "waypointsreload",
                    "reload", List.of("wpreload"), new ReloadCommand(this));
        });
    }

    public void reloadPluginConfig() {
        reloadConfig();
        pluginConfig = new PluginConfig(getConfig());
    }

    public void resetWaypointsLoaded() { this.waypointsLoaded = false; }
    public PluginConfig getPluginConfig() { return pluginConfig; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public WaypointManager getWaypointManager() { return waypointManager; }
    public WaypointHologramManager getWaypointHologramManager() { return waypointHologramManager; }
    public TerritoryManager getTerritoryManager() { return territoryManager; }
    public Squaremap getSquaremapApi() { return squaremapApi; }
}
