package dev.sculkcore.listeners;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class CombatListener implements Listener {
    private final SculkCorePlugin plugin;

    public CombatListener(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onExtraDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        if (event.isCancelled() || event.getDamage() <= 1.0) {
            return;
        }

        double extraDamage = plugin.getConfig().getDouble("rules.extra_damage", 0.0);
        if (extraDamage > 0.0) {
            event.setDamage(event.getDamage() + extraDamage);
        }
    }

    @EventHandler
    public void onShieldBreak(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        ItemStack mainHand = attacker.getInventory().getItemInMainHand();
        if (mainHand.getType() != Material.MACE) {
            return;
        }

        if (!plugin.getConfig().getBoolean("rules.mace_stun_shield", false)) {
            return;
        }

        // Stun victim if blocking facing the attacker
        if (victim.isBlocking()) {
            Vector attackerDir = attacker.getLocation().toVector().subtract(victim.getLocation().toVector()).normalize();
            Vector victimLookDir = victim.getLocation().getDirection().normalize();
            double dot = victimLookDir.dot(attackerDir);

            // If they are facing each other
            if (dot >= 0.0) {
                event.setCancelled(true);
                victim.clearActiveItem();
                
                int shieldCooldown = plugin.getConfig().getInt("rules.shield_cooldown", 100);
                victim.setCooldown(Material.SHIELD, shieldCooldown);
                victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.isCancelled()) {
            return;
        }

        boolean unlimitedLifetime = plugin.getConfig().getBoolean("rules.drops", true);
        boolean invulnerable = plugin.getConfig().getBoolean("rules.immortal_item", false);

        ArrayList<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();

        Location loc = event.getEntity().getLocation();
        for (ItemStack stack : drops) {
            if (stack == null || stack.getType() == Material.AIR) continue;
            
            Item item = loc.getWorld().dropItemNaturally(loc, stack);
            if (unlimitedLifetime) {
                item.setUnlimitedLifetime(true);
            }
            if (invulnerable) {
                item.setInvulnerable(true);
            }
            
            // Check glowing item meta flag
            if (stack.getItemMeta() != null && stack.getItemMeta().getPersistentDataContainer().has(SculkCorePlugin.glowing, PersistentDataType.BOOLEAN)) {
                item.setGlowing(true);
            }
        }
    }

    @EventHandler
    public void onImmortalItemDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item) {
            Item item = (Item) event.getEntity();
            if (item.isInvulnerable()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onCombatTag(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || event.getFinalDamage() <= 0.0) {
            return;
        }
        if (!plugin.getConfig().getBoolean("rules.combat_system", false)) {
            return;
        }

        Player victim = null;
        Player damager = null;

        if (event.getEntity() instanceof Player) {
            victim = (Player) event.getEntity();
        }

        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                damager = (Player) projectile.getShooter();
            }
        }

        if (victim != null && damager != null && victim != damager) {
            int tagTime = plugin.getConfig().getInt("config.combat_tag_time", 30);
            
            dev.sculkcore.game.GameState.setCooldown(victim, "combat", tagTime);
            dev.sculkcore.game.GameState.setCooldown(damager, "combat", tagTime);

            if (plugin.getConfig().getBoolean("config.combat_trident", false)) {
                victim.setCooldown(Material.TRIDENT, tagTime * 20);
                damager.setCooldown(Material.TRIDENT, tagTime * 20);
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!plugin.getConfig().getBoolean("rules.combat_system", false)) return;
        if (plugin.getConfig().getBoolean("config.combat_log_no_restock", true)) {
            if (dev.sculkcore.game.GameState.isCooldownActive(event.getPlayer().getUniqueId(), "combat")) {
                event.getPlayer().sendMessage("§cYou can't restock while in combat");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onGlideToggle(org.bukkit.event.entity.EntityToggleGlideEvent event) {
        if (!plugin.getConfig().getBoolean("rules.combat_system", false)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getConfig().getBoolean("config.combat_elytra", false)) {
            if (dev.sculkcore.game.GameState.isCooldownActive(player.getUniqueId(), "combat")) {
                if (event.isGliding() && player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() == Material.ELYTRA) {
                    player.sendMessage("§cYou can't use elytra while in combat");
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onElytraInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("rules.combat_system", false)) return;
        if (event.getItem() == null || event.getItem().getType() != Material.ELYTRA) return;
        if (!event.getAction().isRightClick()) return;
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("config.combat_elytra", false)) {
            if (dev.sculkcore.game.GameState.isCooldownActive(player.getUniqueId(), "combat")) {
                player.sendMessage("§cYou can't use an elytra while in combat");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onRiptide(org.bukkit.event.player.PlayerRiptideEvent event) {
        if (!plugin.getConfig().getBoolean("rules.combat_system", false)) return;
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("config.combat_trident", false)) {
            if (dev.sculkcore.game.GameState.isCooldownActive(player.getUniqueId(), "combat")) {
                player.sendMessage("§cYou can't use riptide while in combat");
                event.getVelocity().setX(0).setY(0).setZ(0);
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.setVelocity(new Vector(0,0,0)), 1L);
            }
        }
    }

    @EventHandler
    public void onPlayerKick(org.bukkit.event.player.PlayerKickEvent event) {
        dev.sculkcore.game.GameState.setCooldown(event.getPlayer().getUniqueId(), "kickedbyserver", 1.0);
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("rules.combat_system", false)) return;
        Player player = event.getPlayer();
        java.util.UUID uuid = player.getUniqueId();
        if (dev.sculkcore.game.GameState.isCooldownActive(uuid, "combat")) {
            if (dev.sculkcore.game.GameState.isCooldownActive(uuid, "kickedbyserver")) {
                event.setQuitMessage("");
                Bukkit.broadcastMessage(player.getName() + " was kicked from the server");
                return;
            }
            String mode = plugin.getConfig().getString("config.combat_log_mode", "ANNOUNCE");
            if (mode.equalsIgnoreCase("ANNOUNCE")) {
                Bukkit.broadcastMessage(player.getName() + " is suspected of combat logging!");
            } else if (mode.equalsIgnoreCase("KILL")) {
                Bukkit.broadcastMessage(player.getName() + " has died due to combat logging!");
                player.setHealth(0.0);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onCombatPlayerDeath(PlayerDeathEvent event) {
        dev.sculkcore.game.GameState.setCooldown(event.getEntity().getUniqueId(), "combat", 0.0);
    }
}
