package dev.sculkcore.tasks;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.altar.RitualManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

public class RitualTask extends BukkitRunnable {
    private final int maxTicks;
    private final BossBar bossBar;
    private final ArmorStand as1;
    private final ArmorStand as2;
    private final Item item;
    private final String itemDisplayName;
    private final Location centerLoc;
    
    private int ticks = 0;
    private double particleAngle = 0.0;

    public RitualTask(int maxTicks, BossBar bossBar, ArmorStand as1, ArmorStand as2, Item item, String itemDisplayName, Location centerLoc) {
        this.maxTicks = maxTicks;
        this.bossBar = bossBar;
        this.as1 = as1;
        this.as2 = as2;
        this.item = item;
        this.itemDisplayName = itemDisplayName;
        this.centerLoc = centerLoc;
    }

    @Override
    public void run() {
        // Stop if item or stands are dead
        if (!item.isValid() || !as1.isValid() || !as2.isValid()) {
            cancel();
            cleanupEntities();
            return;
        }

        if (ticks >= maxTicks) {
            cancel();
            finishRitual();
            return;
        }

        // Lock item to the center of the altar
        Location lockLoc = centerLoc.clone().add(0.5, 0.5, 0.5);
        item.teleport(lockLoc);

        // Spawn ritual circle particles
        spawnParticles(lockLoc);

        // Update BossBar and ArmorStand countdown display
        double progress = 1.0 - ((double) ticks / maxTicks);
        bossBar.setProgress(Math.max(0.0, progress));

        int secondsRemaining = (maxTicks - ticks) / 20;
        int mins = secondsRemaining / 60;
        int secs = secondsRemaining % 60;
        String formattedTime = String.format("%d:%02d", mins, secs);

        as2.setCustomName(ChatColor.YELLOW + formattedTime);

        ticks++;
    }

    private void spawnParticles(Location lockLoc) {
        SculkCorePlugin plugin = SculkCorePlugin.getInstance();
        double radius = plugin.getConfig().getDouble("config.ritual_radius", 5.0);
        if (radius > 0.0) {
            for (int i = 0; i < 4; i++) {
                double angle = particleAngle + (Math.PI / 2.0) * i;
                double dx = Math.cos(angle) * radius;
                double dz = Math.sin(angle) * radius;
                Location particleLoc = centerLoc.clone().add(0.5 + dx, 0.75, 0.5 + dz);

                org.bukkit.Color color = RitualManager.getRitualParticleColor();
                particleLoc.getWorld().spawnParticle(Particle.ENTITY_EFFECT, particleLoc, 0, 0.0, 0.0, 0.0, color);
                particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 0, 0.0, 0.0, 0.0, new Particle.DustOptions(color, 2.0f));
                particleLoc.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 0, 0.0, 0.0, 0.0, 0.0);
            }
        }
        particleAngle += 0.09817477042468103;
    }

    private void finishRitual() {
        cleanupEntities();

        Location lockLoc = centerLoc.clone().add(0.5, 0.5, 0.5);
        lockLoc.getWorld().strikeLightningEffect(lockLoc);
        lockLoc.getWorld().spawnParticle(Particle.POOF, lockLoc, 500, 1.0, 1.0, 1.0, 1.0);
        lockLoc.getWorld().playSound(lockLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        lockLoc.getWorld().playSound(lockLoc, Sound.BLOCK_VAULT_CLOSE_SHUTTER, 1.0f, 1.0f);

        // Make item pickable
        item.setPickupDelay(20);
        item.setGravity(true);
        item.setNoPhysics(false);

        String endMsg = SculkCorePlugin.getInstance().getConfigManager().translateColor("&dThe ritual for &6" + itemDisplayName + "&d has ended!");
        Bukkit.broadcastMessage(endMsg);
    }

    private void cleanupEntities() {
        bossBar.setVisible(false);
        bossBar.removeAll();
        
        if (as1.isValid()) as1.remove();
        if (as2.isValid()) as2.remove();

        RitualManager.activeEntities.remove(as1);
        RitualManager.activeEntities.remove(as2);
        RitualManager.activeBossBars.remove(bossBar);
        if (RitualManager.activeBossBar == bossBar) {
            RitualManager.activeBossBar = null;
        }
    }
}
