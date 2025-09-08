package dev.lrdcxdes.ghostlypath;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlayerEventsListener implements Listener {

    private final DeathManager deathManager;

    public PlayerEventsListener(DeathManager deathManager) {
        this.deathManager = deathManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!deathManager.plugin.getConfig().getBoolean("save-items-in-ghost", true) ||
                player.getGameMode() == GameMode.SPECTATOR ||
                event.getKeepInventory() ||
                event.getDrops().isEmpty()) {
            return;
        }

        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();
        deathManager.createGhost(player, drops);
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemDisplay)) return;

        // Efficiently check if the interacted entity is a ghost
        for (DeathManager.GhostInfo info : deathManager.getNearbyGhosts(event.getRightClicked().getLocation(), 0.5)) {
            if (info.getGhostEntity() != null && info.getGhostEntity().equals(event.getRightClicked())) {
                deathManager.handleGhostInteraction(event.getPlayer(), info);
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Optional<Location> deathLocationOpt = deathManager.getLatestDeathLocation(player.getUniqueId());

        deathLocationOpt.ifPresent(deathLocation -> {
            boolean compassEnabled = deathManager.plugin.getConfig().getBoolean("enable-compass-tracking", true);
            String messageKey = compassEnabled ? "messages.death-location-set" : "messages.compass-not-enabled";

            if (compassEnabled) {
                player.setCompassTarget(deathLocation);
            }

            String message = deathManager.plugin.getConfig().getString(messageKey);
            if (message != null && !message.isEmpty()) {
                player.sendMessage(GhostlyPath.MM.deserialize(message));
            }
        });
    }
}