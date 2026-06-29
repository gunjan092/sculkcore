package dev.sculkcore.listeners;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class GraceListener implements Listener {
    private final SculkCorePlugin plugin;

    public GraceListener(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        
        // Cancel all damage before game starts (if world border size is smaller than the pre-start border)
        if (player.getWorld().getWorldBorder().getSize() < plugin.beforeStartBorder) {
            event.setCancelled(true);
            return;
        }

        // Cancel all damage if player immunity during grace is enabled
        boolean immunity = plugin.getConfig().getBoolean("config.immunity_during_grace", true);
        if (immunity && GameState.isGlobalCooldownActive("grace")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = null;

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
            }
        }

        if (attacker != null) {
            boolean pvpConfig = plugin.getConfig().getBoolean("rules.pvp", true);
            boolean grace = GameState.isGlobalCooldownActive("grace");
            
            // Cancel PvP if grace is active or PvP is globally disabled
            if (grace || !pvpConfig) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        
        // Cancel hunger before game start (when border size is smaller than before_start_border)
        if (player.getWorld().getWorldBorder().getSize() < plugin.beforeStartBorder) {
            event.setCancelled(true);
        }
    }
}
