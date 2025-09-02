package dev.lrdcxdes.ghostlypath;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class GhostlyPath extends JavaPlugin {

    private DeathManager deathManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        File ghostsFile = new File(getDataFolder(), "ghosts.yml");
        this.deathManager = new DeathManager(this, ghostsFile);

        // Загружаем призраков из файла ПОСЛЕ того, как миры полностью загрузятся
        getServer().getScheduler().runTaskLater(this, () -> {
            deathManager.loadGhosts();
            getLogger().info("Loaded " + deathManager.getActiveGhostCount() + " ghosts from disk.");
        }, 1L); // 1 тик задержки - это стандартная практика

        getServer().getPluginManager().registerEvents(new PlayerEventsListener(deathManager), this);
        Objects.requireNonNull(getCommand("ghostlypath")).setExecutor(new AdminCommands(this));
        getLogger().info("GhostlyPath has been enabled!");
    }

    @Override
    public void onDisable() {
        // Больше не возвращаем вещи, а СОХРАНЯЕМ их на диск
        if (deathManager != null) {
            deathManager.saveGhosts();
            getLogger().info("Saved " + deathManager.getActiveGhostCount() + " active ghosts to disk.");
            // Очищаем сущности, чтобы не было "двойников" при /reload
            deathManager.clearAllGhostEntities();
        }
        getLogger().info("GhostlyPath has been disabled!");
    }

    public void reloadPluginConfig() {
        reloadConfig();
        deathManager.restartUpdateTask();
        getLogger().info("Configuration reloaded.");
    }
}