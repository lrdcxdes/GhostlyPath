package dev.lrdcxdes.ghostlypath;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class GhostlyPath extends JavaPlugin {

    private DeathManager deathManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.deathManager = new DeathManager(this);
        getServer().getPluginManager().registerEvents(new PlayerEventsListener(deathManager), this);
        Objects.requireNonNull(getCommand("ghostlypath")).setExecutor(new AdminCommands(this));
        getLogger().info("GhostlyPath has been enabled!");
    }

    @Override
    public void onDisable() {
        if (deathManager != null) {
            // При выключении сервера возвращаем вещи всем онлайн игрокам, чтобы они не потерялись
            deathManager.clearAllGhosts(true);
        }
        getLogger().info("GhostlyPath has been disabled!");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        deathManager.restartUpdateTask();
        getLogger().info("Configuration reloaded.");
    }
}