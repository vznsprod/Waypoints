package gg.vanezus.waypoint.listeners;

import gg.vanezus.waypoint.Waypoint;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

public class BannerInteractListener implements Listener {

    private static final String INPUT_NAME = "waypoint_name";
    private static final String INPUT_RADIUS = "waypoint_radius";
    private final Waypoint plugin;

    public BannerInteractListener(Waypoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBannerClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        var block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof Banner banner)) return;

        var player = event.getPlayer();
        var item = player.getInventory().getItemInMainHand();
        var location = block.getLocation();

        if (!isAllowedLocation(location)) {
            player.sendActionBar(Component.text(
                "В центре нельзя ставить точки: ставьте за пределами 500x500 (Обычный), 100x100 (Ад), 350x350 (Энд)",
                NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        if (plugin.getWaypointManager().hasWaypoint(location)) {
            var waypoint = plugin.getWaypointManager().getWaypoint(location);
            if (waypoint != null && waypoint.getOwnerUuid().equals(player.getUniqueId())) {
                showEditDialog(player, location, waypoint.getName(), waypoint.getRadius());
                event.setCancelled(true);
            }
            return;
        }

        if (item.getType() != Material.COMPASS) return;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) return;

        int minDist = plugin.getPluginConfig().getMinDistance();
        if (plugin.getWaypointManager().findNearestWaypoint(location, minDist) != null) {
            player.sendActionBar(plugin.getPluginConfig().getTooCloseMessage()
                    .replaceText(c -> c.matchLiteral("{distance}").replacement(String.valueOf(minDist))));
            event.setCancelled(true);
            return;
        }

        int max = plugin.getPluginConfig().getMaxWaypointsForPlayer(player);
        int count = plugin.getDatabaseManager().getWaypointCountByPlayer(player.getUniqueId());
        if (count >= max) {
            player.sendActionBar(plugin.getPluginConfig().getMaxWaypointsMessage()
                    .replaceText(c -> c.matchLiteral("{max}").replacement(String.valueOf(max))));
            event.setCancelled(true);
            return;
        }

        showCreateDialog(player, location, banner.getBaseColor());
        event.setCancelled(true);
    }

    private void showCreateDialog(Player player, Location location, DyeColor bannerColor) {
        int maxRadius = plugin.getPluginConfig().getMaxRadiusForPlayer(player);
        int defaultRadius = Math.min(plugin.getPluginConfig().getDefaultRadius(), maxRadius);

        var inputs = List.of(
                DialogInput.text(INPUT_NAME, Component.text("Название точки"))
                        .maxLength(10)
                        .build(),
                DialogInput.numberRange(INPUT_RADIUS, Component.text("Радиус территории"), 1f, (float) maxRadius)
                        .initial((float) defaultRadius)
                        .step(1f)
                        .build()
        );

        var createAction = DialogAction.customClick(
                (view, audience) -> {
                    if (audience instanceof Player p) {
                        float radius = view.getFloat(INPUT_RADIUS);
                        handleCreation(p, location, bannerColor, view.getText(INPUT_NAME), (int) radius);
                    }
                },
                ClickCallback.Options.builder().uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build()
        );

        var dialog = Dialog.create(builder ->
                builder.empty()
                        .base(DialogBase.builder(Component.text("Создать точку", NamedTextColor.GOLD))
                                .inputs(inputs).build())
                        .type(DialogType.confirmation(
                                ActionButton.create(
                                        Component.text("Создать", TextColor.color(0xAEFFC1)),
                                        Component.text("Нажмите для создания"),
                                        100, createAction),
                                ActionButton.create(
                                        Component.text("Отмена", TextColor.color(0xFFA0B1)),
                                        Component.text("Нажмите для отмены"),
                                        100, null)
                        ))
        );

        player.showDialog(dialog);
    }

    private void showEditDialog(Player player, Location location, String currentName, int currentRadius) {
        int maxRadius = plugin.getPluginConfig().getMaxRadiusForPlayer(player);
        int initialRadius = Math.min(currentRadius, maxRadius);

        var inputs = List.of(
                DialogInput.text(INPUT_NAME, Component.text("Название точки"))
                        .initial(currentName)
                        .maxLength(10)
                        .build(),
                DialogInput.numberRange(INPUT_RADIUS, Component.text("Радиус территории"), 1f, (float) maxRadius)
                    .initial((float) initialRadius)
                        .step(1f)
                        .build()
        );

        var saveAction = DialogAction.customClick(
                (view, audience) -> {
                    if (audience instanceof Player p) {
                        float radius = view.getFloat(INPUT_RADIUS);
                        handleEdit(p, location, view.getText(INPUT_NAME), (int) radius);
                    }
                },
                ClickCallback.Options.builder().uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build()
        );

        var dialog = Dialog.create(builder ->
                builder.empty()
                        .base(DialogBase.builder(Component.text("Настройка точки", NamedTextColor.GOLD))
                                .inputs(inputs).build())
                        .type(DialogType.confirmation(
                                ActionButton.create(
                                        Component.text("Сохранить", TextColor.color(0xAEFFC1)),
                                        Component.text("Нажмите для сохранения"),
                                        100, saveAction),
                                ActionButton.create(
                                        Component.text("Отмена", TextColor.color(0xFFA0B1)),
                                        Component.text("Нажмите для отмены"),
                                        100, null)
                        ))
        );

        player.showDialog(dialog);
    }

    private void handleCreation(Player player, Location location, DyeColor bannerColor, String name, int radius) {
        if (name == null || name.isBlank()) {
            player.sendActionBar(Component.text("Название не может быть пустым!", NamedTextColor.RED));
            return;
        }
        if (name.length() > 10) return;

        int maxRadius = plugin.getPluginConfig().getMaxRadiusForPlayer(player);
        radius = Math.min(radius, maxRadius);

        var compass = player.getInventory().getItemInMainHand();
        if (compass.getType() != Material.COMPASS) return;
        compass.subtract(1);

        plugin.getWaypointManager().createWaypoint(
                Component.text(name), location, bannerColor, player, radius);
    }

    private void handleEdit(Player player, Location location, String name, int radius) {
        if (name == null || name.isBlank()) {
            player.sendActionBar(Component.text("Название не может быть пустым!", NamedTextColor.RED));
            return;
        }
        if (name.length() > 10) return;

        int maxRadius = plugin.getPluginConfig().getMaxRadiusForPlayer(player);
        radius = Math.min(radius, maxRadius);

        plugin.getWaypointManager().editWaypoint(location, name, radius);
        player.sendMessage(Component.text("Точка обновлена!", NamedTextColor.GREEN));
    }

    private boolean isAllowedLocation(Location location) {
        double x = Math.abs(location.getX());
        double z = Math.abs(location.getZ());
        return switch (location.getWorld().getEnvironment()) {
            case NORMAL -> x > 250 || z > 250;  // forbid inside 500x500
            case NETHER -> x > 50 || z > 50;    // forbid inside 100x100
            case THE_END -> x > 175 || z > 175; // forbid inside 350x350
            default -> true;
        };
    }

}
