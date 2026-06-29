package dev.sculkcore.listeners;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.MerchantRecipe;

public class VillagerRestockListener implements Listener {
    private final SculkCorePlugin plugin;

    public VillagerRestockListener(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        if (!plugin.getConfig().getBoolean("rules.infinite_restock", false)) {
            return;
        }

        Entity clicked = event.getRightClicked();
        if (clicked instanceof Villager villager) {
            restock(villager);
        } else if (clicked instanceof WanderingTrader wanderingTrader) {
            for (MerchantRecipe recipe : wanderingTrader.getRecipes()) {
                recipe.setUses(0);
                recipe.setMaxUses(Integer.MAX_VALUE);
            }
        }
    }

    @EventHandler
    public void onTradeAcquire(VillagerAcquireTradeEvent event) {
        if (!plugin.getConfig().getBoolean("rules.infinite_restock", false)) {
            return;
        }

        AbstractVillager abstractVillager = event.getEntity();
        if (abstractVillager instanceof Villager villager) {
            restock(villager);
        }
    }

    public static void restock(Villager villager) {
        for (MerchantRecipe recipe : villager.getRecipes()) {
            recipe.setUses(0);
            recipe.setMaxUses(Integer.MAX_VALUE);
            recipe.setPriceMultiplier(1.0f);
            recipe.setSpecialPrice(0);
            recipe.setDemand(0);
            recipe.setIgnoreDiscounts(false);
        }
    }
}
