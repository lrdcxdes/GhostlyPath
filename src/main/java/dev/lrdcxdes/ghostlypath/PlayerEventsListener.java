package dev.lrdcxdes.ghostlypath;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class PlayerEventsListener implements Listener {

    private final DeathManager deathManager;

    public PlayerEventsListener(DeathManager deathManager) {
        this.deathManager = deathManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!deathManager.plugin.getConfig().getBoolean("save-items-in-ghost", true)) {
            return;
        }

        Player player = event.getEntity();
        if (event.getKeepInventory()) return;

        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();

        deathManager.createGhost(player, drops);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLocation = deathManager.getDeathLocation(player.getUniqueId());

        if (deathLocation != null) {
            boolean compassEnabled = deathManager.plugin.getConfig().getBoolean("enable-compass-tracking", true);
            String messageKey = compassEnabled ? "messages.death-location-set" : "messages.compass-not-enabled";

            if(compassEnabled) {
                player.setCompassTarget(deathLocation);
            }

            String message = deathManager.plugin.getConfig().getString(messageKey);
            if (message != null && !message.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        }
    }
}