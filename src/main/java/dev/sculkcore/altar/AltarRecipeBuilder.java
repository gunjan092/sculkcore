package dev.sculkcore.altar;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AltarRecipeBuilder {
    private final String id;
    private Material resultMaterial;
    private String resultName;
    private final List<String> resultLore = new ArrayList<>();
    private final Map<Enchantment, Integer> resultEnchants = new HashMap<>();
    private int resultCustomModelData = 0;
    private final List<AltarIngredient> ingredients = new ArrayList<>();

    public AltarRecipeBuilder(String id) {
        this.id = id;
    }

    public AltarRecipeBuilder setResultMaterial(Material material) {
        this.resultMaterial = material;
        return this;
    }

    public AltarRecipeBuilder setResultName(String resultName) {
        this.resultName = resultName;
        return this;
    }

    public AltarRecipeBuilder addLoreLine(String line) {
        this.resultLore.add(line);
        return this;
    }

    public AltarRecipeBuilder addEnchantment(Enchantment enchantment, int level) {
        this.resultEnchants.put(enchantment, level);
        return this;
    }

    public AltarRecipeBuilder setResultCustomModelData(int customModelData) {
        this.resultCustomModelData = customModelData;
        return this;
    }

    public AltarRecipeBuilder addIngredient(Material material, int amount, int customModelData, String itemName) {
        this.ingredients.add(new AltarIngredient(material, amount, customModelData, itemName));
        return this;
    }

    public AltarRecipe build() {
        ItemStack itemStack = new ItemStack(resultMaterial);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            if (resultName != null) {
                itemMeta.setDisplayName(resultName);
            }
            if (!resultLore.isEmpty()) {
                itemMeta.setLore(resultLore);
            }
            if (resultCustomModelData > 0) {
                itemMeta.setCustomModelData(resultCustomModelData);
            }
            itemStack.setItemMeta(itemMeta);
        }
        for (Map.Entry<Enchantment, Integer> entry : resultEnchants.entrySet()) {
            itemStack.addUnsafeEnchantment(entry.getKey(), entry.getValue());
        }
        return new AltarRecipe(id, itemStack, ingredients);
    }
}
