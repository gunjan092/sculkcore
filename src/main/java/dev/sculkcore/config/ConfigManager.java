package dev.sculkcore.config;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.altar.AltarConfig;
import dev.sculkcore.altar.AltarIngredient;
import dev.sculkcore.altar.AltarRecipe;
import dev.sculkcore.altar.AltarRecipeBuilder;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    private final SculkCorePlugin plugin;
    public final Map<String, AltarConfig> altars = new HashMap<>();
    public static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private File altarsFile;
    private FileConfiguration altarsConfig;

    public ConfigManager(SculkCorePlugin plugin) {
        this.plugin = plugin;
        this.loadAltarsConfig();
        this.loadAltars();
    }

    private void loadAltarsConfig() {
        altarsFile = new File(plugin.getDataFolder(), "altars.yml");
        if (!altarsFile.exists()) {
            altarsFile.getParentFile().mkdirs();
            plugin.saveResource("altars.yml", false);
        }
        altarsConfig = YamlConfiguration.loadConfiguration(altarsFile);
    }

    public FileConfiguration getAltarsConfig() {
        return altarsConfig;
    }

    public void saveAltarsConfig() {
        try {
            altarsConfig.save(altarsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save altars.yml: " + e.getMessage());
        }
    }

    public String translateColor(String input) {
        if (input == null) return null;
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer(input.length() + 32);
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(sb, ChatColor.of("#" + hexCode).toString());
        }
        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(sb).toString());
    }

    public void loadAltars() {
        altars.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("altars");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection altarSec = section.getConfigurationSection(id);
            if (altarSec == null) continue;
            
            AltarConfig config = parseAltarConfig(id, altarSec);
            if (config != null) {
                altars.put(id, config);
            }
        }
    }

    private AltarConfig parseAltarConfig(String id, ConfigurationSection section) {
        String name = translateColor(section.getString("name", "&fAltar"));
        double hologramHeight = section.getDouble("hologram-height", -1.0);
        
        List<String> hologram = new ArrayList<>();
        if (section.contains("hologram")) {
            for (String line : section.getStringList("hologram")) {
                hologram.add(translateColor(line));
            }
        }
        
        Map<String, AltarRecipe> recipes = new HashMap<>();
        ConfigurationSection recipeSection = section.getConfigurationSection("recipe");
        if (recipeSection != null) {
            AltarRecipe recipe = parseRecipe("recipe", recipeSection);
            if (recipe != null) {
                recipes.put("recipe", recipe);
            }
        }
        return new AltarConfig(id, name, hologram, recipes, hologramHeight);
    }

    private AltarRecipe parseRecipe(String id, ConfigurationSection section) {
        // Default to STONE if material missing, matching original plugin behavior
        String materialStr = section.getString("material", "STONE");
        Material material = Material.getMaterial(materialStr.toUpperCase());
        if (material == null) {
            material = Material.STONE;
        }

        AltarRecipeBuilder builder = new AltarRecipeBuilder(id).setResultMaterial(material);
        builder.setResultName(translateColor(section.getString("item_name")));
        
        if (section.contains("lore")) {
            for (String line : section.getStringList("lore")) {
                builder.addLoreLine(translateColor(line));
            }
        }
        
        builder.setResultCustomModelData(section.getInt("custom_model_data", 0));
        
        List<?> list = section.getList("ingredients");
        if (list != null) {
            for (Object item : list) {
                if (!(item instanceof Map)) continue;
                Map<?, ?> map = (Map<?, ?>) item;
                String ingMatStr = (String) map.get("material");
                if (ingMatStr == null) continue;
                Material ingMat = Material.getMaterial(ingMatStr.toUpperCase());
                if (ingMat == null) continue;
                
                int amount = map.containsKey("amount") ? (Integer) map.get("amount") : 1;
                int customModelData = map.containsKey("custom_model_data") ? (Integer) map.get("custom_model_data") : 0;
                String itemName = translateColor((String) map.get("item_name"));
                
                builder.addIngredient(ingMat, amount, customModelData, itemName);
            }
        }
        return builder.build();
    }

    public void reload() {
        plugin.reloadConfig();
        loadAltarsConfig();
        loadAltars();
    }
}
