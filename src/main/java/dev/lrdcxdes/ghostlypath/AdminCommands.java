package dev.lrdcxdes.ghostlypath;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AdminCommands implements CommandExecutor {

    private final GhostlyPath plugin;

    public AdminCommands(GhostlyPath plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ghostlypath.admin")) {
                sender.sendMessage(GhostlyPath.MM.deserialize(
                        plugin.getConfig().getString("messages.no-permission")));
                return true;
            }
            plugin.reloadPluginConfig();
            sender.sendMessage(GhostlyPath.MM.deserialize(
                    plugin.getConfig().getString("messages.config-reloaded")));
            return true;
        }
        return false;
    }
}