package dev.sculkcore.altar;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.game.GameState;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AltarListener implements Listener {
    private final SculkCorePlugin plugin;

    public AltarListener(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAltarInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        String materialName = plugin.getConfig().getString("altarSettings.blocktype", "STRUCTURE_BLOCK");
        if (!event.getClickedBlock().getType().name().contains(materialName.toUpperCase())) {
            return;
        }

        Location loc = event.getClickedBlock().getLocation();
        AltarInstance instance = plugin.getAltarManager().activeAltars.get(loc);
        if (instance == null) {
            return;
        }

        event.setCancelled(true);

        if (plugin.getAltarManager().isAltarUsed(loc)) {
            return;
        }

        String altarId = instance.getAltarTypeId();
        if (!plugin.getConfig().getBoolean("altars." + altarId + ".enabled", true)) {
            return;
        }

        AltarConfig config = plugin.getConfigManager().altars.get(altarId);
        if (config == null || config.getRecipes().isEmpty()) {
            event.getPlayer().sendMessage(ChatColor.RED + "No valid recipe found!");
            return;
        }

        Player player = event.getPlayer();
        AltarRecipe recipe = config.getRecipes().values().iterator().next();

        List<AltarIngredient> missing = plugin.getAltarManager().checkIngredients(player, recipe);
        if (!missing.isEmpty()) {
            if (!GameState.isCooldownActive(player.getUniqueId(), "altar_msg_spam")) {
                plugin.getAltarManager().sendMissingItemsMessage(player, missing);
                GameState.setCooldown(player, "altar_msg_spam", 1.0);
            }
            return;
        }

        // Consume ingredients
        boolean consume = plugin.getConfig().getBoolean("altars." + altarId + ".consume_materials", true);
        if (consume) {
            consumeIngredients(player, recipe);
        }

        ItemStack reward = plugin.getAltarManager().getReward(altarId);

        boolean ritual = plugin.getConfig().getBoolean("altars." + altarId + ".ritual", false);
        if (ritual) {
            Location ritualLoc = loc.clone().add(0.5, 0.0, 0.5);
            RitualManager.startRitual(player, reward, ritualLoc);
        } else {
            player.getInventory().addItem(reward);
        }

        loc.getWorld().strikeLightning(loc);
        plugin.getAltarManager().broadcastCraftMessage(player, reward, altarId);
        plugin.getAltarManager().setAltarUsed(loc, true);
        
        // Remove the altar block displays and active registration
        plugin.getAltarManager().removeAltar(loc);
    }

    private void consumeIngredients(Player player, AltarRecipe recipe) {
        PlayerInventory inv = player.getInventory();
        Map<AltarIngredient, Integer> remaining = new HashMap<>();
        for (AltarIngredient ing : recipe.getIngredients()) {
            remaining.put(ing, ing.getAmount());
        }

        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;

            for (Map.Entry<AltarIngredient, Integer> entry : remaining.entrySet()) {
                if (entry.getValue() <= 0) continue;

                AltarIngredient ing = entry.getKey();
                if (ing.matches(item)) {
                    int toTake = Math.min(entry.getValue(), item.getAmount());
                    item.setAmount(item.getAmount() - toTake);
                    entry.setValue(entry.getValue() - toTake);
                    
                    if (item.getAmount() <= 0) {
                        inv.setItem(i, null);
                    }
                    break;
                }
            }
        }
    }
}
