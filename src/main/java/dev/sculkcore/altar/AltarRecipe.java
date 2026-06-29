package dev.sculkcore.altar;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class AltarRecipe {
    private final String id;
    private final ItemStack result;
    private final List<AltarIngredient> ingredients;

    public AltarRecipe(String id, ItemStack result, List<AltarIngredient> ingredients) {
        this.id = id;
        this.result = result;
        this.ingredients = ingredients;
    }

    public String getId() {
        return id;
    }

    public ItemStack getResult() {
        return result;
    }

    public List<AltarIngredient> getIngredients() {
        return new ArrayList<>(ingredients);
    }
}
