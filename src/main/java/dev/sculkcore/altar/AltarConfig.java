package dev.sculkcore.altar;

import java.util.List;
import java.util.Map;

public class AltarConfig {
    private final String id;
    private final String name;
    private final List<String> hologram;
    private final Map<String, AltarRecipe> recipes;
    private final double hologramHeight;

    public AltarConfig(String id, String name, List<String> hologram, Map<String, AltarRecipe> recipes, double hologramHeight) {
        this.id = id;
        this.name = name;
        this.hologram = hologram;
        this.recipes = recipes;
        this.hologramHeight = hologramHeight;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getHologram() {
        return hologram;
    }

    public Map<String, AltarRecipe> getRecipes() {
        return recipes;
    }

    public double getHologramHeight() {
        return hologramHeight;
    }
}
