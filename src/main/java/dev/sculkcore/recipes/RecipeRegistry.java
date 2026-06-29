package dev.sculkcore.recipes;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeRegistry {

    public static void registerAll(SculkCorePlugin plugin) {
        // 1. Register Golden Apple (8 Gold Ingots + 1 Apple)
        try {
            NamespacedKey keyGapple = new NamespacedKey(plugin, "custom_golden_apple");
            if (Bukkit.getRecipe(keyGapple) == null) {
                ShapedRecipe recipeGapple = new ShapedRecipe(keyGapple, new ItemStack(Material.GOLDEN_APPLE));
                recipeGapple.shape(" G ", "GAG", " G ");
                recipeGapple.setIngredient('G', Material.GOLD_INGOT);
                recipeGapple.setIngredient('A', Material.APPLE);
                Bukkit.addRecipe(recipeGapple);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register custom Golden Apple recipe: " + e.getMessage());
        }

        // 2. Register Cobweb (5 String in an X shape)
        try {
            NamespacedKey keyCobweb = new NamespacedKey(plugin, "custom_cobweb");
            if (Bukkit.getRecipe(keyCobweb) == null) {
                ShapedRecipe recipeCobweb = new ShapedRecipe(keyCobweb, new ItemStack(Material.COBWEB));
                recipeCobweb.shape("S S", " S ", "S S");
                recipeCobweb.setIngredient('S', Material.STRING);
                Bukkit.addRecipe(recipeCobweb);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register custom Cobweb recipe: " + e.getMessage());
        }

        // 3. Register dynamic custom recipes from config
        try {
            List<?> craftItems = plugin.getConfig().getList("custom_craft_items");
            if (craftItems != null) {
                for (int i = 0; i < 54; i++) {
                    List<?> ingredientsList = plugin.getConfig().getList("custom_recipes." + i);
                    if (ingredientsList == null || ingredientsList.isEmpty() || craftItems.size() <= i) {
                        continue;
                    }

                    Object resultObj = craftItems.get(i);
                    ItemStack result = null;
                    if (resultObj instanceof ItemStack is) {
                        result = is;
                    } else if (resultObj instanceof Map) {
                        //noinspection unchecked
                        result = ItemStack.deserialize((Map<String, Object>) resultObj);
                    }

                    if (result == null) continue;

                    NamespacedKey key = new NamespacedKey(plugin, "custom_recipe_" + i);
                    if (Bukkit.getRecipe(key) != null) {
                        Bukkit.removeRecipe(key);
                    }

                    ShapedRecipe shapedRecipe = new ShapedRecipe(key, result);
                    shapedRecipe.shape("ABC", "DEF", "GHI");
                    char[] keys = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};

                    boolean hasIngredients = false;
                    for (int j = 0; j < 9 && j < ingredientsList.size(); j++) {
                        Object ingObj = ingredientsList.get(j);
                        ItemStack ing = null;
                        if (ingObj instanceof ItemStack is) {
                            ing = is;
                        } else if (ingObj instanceof Map) {
                            //noinspection unchecked
                            ing = ItemStack.deserialize((Map<String, Object>) ingObj);
                        }

                        if (ing != null && ing.getType() != Material.AIR) {
                            if (ing.hasItemMeta() && ing.getItemMeta().hasCustomModelData()) {
                                shapedRecipe.setIngredient(keys[j], new RecipeChoice.ExactChoice(ing));
                            } else {
                                shapedRecipe.setIngredient(keys[j], ing.getType());
                            }
                            hasIngredients = true;
                        }
                    }

                    if (hasIngredients) {
                        Bukkit.addRecipe(shapedRecipe);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load dynamic custom recipes: " + e.getMessage());
        }
    }
}
