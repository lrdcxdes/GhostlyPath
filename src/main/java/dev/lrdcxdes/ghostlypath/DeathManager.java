package dev.lrdcxdes.ghostlypath;

import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DeathManager {

    final GhostlyPath plugin;
    private final ConcurrentHashMap<UUID, GhostInfo> activeGhosts = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public DeathManager(GhostlyPath plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    public void createGhost(Player player, List<ItemStack> drops) {
        removeGhost(player.getUniqueId(), GhostRemoveReason.NEW_DEATH);

        Location location = player.getLocation();
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            playerHead.setItemMeta(meta);
        }

        Objects.requireNonNull(location.getWorld()).spawn(location, ItemDisplay.class, display -> {
            display.setItemStack(playerHead);
            display.setGlowing(true);
            display.setGlowColorOverride(Color.AQUA);
            display.setBrightness(new Display.Brightness(15, 15));

            Transformation transformation = display.getTransformation();
            transformation.getTranslation().set(-0.5f, 0.5f, -0.5f);
            display.setTransformation(transformation);

            activeGhosts.put(player.getUniqueId(), new GhostInfo(display, System.currentTimeMillis(), drops));
        });
    }

    public void removeGhost(UUID playerUUID, GhostRemoveReason reason) {
        GhostInfo ghostInfo = activeGhosts.remove(playerUUID);
        if (ghostInfo == null) return;

        if (ghostInfo.display().isValid()) {
            ghostInfo.display().remove();
        }

        Player player = plugin.getServer().getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            player.setCompassTarget(player.getWorld().getSpawnLocation());

            String message = plugin.getConfig().getString("messages." + reason.getMessageKey());
            if (message != null && !message.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        }

        // Если причина - нахождение призрака, возвращаем вещи
        if (reason == GhostRemoveReason.FOUND && player != null) {
            returnItems(player, ghostInfo.drops());
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
        return info != null ? info.display().getLocation() : null;
    }

    public void clearAllGhosts(boolean returnItemsToOnlinePlayers) {
        if (updateTask != null) updateTask.cancel();
        activeGhosts.forEach((uuid, info) -> {
            if (info.display().isValid()) {
                info.display().remove();
            }
            if (returnItemsToOnlinePlayers) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    returnItems(player, info.drops());
                }
            }
        });
        activeGhosts.clear();
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
                double removeDistanceSquared = Math.pow(plugin.getConfig().getDouble("remove-distance-blocks", 3), 2);

                for (UUID uuid : activeGhosts.keySet()) {
                    GhostInfo info = activeGhosts.get(uuid);
                    if (info == null || !info.display().isValid()) {
                        activeGhosts.remove(uuid);
                        continue;
                    }

                    if (timeoutMillis > 0 && (System.currentTimeMillis() - info.timestamp() > timeoutMillis)) {
                        removeGhost(uuid, GhostRemoveReason.TIMED_OUT);
                        continue;
                    }

                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player != null && player.isOnline() && !player.isDead() &&
                            player.getWorld().equals(info.display().getWorld()) &&
                            player.getLocation().distanceSquared(info.display().getLocation()) < removeDistanceSquared) {
                        removeGhost(uuid, GhostRemoveReason.FOUND);
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 20L); // Проверка каждую секунду
    }

    // Enum для причин удаления призрака
    public enum GhostRemoveReason {
        FOUND("ghost-removed-nearby"),
        TIMED_OUT("ghost-timed-out"),
        NEW_DEATH(""); // Нет сообщения при новой смерти

        private final String messageKey;
        GhostRemoveReason(String key) { this.messageKey = key; }
        public String getMessageKey() { return messageKey; }
    }

    // Внутренний класс для хранения информации
    private record GhostInfo(ItemDisplay display, long timestamp, List<ItemStack> drops) { }
}