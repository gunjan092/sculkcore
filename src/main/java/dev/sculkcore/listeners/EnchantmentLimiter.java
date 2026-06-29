package dev.sculkcore.listeners;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Enforces per-enchantment level caps configured via the /settings GUI.
 * Config key {@code enchant-limits.<enchant>}:
 *   -1 (or absent) = allowed at any level, 0 = banned (stripped), n = capped at n.
 */
public class EnchantmentLimiter implements Listener {
    private final SculkCorePlugin plugin;

    public EnchantmentLimiter(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    /** Configured limit for an enchantment, or -1 when unrestricted. */
    private int limitFor(Enchantment enchant) {
        return plugin.getConfig().getInt("enchant-limits." + enchant.getKey().getKey(), -1);
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        Map<Enchantment, Integer> toAdd = event.getEnchantsToAdd();
        Iterator<Map.Entry<Enchantment, Integer>> it = toAdd.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Enchantment, Integer> entry = it.next();
            int limit = limitFor(entry.getKey());
            if (limit == -1) continue;
            if (limit == 0) {
                it.remove();
            } else if (entry.getValue() > limit) {
                entry.setValue(limit);
            }
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null || result.getType() == Material.AIR) return;
        ItemStack clone = result.clone();
        if (capItem(clone)) {
            event.setResult(clone);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && capItem(item)) {
                player.getInventory().setItem(i, item);
            }
        }
    }

    /** Caps both direct and stored (book) enchantments. Returns true if changed. */
    private boolean capItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        boolean changed = false;

        for (Map.Entry<Enchantment, Integer> entry : new HashMap<>(item.getEnchantments()).entrySet()) {
            int limit = limitFor(entry.getKey());
            if (limit == -1) continue;
            if (limit == 0) {
                item.removeEnchantment(entry.getKey());
                changed = true;
            } else if (entry.getValue() > limit) {
                item.removeEnchantment(entry.getKey());
                item.addUnsafeEnchantment(entry.getKey(), limit);
                changed = true;
            }
        }

        if (item.getItemMeta() instanceof EnchantmentStorageMeta storage && storage.hasStoredEnchants()) {
            boolean metaChanged = false;
            for (Map.Entry<Enchantment, Integer> entry : new HashMap<>(storage.getStoredEnchants()).entrySet()) {
                int limit = limitFor(entry.getKey());
                if (limit == -1) continue;
                if (limit == 0) {
                    storage.removeStoredEnchant(entry.getKey());
                    metaChanged = true;
                } else if (entry.getValue() > limit) {
                    storage.removeStoredEnchant(entry.getKey());
                    storage.addStoredEnchant(entry.getKey(), limit, true);
                    metaChanged = true;
                }
            }
            if (metaChanged) {
                item.setItemMeta(storage);
                changed = true;
            }
        }

        return changed;
    }
}
