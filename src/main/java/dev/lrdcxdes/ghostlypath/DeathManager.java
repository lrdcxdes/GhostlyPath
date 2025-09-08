package dev.lrdcxdes.ghostlypath;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DeathManager {

    final GhostlyPath plugin;
    private final ConcurrentHashMap<UUID, GhostInfo> activeGhosts = new ConcurrentHashMap<>();
    private final File ghostsFile;
    private FileConfiguration ghostsConfig;
    private BukkitTask mainTask;

    // --- Cached Config Values ---
    private long ghostTimeoutTicks;
    private double retrieveDistanceSquared;
    private boolean allowLootingByOthers;
    private long ownerOnlyDuration;

    public DeathManager(GhostlyPath plugin, File ghostsFile) {
        this.plugin = plugin;
        this.ghostsFile = ghostsFile;
        loadConfigValues();
        startMainTask();
    }

    public void loadConfigValues() {
        FileConfiguration config = plugin.getConfig();
        this.ghostTimeoutTicks = config.getLong("ghost-timeout-ticks", 6000);
        double retrieveDistance = config.getDouble("retrieve-distance-blocks", 2.5);
        this.retrieveDistanceSquared = retrieveDistance * retrieveDistance;
        this.allowLootingByOthers = config.getBoolean("looting.allow-looting-by-others", true);
        this.ownerOnlyDuration = config.getLong("looting.owner-only-duration", 600);
    }

    private void startMainTask() {
        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeGhosts.isEmpty()) return;

                for (GhostInfo info : activeGhosts.values()) {
                    Location loc = info.getLocation();
                    World world = loc.getWorld();
                    if (world == null) continue;

                    // --- Chunk-based Despawn Logic ---
                    Chunk chunk = loc.getChunk();
                    if (chunk.isLoaded() && chunk.isEntitiesLoaded()) {
                        info.incrementTicksInLoadedChunk(20); // Task runs every 20 ticks (1s)
                        if (ghostTimeoutTicks > 0 && info.getTicksInLoadedChunk() >= ghostTimeoutTicks) {
                            removeGhost(info.getGhostUUID(), GhostRemoveReason.TIMED_OUT, null);
                            continue; // Ghost removed, move to the next one
                        }
                    }

                    // --- Owner Retrieval Logic ---
                    Player owner = Bukkit.getPlayer(info.getOwnerUUID());
                    if (owner != null && owner.isOnline() && !owner.isDead() && owner.getWorld().equals(world)) {
                        if (owner.getLocation().distanceSquared(loc) < retrieveDistanceSquared) {
                            removeGhost(info.getGhostUUID(), GhostRemoveReason.RETRIEVED, owner);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 20L); // Run every second
    }

    public void restartMainTask() {
        if (mainTask != null) mainTask.cancel();
        loadConfigValues();
        startMainTask();
    }

    public void createGhost(Player player, List<ItemStack> drops) {
        Location location = player.getEyeLocation();
        GhostInfo newGhostInfo = new GhostInfo(player.getUniqueId(), location, drops);
        activeGhosts.put(newGhostInfo.getGhostUUID(), newGhostInfo);
        spawnGhostEntity(newGhostInfo);
        saveGhosts();
    }

    private void spawnGhostEntity(GhostInfo info) {
        World world = info.getLocation().getWorld();
        if (world == null) return;

        world.spawn(info.getLocation(), ItemDisplay.class, head -> {
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(info.getOwnerUUID()));
            playerHead.setItemMeta(meta);

            head.setItemStack(playerHead);
            head.setBillboard(Display.Billboard.CENTER);
            head.setGlowing(true);
            head.setGlowColorOverride(Color.AQUA);

            Transformation transformation = head.getTransformation();
            transformation.getScale().set(0.8f, 0.8f, 0.8f);
            head.setTransformation(transformation);

            info.setGhostEntity(head);
        });
    }

    public void handleGhostInteraction(Player interactor, GhostInfo info) {
        UUID ownerUUID = info.getOwnerUUID();

        if (interactor.getUniqueId().equals(ownerUUID)) {
            removeGhost(info.getGhostUUID(), GhostRemoveReason.RETRIEVED, interactor);
            return;
        }

        if (!allowLootingByOthers) {
            interactor.sendMessage(GhostlyPath.MM.deserialize(plugin.getConfig().getString("messages.not-your-ghost")));
            return;
        }

        long timeSinceDeath = (System.currentTimeMillis() / 1000) - info.getTimestamp();
        if (timeSinceDeath < ownerOnlyDuration) {
            long remainingTime = ownerOnlyDuration - timeSinceDeath;
            String message = plugin.getConfig().getString("messages.owner-protection-active")
                    .replace("%time%", formatTime(remainingTime));
            interactor.sendMessage(GhostlyPath.MM.deserialize(message));
            return;
        }

        removeGhost(info.getGhostUUID(), GhostRemoveReason.LOOTED_BY_OTHER, interactor);
    }

    public void removeGhost(UUID ghostUUID, GhostRemoveReason reason, Player retriever) {
        GhostInfo ghostInfo = activeGhosts.remove(ghostUUID);
        if (ghostInfo == null) return;

        if (ghostInfo.getGhostEntity() != null && ghostInfo.getGhostEntity().isValid()) {
            ghostInfo.getGhostEntity().remove();
        }

        Player owner = Bukkit.getPlayer(ghostInfo.getOwnerUUID());

        // Handle Messages
        if (reason.getMessageKey() != null) {
            if (owner != null && owner.isOnline()) {
                String message = plugin.getConfig().getString("messages." + reason.getMessageKey());
                if (message != null) owner.sendMessage(GhostlyPath.MM.deserialize(message));
            }
        }

        if (reason == GhostRemoveReason.LOOTED_BY_OTHER && retriever != null) {
            if (owner != null && owner.isOnline()) {
                String ownerMsg = plugin.getConfig().getString("messages.owner-looted-notification")
                        .replace("%looter_name%", retriever.getName());
                owner.sendMessage(GhostlyPath.MM.deserialize(ownerMsg));
            }
            String looterMsg = plugin.getConfig().getString("messages.looted-by-other")
                    .replace("%player_name%", Objects.requireNonNull(Bukkit.getOfflinePlayer(ghostInfo.getOwnerUUID()).getName()));
            retriever.sendMessage(GhostlyPath.MM.deserialize(looterMsg));
        }

        // Handle Item Return
        if (retriever != null && (reason == GhostRemoveReason.RETRIEVED || reason == GhostRemoveReason.LOOTED_BY_OTHER)) {
            returnItems(retriever, ghostInfo.getDrops());
        }

        saveGhosts();
    }

    private void returnItems(Player player, List<ItemStack> items) {
        if (items == null || items.isEmpty()) return;

        Collection<ItemStack> remainingItems = player.getInventory().addItem(items.toArray(new ItemStack[0])).values();
        if (!remainingItems.isEmpty()) {
            String message = plugin.getConfig().getString("messages.items-dropped-full-inv");
            if (message != null) player.sendMessage(GhostlyPath.MM.deserialize(message));
            for (ItemStack item : remainingItems) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
    }

    public void saveGhosts() {
        if (ghostsConfig == null) ghostsConfig = YamlConfiguration.loadConfiguration(ghostsFile);
        ghostsConfig.set("ghosts", null);

        for (GhostInfo info : activeGhosts.values()) {
            String path = "ghosts." + info.getGhostUUID().toString();
            ghostsConfig.set(path + ".owner-uuid", info.getOwnerUUID().toString());
            ghostsConfig.set(path + ".location", info.getLocation());
            ghostsConfig.set(path + ".timestamp", info.getTimestamp());
            ghostsConfig.set(path + ".ticks-in-loaded-chunk", info.getTicksInLoadedChunk());
            try {
                ghostsConfig.set(path + ".items-base64", ItemSerializer.itemStackListToBase64(info.getDrops()));
            } catch (Exception e) {
                plugin.getLogger().severe("Could not serialize items for ghost " + info.getGhostUUID());
            }
        }
        try {
            ghostsConfig.save(ghostsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save ghosts to " + ghostsFile.getName());
        }
    }

    public void loadGhosts() {
        if (!ghostsFile.exists()) return;
        ghostsConfig = YamlConfiguration.loadConfiguration(ghostsFile);
        ConfigurationSection section = ghostsConfig.getConfigurationSection("ghosts");
        if (section == null) return;

        for (String ghostUuidString : section.getKeys(false)) {
            try {
                UUID ghostUUID = UUID.fromString(ghostUuidString);
                String path = ghostUuidString + ".";
                UUID ownerUUID = UUID.fromString(Objects.requireNonNull(section.getString(path + "owner-uuid")));
                Location loc = section.getLocation(path + "location");
                long timestamp = section.getLong(path + "timestamp");
                long ticks = section.getLong(path + "ticks-in-loaded-chunk", 0);
                List<ItemStack> items = ItemSerializer.itemStackListFromBase64(section.getString(path + "items-base64"));

                if (loc != null) {
                    GhostInfo loadedGhost = new GhostInfo(ghostUUID, ownerUUID, loc, timestamp, items, ticks);
                    activeGhosts.put(ghostUUID, loadedGhost);
                    spawnGhostEntity(loadedGhost);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load ghost " + ghostUuidString + ": " + e.getMessage());
            }
        }
    }

    public Optional<Location> getLatestDeathLocation(UUID playerUUID) {
        return activeGhosts.values().stream()
                .filter(info -> info.getOwnerUUID().equals(playerUUID))
                .max(Comparator.comparingLong(GhostInfo::getTimestamp))
                .map(GhostInfo::getLocation);
    }

    public Collection<GhostInfo> getNearbyGhosts(Location loc, double radius) {
        double radiusSquared = radius * radius;
        return activeGhosts.values().stream()
                .filter(info -> info.getLocation().getWorld().equals(loc.getWorld()) &&
                        info.getLocation().distanceSquared(loc) <= radiusSquared)
                .collect(Collectors.toList());
    }

    public int getActiveGhostCount() {
        return activeGhosts.size();
    }

    public void clearAllGhostEntities() {
        activeGhosts.values().forEach(info -> {
            if (info.getGhostEntity() != null) info.getGhostEntity().remove();
        });
    }

    private String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long remSeconds = seconds % 60;
        return minutes + "m " + remSeconds + "s";
    }

    public enum GhostRemoveReason {
        RETRIEVED("ghost-retrieved"),
        TIMED_OUT("ghost-timed-out"),
        LOOTED_BY_OTHER(null),
        NEW_DEATH(null);

        private final String messageKey;
        GhostRemoveReason(String key) { this.messageKey = key; }
        public String getMessageKey() { return messageKey; }
    }

    public static class GhostInfo {
        private final UUID ghostUUID;
        private final UUID ownerUUID;
        private final Location location;
        private final long timestamp; // Stored in seconds
        private final List<ItemStack> drops;
        private long ticksInLoadedChunk;
        private transient ItemDisplay ghostEntity;

        public GhostInfo(UUID ownerUUID, Location location, List<ItemStack> drops) {
            this.ghostUUID = UUID.randomUUID();
            this.ownerUUID = ownerUUID;
            this.location = location;
            this.drops = new ArrayList<>(drops);
            this.timestamp = System.currentTimeMillis() / 1000;
            this.ticksInLoadedChunk = 0;
        }

        // Constructor for loading from file
        public GhostInfo(UUID ghostUUID, UUID ownerUUID, Location loc, long ts, List<ItemStack> items, long ticks) {
            this.ghostUUID = ghostUUID;
            this.ownerUUID = ownerUUID;
            this.location = loc;
            this.timestamp = ts;
            this.drops = items;
            this.ticksInLoadedChunk = ticks;
        }

        public UUID getGhostUUID() { return ghostUUID; }
        public UUID getOwnerUUID() { return ownerUUID; }
        public Location getLocation() { return location; }
        public long getTimestamp() { return timestamp; }
        public List<ItemStack> getDrops() { return drops; }
        public ItemDisplay getGhostEntity() { return ghostEntity; }
        public void setGhostEntity(ItemDisplay ghostEntity) { this.ghostEntity = ghostEntity; }
        public long getTicksInLoadedChunk() { return ticksInLoadedChunk; }
        public void incrementTicksInLoadedChunk(long ticks) { this.ticksInLoadedChunk += ticks; }
    }
}