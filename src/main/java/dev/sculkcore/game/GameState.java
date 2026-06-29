package dev.sculkcore.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import dev.sculkcore.SculkCorePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameState {
    private static final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();
    private static final Map<String, Long> globalCooldowns = new HashMap<>();
    private static final Map<UUID, BukkitTask> combatTasks = new HashMap<>();

    public static boolean isCooldownActive(UUID uuid, String key) {
        return getCooldownRemaining(uuid, key) > 0;
    }

    public static boolean isGlobalCooldownActive(String key) {
        return getGlobalCooldownRemaining(key) > 0;
    }

    public static void setCooldown(Player player, String key, double seconds) {
        UUID uuid = player.getUniqueId();
        if (key.equals("combat")) {
            setCombatTag(player, (int) seconds);
        }
        playerCooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(key, System.currentTimeMillis() + (long) (seconds * 1000.0));
    }

    public static void setCooldown(UUID uuid, String key, double seconds) {
        playerCooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(key, System.currentTimeMillis() + (long) (seconds * 1000.0));
    }

    public static void setGlobalCooldown(String key, double seconds) {
        globalCooldowns.put(key, System.currentTimeMillis() + (long) (seconds * 1000.0));
    }

    public static double getGlobalCooldownRemaining(String key) {
        if (globalCooldowns.containsKey(key)) {
            long end = globalCooldowns.get(key);
            long remaining = end - System.currentTimeMillis();
            return Math.max(0.0, (double) remaining / 1000.0);
        }
        return 0.0;
    }

    public static double getCooldownRemaining(UUID uuid, String key) {
        if (playerCooldowns.containsKey(uuid) && playerCooldowns.get(uuid).containsKey(key)) {
            long end = playerCooldowns.get(uuid).get(key);
            long remaining = end - System.currentTimeMillis();
            return Math.max(0.0, (double) remaining / 1000.0);
        }
        return 0.0;
    }

    public static Map<String, Long> getActivePlayerCooldowns(UUID uuid) {
        Map<String, Long> active = new HashMap<>();
        Map<String, Long> map = playerCooldowns.get(uuid);
        if (map != null) {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Long> entry : map.entrySet()) {
                long remaining = entry.getValue() - now;
                if (remaining > 0) {
                    active.put(entry.getKey(), remaining);
                }
            }
        }
        return active;
    }

    public static void clearPlayerCooldowns(UUID uuid) {
        playerCooldowns.remove(uuid);
        BukkitTask task = combatTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private static void setCombatTag(Player player, int seconds) {
        String mode = SculkCorePlugin.getInstance().getConfig().getString("config.combat_display", "BOSSBAR");
        long minutes = seconds / 60;
        long secs = seconds % 60;
        String formattedTime = String.format("%d:%02d", minutes, secs);

        if (!isCooldownActive(player.getUniqueId(), "combat")) {
            if (mode.equalsIgnoreCase("MESSAGES") || mode.equalsIgnoreCase("BOTH")) {
                String message = SculkCorePlugin.getInstance().getConfig().getString("rules.combat_message", "&cYou are now in combat for &f&l<combat_time>")
                        .replace("<combat_time>", formattedTime);
                // Try translation if manager is initialized, otherwise fall back to simple translation
                String colored = (SculkCorePlugin.getInstance().getConfigManager() != null)
                        ? SculkCorePlugin.getInstance().getConfigManager().translateColor(message)
                        : org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
                player.sendMessage(colored);
            }
        }

        BukkitTask existing = combatTasks.remove(player.getUniqueId());
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask newTask = new BukkitRunnable() {
            @Override
            public void run() {
                String mode = SculkCorePlugin.getInstance().getConfig().getString("config.combat_display", "BOSSBAR");
                if (mode.equalsIgnoreCase("MESSAGES") || mode.equalsIgnoreCase("BOTH")) {
                    String endMsg = "&aYou are no longer in combat";
                    String coloredEnd = (SculkCorePlugin.getInstance().getConfigManager() != null)
                            ? SculkCorePlugin.getInstance().getConfigManager().translateColor(endMsg)
                            : org.bukkit.ChatColor.translateAlternateColorCodes('&', endMsg);
                    player.sendMessage(coloredEnd);
                }
                combatTasks.remove(player.getUniqueId());
            }
        }.runTaskLater(SculkCorePlugin.getInstance(), (long) seconds * 20L);

        combatTasks.put(player.getUniqueId(), newTask);
    }
}
