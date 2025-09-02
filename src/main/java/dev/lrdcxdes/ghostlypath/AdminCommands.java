package dev.lrdcxdes.ghostlypath;

import org.bukkit.ChatColor;
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ghostlypath.admin.reload")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.no-permission", "&cYou do not have permission.")));
                return true;
            }

            plugin.reloadPluginConfig();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.config-reloaded", "&aGhostlyPath config reloaded.")));
            return true;
        }
        return false;
    }
}