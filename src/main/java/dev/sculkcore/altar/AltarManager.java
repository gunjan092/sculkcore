package dev.sculkcore.altar;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Structure;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class AltarManager implements Listener {
    private final SculkCorePlugin plugin;
    public final NamespacedKey typeKey;
    public final NamespacedKey usedKey;
    public final Map<Location, AltarInstance> activeAltars = new HashMap<>();
    private BukkitRunnable rotationTask;

    public AltarManager(SculkCorePlugin plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "altar_type");
        this.usedKey = new NamespacedKey(plugin, "altar_used");
        this.loadActiveAltars();
        this.startRotationTask();
    }

    public boolean isAltarUsed(Location location) {
        BlockState blockState = location.getBlock().getState();
        if (blockState instanceof Structure) {
            Structure structure = (Structure) blockState;
            return structure.getPersistentDataContainer().getOrDefault(usedKey, PersistentDataType.BOOLEAN, false);
        }
        return false;
    }

    public void setAltarUsed(Location location, boolean used) {
        BlockState blockState = location.getBlock().getState();
        if (blockState instanceof Structure) {
            Structure structure = (Structure) blockState;
            structure.getPersistentDataContainer().set(usedKey, PersistentDataType.BOOLEAN, used);
            structure.update();
        }
    }

    public List<AltarIngredient> checkIngredients(Player player, AltarRecipe recipe) {
        List<AltarIngredient> missing = new ArrayList<>();
        for (AltarIngredient ingredient : recipe.getIngredients()) {
            int required = ingredient.getAmount();
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && ingredient.matches(item)) {
                    count += item.getAmount();
                }
            }
            if (count < required) {
                missing.add(new AltarIngredient(ingredient.getMaterial(), required - count, ingredient.getCustomModelData(), ingredient.getItemName()));
            }
        }
        return missing;
    }

    public void sendMissingItemsMessage(Player player, List<AltarIngredient> missing) {
        if (missing.isEmpty()) return;
        player.sendMessage(plugin.getConfigManager().translateColor("&c&lMissing items:"));
        for (AltarIngredient ingredient : missing) {
            String name = ingredient.getItemName();
            if (name == null || name.isEmpty()) {
                String rawName = ingredient.getMaterial().name().toLowerCase();
                name = Arrays.stream(rawName.split("_"))
                        .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                        .collect(Collectors.joining(" "));
            }
            player.sendMessage(plugin.getConfigManager().translateColor("&7- &7" + ingredient.getAmount() + "x &f" + name));
        }
    }

    public String getItemDisplayName(ItemStack itemStack) {
        if (itemStack == null) return "AIR";
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null && itemMeta.hasDisplayName()) {
            return itemMeta.getDisplayName();
        }
        String name = itemStack.getType().name().toLowerCase().replace("_", " ");
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public void broadcastCraftMessage(Player player, ItemStack itemStack, String altarId) {
        String displayName = getItemDisplayName(itemStack);
        String message = plugin.getConfig().getString("altars." + altarId + ".message", "&f&l<player> &6&lhas crafted the &e&l<item>&6!");
        message = message.replace("<player>", player.getName());
        message = message.replace("<item>", displayName);
        message = message.replace("<location>", player.getLocation().getBlockX() + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ());
        message = message.replace("<world>", player.getWorld().getName());

        Bukkit.broadcastMessage(plugin.getConfigManager().translateColor(message));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    public ItemStack getReward(String altarId) {
        ItemStack item = plugin.getConfig().getItemStack("altars." + altarId + ".reward");
        if (item == null) {
            item = new ItemStack(Material.NETHER_STAR);
            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta != null) {
                itemMeta.setDisplayName("CHECK MY LORE (Description)");
                itemMeta.setLore(Collections.singletonList(ChatColor.GRAY + "(/saltar set) While holding something in your hand"));
                item.setItemMeta(itemMeta);
            }
        }
        return item;
    }

    public void spawnItemDisplay(AltarInstance instance, Location location, String altarId) {
        double height = getHologramHeight(altarId);
        Location spawnLoc = location.clone().add(0.5, height, 0.5);
        ItemDisplay itemDisplay = location.getWorld().spawn(spawnLoc, ItemDisplay.class, display -> {
            display.setItemStack(getReward(altarId));
            display.setInvulnerable(true);
            display.setPersistent(false);
            Transformation trans = display.getTransformation();
            trans.getScale().set(1.0f);
            display.setTransformation(trans);
        });
        instance.setItemDisplay(itemDisplay);
    }

    public double getHologramHeight(String altarId) {
        AltarConfig cfg = plugin.getConfigManager().altars.get(altarId);
        if (cfg != null && cfg.getHologramHeight() > 0) {
            return cfg.getHologramHeight();
        }
        return 2.2; // default height
    }

    public void spawnHolograms(AltarInstance instance, Location location, String altarId) {
        AltarConfig cfg = plugin.getConfigManager().altars.get(altarId);
        if (cfg == null || cfg.getRecipes().isEmpty()) return;

        AltarRecipe recipe = cfg.getRecipes().values().iterator().next();
        double height = getHologramHeight(altarId);
        World world = location.getWorld();
        
        int offsetIndex = 0;
        for (AltarIngredient ingredient : recipe.getIngredients()) {
            Location lineLoc = location.clone().add(0.5, height + (double) offsetIndex * 0.25, 0.5);
            TextDisplay textDisplay = world.spawn(lineLoc, TextDisplay.class, display -> {
                display.setShadowed(false);
                display.setBillboard(Display.Billboard.CENTER);
                display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                display.setGravity(false);
                display.setInvulnerable(true);
                
                String name = ingredient.getItemName();
                if (name == null || name.isEmpty()) {
                    String rawName = ingredient.getMaterial().name().toLowerCase();
                    name = Arrays.stream(rawName.split("_"))
                            .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                            .collect(Collectors.joining(" "));
                }
                display.setText(ChatColor.GRAY + String.valueOf(ingredient.getAmount()) + "x " + ChatColor.WHITE + name);
            });
            instance.addTextDisplay(textDisplay);
            offsetIndex++;
        }

        ItemStack reward = getReward(altarId);
        String rewardName = getItemDisplayName(reward);

        Location titleLoc = location.clone().add(0.5, height - 0.25, 0.5);
        TextDisplay titleDisplay = world.spawn(titleLoc, TextDisplay.class, display -> {
            display.setShadowed(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            display.setGravity(false);
            display.setInvulnerable(true);
            display.setText(ChatColor.GOLD + rewardName);
        });
        instance.addTextDisplay(titleDisplay);
    }

    public void loadActiveAltars() {
        FileConfiguration fileConfiguration = plugin.getConfigManager().getAltarsConfig();
        if (!fileConfiguration.contains("altars")) {
            return;
        }
        ConfigurationSection section = fileConfiguration.getConfigurationSection("altars");
        if (section == null) return;
        
        for (String key : section.getKeys(false)) {
            String worldName = fileConfiguration.getString("altars." + key + ".world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            double x = fileConfiguration.getDouble("altars." + key + ".x");
            double y = fileConfiguration.getDouble("altars." + key + ".y");
            double z = fileConfiguration.getDouble("altars." + key + ".z");
            String type = fileConfiguration.getString("altars." + key + ".type");

            Location loc = new Location(world, x, y, z);
            if (activeAltars.containsKey(loc)) continue;

            loc.getChunk().load(true);
            AltarInstance instance = new AltarInstance(type, loc);
            spawnItemDisplay(instance, loc, type);
            spawnHolograms(instance, loc, type);
            activeAltars.put(loc, instance);
        }
    }

    public boolean createAltar(Location location, String type) {
        Block block = location.getBlock();
        String materialName = plugin.getConfig().getString("altarSettings.blocktype", "STRUCTURE_BLOCK");
        Material mat = Material.getMaterial(materialName.toUpperCase());
        if (mat == null) {
            mat = Material.STRUCTURE_BLOCK;
        }

        block.setType(mat);
        if (block.getType() == Material.STRUCTURE_BLOCK) {
            BlockState state = block.getState();
            if (state instanceof Structure) {
                Structure structure = (Structure) state;
                structure.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type);
                structure.update();
            }
        }
        
        block.getChunk().addPluginChunkTicket(plugin);
        
        AltarInstance instance = new AltarInstance(type, location);
        spawnItemDisplay(instance, location, type);
        spawnHolograms(instance, location, type);
        activeAltars.put(location, instance);
        
        saveAltarToConfig(location, type);
        return true;
    }

    public boolean removeAltar(Location location) {
        AltarInstance instance = activeAltars.remove(location);
        if (instance == null) {
            return false;
        }
        instance.clearDisplays();

        BlockState state = location.getBlock().getState();
        if (state instanceof Structure) {
            Structure structure = (Structure) state;
            structure.getPersistentDataContainer().remove(typeKey);
            structure.getPersistentDataContainer().remove(usedKey);
            structure.update();
        }

        removeAltarFromConfig(location);
        return true;
    }

    private void saveAltarToConfig(Location location, String type) {
        FileConfiguration fileConfiguration = plugin.getConfigManager().getAltarsConfig();
        String key = location.getWorld().getName() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        fileConfiguration.set("altars." + key + ".world", location.getWorld().getName());
        fileConfiguration.set("altars." + key + ".x", location.getX());
        fileConfiguration.set("altars." + key + ".y", location.getY());
        fileConfiguration.set("altars." + key + ".z", location.getZ());
        fileConfiguration.set("altars." + key + ".type", type);
        plugin.getConfigManager().saveAltarsConfig();
    }

    private void removeAltarFromConfig(Location location) {
        FileConfiguration fileConfiguration = plugin.getConfigManager().getAltarsConfig();
        String key = location.getWorld().getName() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
        fileConfiguration.set("altars." + key, null);
        plugin.getConfigManager().saveAltarsConfig();
    }

    public void cleanUpDisplays() {
        activeAltars.values().forEach(AltarInstance::clearDisplays);
        activeAltars.clear();
    }

    private void startRotationTask() {
        rotationTask = new BukkitRunnable() {
            private float rotation = 0.0f;

            @Override
            public void run() {
                int speed = plugin.getConfig().getInt("altarSettings.rotationSpeed", 2);
                rotation += (float) Math.toRadians(speed);
                if (rotation >= Math.PI * 2) {
                    rotation = 0.0f;
                }

                for (AltarInstance instance : activeAltars.values()) {
                    ItemDisplay display = instance.getItemDisplay();
                    if (display == null || display.isDead()) continue;

                    Transformation trans = display.getTransformation();
                    trans.getLeftRotation().set(new AxisAngle4f(rotation, 0.0f, 1.0f, 0.0f));
                    display.setInterpolationDelay(0);
                    display.setInterpolationDuration(5);
                    display.setTeleportDuration(5);
                    display.setTransformation(trans);
                }
            }
        };
        rotationTask.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onAltarBreak(BlockBreakEvent event) {
        String materialName = plugin.getConfig().getString("altarSettings.blocktype", "STRUCTURE_BLOCK");
        Material mat = Material.getMaterial(materialName.toUpperCase());
        if (mat == null) {
            mat = Material.STRUCTURE_BLOCK;
        }

        if (event.getBlock().getType() != mat) {
            return;
        }

        Location loc = event.getBlock().getLocation();
        if (activeAltars.containsKey(loc)) {
            removeAltar(loc);
        }
    }

    @EventHandler
    public void onAltarExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> activeAltars.containsKey(block.getLocation()));
    }

    @EventHandler
    public void onAltarExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> activeAltars.containsKey(block.getLocation()));
    }
}
