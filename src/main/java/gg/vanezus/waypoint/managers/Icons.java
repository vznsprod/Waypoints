package gg.vanezus.waypoint.managers;

import gg.vanezus.waypoint.Waypoint;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.List;
import java.util.logging.Level;

public final class Icons {
    public static Key BLACK,BLUE,BROWN,CYAN,GRAY,GREEN,LIGHT_BLUE,LIGHT_GRAY,LIME,MAGENTA,ORANGE,PINK,PURPLE,RED,WHITE,YELLOW;
    private Icons() {}

    public static void initialize(Waypoint plugin) {
        BLACK=register(plugin,"BLACK"); BLUE=register(plugin,"BLUE"); BROWN=register(plugin,"BROWN");
        CYAN=register(plugin,"CYAN"); GRAY=register(plugin,"GRAY"); GREEN=register(plugin,"GREEN");
        LIGHT_BLUE=register(plugin,"LIGHT_BLUE"); LIGHT_GRAY=register(plugin,"LIGHT_GRAY");
        LIME=register(plugin,"LIME"); MAGENTA=register(plugin,"MAGENTA"); ORANGE=register(plugin,"ORANGE");
        PINK=register(plugin,"PINK"); PURPLE=register(plugin,"PURPLE"); RED=register(plugin,"RED");
        WHITE=register(plugin,"WHITE"); YELLOW=register(plugin,"YELLOW");
    }

    private static Key register(Waypoint plugin, String name) {
        String filename = "icons" + File.separator + name + ".png";
        File file = new File(plugin.getDataFolder(), filename);
        if (!file.exists()) plugin.saveResource(filename, false);
        Key key = Key.of(name);
        try { SquaremapProvider.get().iconRegistry().register(key, ImageIO.read(file)); }
        catch (Exception e) { plugin.getLogger().log(Level.WARNING, "Failed to register icon: " + name, e); }
        return key;
    }

    public static Key getIcon(Material m) {
        return switch (m) {
            case BLACK_BANNER,BLACK_WALL_BANNER -> BLACK;
            case BLUE_BANNER,BLUE_WALL_BANNER -> BLUE;
            case BROWN_BANNER,BROWN_WALL_BANNER -> BROWN;
            case CYAN_BANNER,CYAN_WALL_BANNER -> CYAN;
            case GRAY_BANNER,GRAY_WALL_BANNER -> GRAY;
            case GREEN_BANNER,GREEN_WALL_BANNER -> GREEN;
            case LIGHT_BLUE_BANNER,LIGHT_BLUE_WALL_BANNER -> LIGHT_BLUE;
            case LIGHT_GRAY_BANNER,LIGHT_GRAY_WALL_BANNER -> LIGHT_GRAY;
            case LIME_BANNER,LIME_WALL_BANNER -> LIME;
            case MAGENTA_BANNER,MAGENTA_WALL_BANNER -> MAGENTA;
            case ORANGE_BANNER,ORANGE_WALL_BANNER -> ORANGE;
            case PINK_BANNER,PINK_WALL_BANNER -> PINK;
            case PURPLE_BANNER,PURPLE_WALL_BANNER -> PURPLE;
            case RED_BANNER,RED_WALL_BANNER -> RED;
            case YELLOW_BANNER,YELLOW_WALL_BANNER -> YELLOW;
            default -> WHITE;
        };
    }

    public static Key getIcon(DyeColor c) {
        return switch (c) {
            case BLACK -> BLACK; case BLUE -> BLUE; case BROWN -> BROWN; case CYAN -> CYAN;
            case GRAY -> GRAY; case GREEN -> GREEN; case LIGHT_BLUE -> LIGHT_BLUE;
            case LIGHT_GRAY -> LIGHT_GRAY; case LIME -> LIME; case MAGENTA -> MAGENTA;
            case ORANGE -> ORANGE; case PINK -> PINK; case PURPLE -> PURPLE;
            case RED -> RED; case YELLOW -> YELLOW; case WHITE -> WHITE;
        };
    }

    public static void unregisterAll() {
        var registry = SquaremapProvider.get().iconRegistry();
        List.of(BLACK,BLUE,BROWN,CYAN,GRAY,GREEN,LIGHT_BLUE,LIGHT_GRAY,LIME,MAGENTA,ORANGE,PINK,PURPLE,RED,WHITE,YELLOW)
            .forEach(key -> { try { registry.unregister(key); } catch (Exception ignored) {} });
    }
}
