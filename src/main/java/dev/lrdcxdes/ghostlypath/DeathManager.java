package dev.lrdcxdes.ghostlypath;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
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

public class DeathManager {

    final GhostlyPath plugin;
    private final ConcurrentHashMap<UUID, GhostInfo> activeGhosts = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    // --- Новые поля для работы с файлом ---
    private final File ghostsFile;
    private FileConfiguration ghostsConfig;

    public DeathManager(GhostlyPath plugin, File ghostsFile) {
        this.plugin = plugin;
        this.ghostsFile = ghostsFile;
        // Загрузка будет вызвана из main класса с задержкой
        startUpdateTask();
    }

    public void createGhost(Player player, List<ItemStack> drops) {
        removeGhost(player.getUniqueId(), GhostRemoveReason.NEW_DEATH, null);

        Location location = player.getLocation();

        // Создаем GhostInfo до спавна сущности
        GhostInfo newGhostInfo = new GhostInfo(player.getUniqueId(), location, System.currentTimeMillis(), drops);
        activeGhosts.put(player.getUniqueId(), newGhostInfo);

        // Спавним сущность и привязываем ее к GhostInfo
        spawnGhostEntity(newGhostInfo);

        saveGhosts(); // Сохраняем изменения на диск
    }

    // Отдельный метод для спавна сущности
    private void spawnGhostEntity(GhostInfo ghostInfo) {
        Location location = ghostInfo.getLocation();
        if (location.getWorld() == null) {
            plugin.getLogger().warning("Could not spawn ghost for " + ghostInfo.getOwnerUUID() + ", world is not loaded.");
            return;
        }

        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(ghostInfo.getOwnerUUID()));
            playerHead.setItemMeta(meta);
        }

        location.getWorld().spawn(location, ItemDisplay.class, display -> {
            display.setItemStack(playerHead);
            display.setGlowing(true);
            display.setGlowColorOverride(Color.AQUA);
            display.setBrightness(new Display.Brightness(15, 15));

            Transformation transformation = display.getTransformation();
            transformation.getTranslation().set(-0.5f, 0.5f, -0.5f);
            display.setTransformation(transformation);

            // Связываем созданную сущность с нашим объектом GhostInfo
            ghostInfo.setDisplayEntity(display);
        });
    }

    public void removeGhost(UUID ownerUUID, GhostRemoveReason reason, Player looter) {
        GhostInfo ghostInfo = activeGhosts.remove(ownerUUID);
        if (ghostInfo == null) return;

        if (ghostInfo.getDisplay().isValid()) {
            ghostInfo.getDisplay().remove();
        }

        Player owner = plugin.getServer().getPlayer(ownerUUID);

        // Логика отправки сообщений
        if (owner != null && owner.isOnline()) {
            owner.setCompassTarget(owner.getWorld().getSpawnLocation());
            if (reason.getMessageKey() != null && !reason.getMessageKey().isEmpty()) {
                String message;
                if (reason == GhostRemoveReason.LOOTED_BY_OTHER && looter != null) {
                    message = plugin.getConfig().getString("messages.owner-looted-notification", "&cYour items were stolen by %looter_name%!")
                            .replace("%looter_name%", looter.getName());
                } else {
                    message = plugin.getConfig().getString("messages." + reason.getMessageKey());
                }
                if (message != null) owner.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        }

        // Логика возврата вещей
        if (reason == GhostRemoveReason.FOUND && owner != null) {
            returnItems(owner, ghostInfo.getDrops());
        } else if (reason == GhostRemoveReason.LOOTED_BY_OTHER && looter != null) {
            returnItems(looter, ghostInfo.getDrops());
            String looterMessage = plugin.getConfig().getString("messages.looted-by-other", "&aYou looted %player_name%'s items!")
                    .replace("%player_name%", Objects.requireNonNull(Bukkit.getOfflinePlayer(ownerUUID).getName()));
            looter.sendMessage(ChatColor.translateAlternateColorCodes('&', looterMessage));
        }

        saveGhosts(); // Сохраняем изменения (удаление) на диск
    }

    public void saveGhosts() {
        if (ghostsConfig == null) {
            ghostsConfig = YamlConfiguration.loadConfiguration(ghostsFile);
        }
        // Очищаем старые данные
        ghostsConfig.set("ghosts", null);

        for (Map.Entry<UUID, GhostInfo> entry : activeGhosts.entrySet()) {
            String uuid = entry.getKey().toString();
            GhostInfo info = entry.getValue();

            ghostsConfig.set("ghosts." + uuid + ".location", info.getLocation());
            ghostsConfig.set("ghosts." + uuid + ".timestamp", info.getTimestamp());
            ghostsConfig.set("ghosts." + uuid + ".items", info.getDrops());
        }

        try {
            ghostsConfig.save(ghostsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save ghosts to " + ghostsFile);
            plugin.getLogger().severe(e.getMessage());
        }
    }

    public void loadGhosts() {
        if (!ghostsFile.exists()) {
            return;
        }
        ghostsConfig = YamlConfiguration.loadConfiguration(ghostsFile);
        ConfigurationSection section = ghostsConfig.getConfigurationSection("ghosts");
        if (section == null) return;

        for (String uuidString : section.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            Location loc = section.getLocation(uuidString + ".location");
            long timestamp = section.getLong(uuidString + ".timestamp");

            // --- ИСПРАВЛЕННЫЙ БЛОК ---

            List<?> rawItemList = section.getList(uuidString + ".items");
            List<ItemStack> items = new ArrayList<>(); // Создаем новый, гарантированно безопасный список

            if (rawItemList != null) {
                for (Object obj : rawItemList) {
                    // Проверяем, действительно ли это предмет
                    if (obj instanceof ItemStack) {
                        items.add((ItemStack) obj); // Теперь каст безопасен
                    } else {
                        // Логируем ошибку, если в файле мусор, но не падаем
                        plugin.getLogger().warning("Found non-ItemStack data in ghost for " + uuidString + ". Ignoring it.");
                    }
                }
            }
            // --- КОНЕЦ ИСПРАВЛЕННОГО БЛОКА ---

            if (loc != null) {
                GhostInfo loadedGhost = new GhostInfo(uuid, loc, timestamp, items);
                activeGhosts.put(uuid, loadedGhost);

                // Спавним сущность в мире
                spawnGhostEntity(loadedGhost);
            }
        }
    }

    private void returnItems(Player player, List<ItemStack> items) {
        if (items == null || items.isEmpty()) return;

        Collection<ItemStack> remainingItems = player.getInventory().addItem(items.toArray(new ItemStack[0])).values();

        if (!remainingItems.isEmpty()) {
            String message = plugin.getConfig().getString("messages.items-dropped-full-inv");
            if (message != null && !message.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
            // Выбрасываем на землю то, что не поместилось
            for (ItemStack item : remainingItems) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
    }

    public Location getDeathLocation(UUID playerUUID) {
        GhostInfo info = activeGhosts.get(playerUUID);
        return info != null ? info.getDisplay().getLocation() : null;
    }

    public int getActiveGhostCount() {
        return activeGhosts.size();
    }

    // Этот метод теперь просто удаляет сущности, не трогая данные
    public void clearAllGhostEntities() {
        activeGhosts.values().forEach(info -> {
            if (info.getDisplay() != null && info.getDisplay().isValid()) {
                info.getDisplay().remove();
            }
        });
    }

    public void restartUpdateTask() {
        if (updateTask != null) updateTask.cancel();
        startUpdateTask();
    }

    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeGhosts.isEmpty()) return;

                long timeoutMillis = plugin.getConfig().getLong("ghost-timeout-seconds", 1800) * 1000;
                double removeDistance = plugin.getConfig().getDouble("remove-distance-blocks", 3);
                double removeDistanceSquared = Math.pow(removeDistance, 2);
                boolean allowLootingByOthers = plugin.getConfig().getBoolean("allow-looting-by-others", false);

                for (GhostInfo info : activeGhosts.values()) {
                    if (!info.getDisplay().isValid()) {
                        activeGhosts.remove(info.ownerUUID);
                        continue;
                    }

                    // 1. Проверка по таймауту
                    if (timeoutMillis > 0 && (System.currentTimeMillis() - info.getTimestamp() > timeoutMillis)) {
                        removeGhost(info.getOwnerUUID(), GhostRemoveReason.TIMED_OUT, null);
                        continue;
                    }

                    // 2. Проверка, подошел ли владелец
                    Player owner = Bukkit.getPlayer(info.getOwnerUUID());
                    if (owner != null && owner.isOnline() && !owner.isDead() &&
                            owner.getWorld().equals(info.getDisplay().getWorld()) &&
                            owner.getLocation().distanceSquared(info.getDisplay().getLocation()) < removeDistanceSquared) {
                        removeGhost(info.getOwnerUUID(), GhostRemoveReason.FOUND, null);
                        continue; // Переходим к следующему призраку
                    }

                    // 3. Если разрешено, проверяем других игроков
                    if (allowLootingByOthers) {
                        Location ghostLocation = info.getDisplay().getLocation();
                        // Используем getNearbyEntities - это самый надежный метод
                        for (Entity entity : Objects.requireNonNull(ghostLocation.getWorld()).getNearbyEntities(ghostLocation, removeDistance, removeDistance, removeDistance)) {
                            // Проверяем, что сущность является игроком
                            if (!(entity instanceof Player looter)) {
                                continue;
                            }

                            // Исключаем владельца и игроков в креативе/спектейтере
                            if (looter.getUniqueId().equals(info.ownerUUID) || looter.getGameMode() == GameMode.CREATIVE || looter.getGameMode() == GameMode.SPECTATOR) {
                                continue;
                            }

                            // Первый же подошедший игрок забирает лут
                            removeGhost(info.ownerUUID, GhostRemoveReason.LOOTED_BY_OTHER, looter);
                            break; // Прерываем цикл, так как призрак уже удален
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 20L);
    }

    public enum GhostRemoveReason {
        FOUND("ghost-removed-nearby"),
        TIMED_OUT("ghost-timed-out"),
        LOOTED_BY_OTHER(""), // Сообщение владельцу обрабатывается отдельно
        NEW_DEATH("");

        private final String messageKey;
        GhostRemoveReason(String key) { this.messageKey = key; }
        public String getMessageKey() { return messageKey; }
    }
    
    private static class GhostInfo {
        private final UUID ownerUUID;
        private final Location location; // Теперь храним Location
        private ItemDisplay display; // Сущность может быть null при загрузке
        private final long timestamp;
        private final List<ItemStack> drops;

        public GhostInfo(UUID ownerUUID, Location location, long timestamp, List<ItemStack> drops) {
            this.ownerUUID = ownerUUID;
            this.location = location;
            this.timestamp = timestamp;
            this.drops = drops;
        }

        public void setDisplayEntity(ItemDisplay display) { this.display = display; }

        public UUID getOwnerUUID() { return ownerUUID; }
        public Location getLocation() { return location; }
        public ItemDisplay getDisplay() { return display; }
        public long getTimestamp() { return timestamp; }
        public List<ItemStack> getDrops() { return drops; }
    }
}