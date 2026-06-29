package dev.sculkcore.altar;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AltarIngredient {
    private final Material material;
    private final int amount;
    private final int customModelData;
    private final String itemName;

    public AltarIngredient(Material material, int amount, int customModelData, String itemName) {
        this.material = material;
        this.amount = amount;
        this.customModelData = customModelData;
        this.itemName = itemName;
    }

    public String getItemName() {
        return itemName;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean hasCustomModelData() {
        return customModelData > 0;
    }

    public boolean matches(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != material) {
            return false;
        }
        if (hasCustomModelData()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta == null || !itemMeta.hasCustomModelData()) {
                return false;
            }
            return itemMeta.getCustomModelData() == customModelData;
        }
        return true;
    }
}
