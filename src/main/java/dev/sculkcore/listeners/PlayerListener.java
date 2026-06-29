package dev.sculkcore.listeners;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.items.SpecialItems;
import io.papermc.paper.event.block.VaultChangeStateEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public class PlayerListener implements Listener {
    private final SculkCorePlugin plugin;

    public PlayerListener(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String sound = plugin.getConfig().getString("custom_join_sound", "");
        if (sound != null && !sound.isEmpty()) {
            float pitch = (float) plugin.getConfig().getDouble("custom_join_sound_pitch", 1.0);
            
            // Play join sound 5 ticks later
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.getWorld().playSound(player, sound, 1.0f, pitch);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.getWorld().playSound(online, sound, 1.0f, pitch);
                    }
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String sound = plugin.getConfig().getString("custom_leave_sound", "");
        if (sound != null && !sound.isEmpty()) {
            float pitch = (float) plugin.getConfig().getDouble("custom_leave_sound_pitch", 1.0);
            player.getWorld().playSound(player, sound, 1.0f, pitch);
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.getWorld().playSound(online, sound, 1.0f, pitch);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getEntity();
        
        // 1. Play death sound and update death message
        float pitch = (float) plugin.getConfig().getDouble("rules.death_sound_pitch", 1.0);
        String deathMsgTemplate = plugin.getConfig().getString("death_message", null);
        String killerName = player.getKiller() != null ? player.getKiller().getName() : "unknown";
        String deathSound = plugin.getConfig().getString("rules.death_sound", "minecraft:entity.player.death");
        
        player.getWorld().playSound(player.getLocation(), deathSound, 1.0f, pitch);
        
        if (deathMsgTemplate != null && !deathMsgTemplate.equals("null")) {
            String deathMessage = deathMsgTemplate
                    .replace("<attacker>", killerName)
                    .replace("<victim>", player.getName());
            event.setDeathMessage(deathMessage);
        }

        // 2. Handle custom item drops (unlimited lifetime, invulnerability, and glow)
        boolean unlimitedLifetime = plugin.getConfig().getBoolean("rules.drops", true);
        boolean immortalItem = plugin.getConfig().getBoolean("rules.immortal_item", false);
        
        ArrayList<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();
        Location location = player.getLocation();
        
        for (ItemStack itemStack : drops) {
            Item item = location.getWorld().dropItemNaturally(location, itemStack);
            if (unlimitedLifetime) {
                item.setUnlimitedLifetime(true);
            }
            if (immortalItem) {
                item.setInvulnerable(true);
            }
            
            // Check if the item has the glowing namespaced key
            if (itemStack.getItemMeta() != null && 
                itemStack.getItemMeta().getPersistentDataContainer().has(SculkCorePlugin.glowing, PersistentDataType.BOOLEAN)) {
                item.setGlowing(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item) {
            Item item = (Item) event.getEntity();
            if (item.isInvulnerable()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onWardenDeath(EntityDeathEvent event) {
        if (event.getEntityType() == EntityType.WARDEN) {
            if (plugin.getConfig().getBoolean("rules.warden", false)) {
                Location location = event.getEntity().getLocation();
                location.getWorld().dropItemNaturally(location, SpecialItems.createWardenDrop());
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (player.getWorld().getWorldBorder().getSize() < (double) plugin.beforeStartBorder) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onVaultChangeState(VaultChangeStateEvent event) {
        if (plugin.getConfig().getBoolean("rules.disable_vaults", false)) {
            event.setCancelled(true);
        }
    }
}
