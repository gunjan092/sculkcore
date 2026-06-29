package dev.sculkcore.listeners;

import dev.sculkcore.SculkCorePlugin;
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
}
