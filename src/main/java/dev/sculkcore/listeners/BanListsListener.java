package dev.sculkcore.listeners;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Enforces the configurable ban lists driven by the /settings GUI:
 *  - banned-tipped        : tipped-arrow effects players may not shoot
 *  - banned-tier1-effects : potion effects banned at level I (amplifier 0)
 *  - banned-tier2-effects : potion effects banned at level II+ (amplifier >= 1)
 */
public class BanListsListener implements Listener {
    private final SculkCorePlugin plugin;

    public BanListsListener(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    // --- Tipped arrows -----------------------------------------------------

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack consumable = event.getConsumable();
        if (consumable == null || consumable.getType() != Material.TIPPED_ARROW) return;
        if (!(consumable.getItemMeta() instanceof PotionMeta meta)) return;

        List<String> banned = plugin.getConfig().getStringList("banned-tipped");
        if (banned.isEmpty()) return;

        for (PotionEffect effect : allEffects(meta)) {
            if (banned.contains(effect.getType().getKey().getKey())) {
                event.setCancelled(true);
                player.sendMessage("§cThat tipped arrow is banned on this server.");
                return;
            }
        }
    }

    // --- Drinking potions --------------------------------------------------

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getType() != Material.POTION) return;
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return;

        if (isPotionBanned(meta)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cThat potion is banned on this server.");
        }
    }

    // --- Throwing splash / lingering potions -------------------------------

    @EventHandler
    public void onThrowPotion(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof ThrownPotion potion)) return;
        ItemStack item = potion.getItem();
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return;

        if (isPotionBanned(meta)) {
            event.setCancelled(true);
            if (potion.getShooter() instanceof Player shooter) {
                shooter.sendMessage("§cThat potion is banned on this server.");
            }
        }
    }

    // --- Helpers -----------------------------------------------------------

    private boolean isPotionBanned(PotionMeta meta) {
        List<String> tier1 = plugin.getConfig().getStringList("banned-tier1-effects");
        List<String> tier2 = plugin.getConfig().getStringList("banned-tier2-effects");
        if (tier1.isEmpty() && tier2.isEmpty()) return false;

        for (PotionEffect effect : allEffects(meta)) {
            String key = effect.getType().getKey().getKey();
            if (effect.getAmplifier() <= 0 && tier1.contains(key)) return true;
            if (effect.getAmplifier() >= 1 && tier2.contains(key)) return true;
        }
        return false;
    }

    private List<PotionEffect> allEffects(PotionMeta meta) {
        List<PotionEffect> effects = new ArrayList<>();
        PotionType base = meta.getBasePotionType();
        if (base != null) {
            effects.addAll(base.getPotionEffects());
        }
        effects.addAll(meta.getCustomEffects());
        return effects;
    }
}
