package dev.sculkcore.listeners;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTransformEvent;

public class VillagerProtectionListener implements Listener {
    private final SculkCorePlugin plugin;

    public VillagerProtectionListener(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVillagerDamage(EntityDamageEvent event) {
        if (!plugin.getConfig().getBoolean("rules.ban_killing_villagers", false)) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof Villager villager) {
            // Allow damage from Zombies (so they can be converted / part of standard mechanics)
            if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
                if (damageByEntityEvent.getDamager() instanceof Zombie) {
                    return;
                }
            }

            // If the villager has a profession (is not NONE), protect them
            if (villager.getProfession() != Villager.Profession.NONE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onVillagerTransform(EntityTransformEvent event) {
        if (!plugin.getConfig().getBoolean("rules.ban_killing_villagers", false)) {
            return;
        }

        Entity entity = event.getEntity();
        if (entity instanceof Villager) {
            // Block transformation into a Witch when struck by lightning
            if (event.getTransformReason() == EntityTransformEvent.TransformReason.LIGHTNING) {
                event.setCancelled(true);
            }
        }
    }
}
