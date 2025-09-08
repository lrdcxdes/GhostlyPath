package dev.lrdcxdes.ghostlypath;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class GhostlyPath extends JavaPlugin {

    private DeathManager deathManager;
    // Public static MiniMessage instance for easy access across the plugin
    public static final MiniMessage MM = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        File ghostsFile = new File(getDataFolder(), "ghosts.yml");
        this.deathManager = new DeathManager(this, ghostsFile);

        // Load ghosts from file after worlds are fully loaded
        getServer().getScheduler().runTaskLater(this, () -> {
            deathManager.loadGhosts();
            getLogger().info("Loaded " + deathManager.getActiveGhostCount() + " ghosts from disk.");
        }, 1L);

        getServer().getPluginManager().registerEvents(new PlayerEventsListener(deathManager), this);
        Objects.requireNonNull(getCommand("ghostlypath")).setExecutor(new AdminCommands(this));
        getLogger().info("GhostlyPath has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all active ghosts to disk on shutdown
        if (deathManager != null) {
            deathManager.saveGhosts();
            getLogger().info("Saved " + deathManager.getActiveGhostCount() + " active ghosts to disk.");
            // Clean up entities to prevent duplicates on /reload
            deathManager.clearAllGhostEntities();
        }
        getLogger().info("GhostlyPath has been disabled!");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        if (deathManager != null) {
            deathManager.restartMainTask();
        }
        getLogger().info("Configuration reloaded.");
    }
}