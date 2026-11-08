package gg.vanezus.waypoint.commands;

import gg.vanezus.waypoint.Waypoint;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;

public class ReloadCommand implements BasicCommand {

    private final Waypoint plugin;

    public ReloadCommand(Waypoint plugin) { this.plugin = plugin; }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        plugin.reloadPluginConfig();
        stack.getSender().sendMessage(plugin.getPluginConfig().getReloadSuccessMessage());
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) { return List.of(); }

    @Override
    public boolean canUse(CommandSender sender) { return sender.hasPermission("waypoints.reload"); }
}
