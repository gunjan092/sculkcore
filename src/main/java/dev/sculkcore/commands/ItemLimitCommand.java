package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemLimitCommand implements CommandExecutor, Listener {
    private final SculkCorePlugin plugin;
    private final Map<String, Integer> limits = new HashMap<>();
    private final File limitsFile;
    private FileConfiguration limitsConfig;
    private final String guiTitle = ChatColor.RED + "Item Limits";

    public ItemLimitCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
        this.limitsFile = new File(plugin.getDataFolder(), "item_limits.yml");
        loadLimits();
        startContinuousCheck();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /itemlimit <amount>");
            return false;
        }

        int limit;
        try {
            limit = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number. Please enter a valid limit.");
            return false;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must be holding an item to set its limit.");
            return false;
        }

        setItemLimit(player, item, limit);
        return true;
    }

    private void setItemLimit(Player player, ItemStack item, int amount) {
        String key = getItemKey(item);
        String name = getItemDisplayName(item);
        
        limits.put(key, amount);
        limitsConfig.set(key, amount);
        saveLimitsConfig();
        
        player.sendMessage(ChatColor.GREEN + "The limit for " + name + " has been set to " + amount + ".");
    }

    private String getItemKey(ItemStack item) {
        Material material = item.getType();
        if (isPotion(material)) {
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (meta != null && meta.getBasePotionType() != null) {
                // Safely get base potion type key
                return "POTION|" + meta.getBasePotionType().getKey().toString();
            }
        }
        return material.name();
    }

    private String getItemDisplayName(ItemStack item) {
        Material material = item.getType();
        if (isPotion(material)) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
            PotionMeta potionMeta = (PotionMeta) meta;
            if (potionMeta != null && potionMeta.getBasePotionType() != null) {
                // Get displayable potion name
                String name = potionMeta.getBasePotionType().getKey().getKey().replace("_", " ");
                return "Potion of " + capitalize(name);
            }
            return "Potion";
        }
        return material.name();
    }

    private boolean isPotion(Material material) {
        return material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void loadLimits() {
        if (!limitsFile.exists()) {
            try {
                plugin.saveResource("item_limits.yml", false);
            } catch (Exception e) {
                // ignore or create empty file
                try {
                    limitsFile.createNewFile();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        limitsConfig = YamlConfiguration.loadConfiguration(limitsFile);
        limits.clear();
        for (String key : limitsConfig.getKeys(false)) {
            limits.put(key, limitsConfig.getInt(key));
        }
    }

    private void saveLimitsConfig() {
        try {
            limitsConfig.save(limitsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startContinuousCheck() {
        boolean continuous = plugin.getConfig().getBoolean("heavy.continuous_item_limit", true);
        int interval = plugin.getConfig().getInt("heavy.item_limit_check_interval", 20);
        if (!continuous) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkPlayerLimits(player);
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private void checkPlayerLimits(Player player) {
        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            String key = entry.getKey();
            int limit = entry.getValue();
            int count = getPlayerItemCount(player, key);
            if (count > limit) {
                int excess = count - limit;
                dropExcessItems(player, key, excess);
                String display = formatItemKeyName(key);
                player.sendMessage(ChatColor.RED + "You are only allowed to carry " + limit + " of " + display + ".");
            }
        }
    }

    private int getPlayerItemCount(Player player, String key) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && getItemKey(item).equals(key)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void dropExcessItems(Player player, String key, int amountToDrop) {
        if (GameState.isCooldownActive(player.getUniqueId(), "drop")) {
            return;
        }
        GameState.setCooldown(player, "drop", 1.0);

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || !getItemKey(item).equals(key)) {
                continue;
            }

            int currentAmount = item.getAmount();
            if (item.getMaxStackSize() == 1) {
                player.getWorld().dropItem(player.getLocation(), item.clone());
                player.getInventory().setItem(i, null);
                amountToDrop--;
            } else {
                if (currentAmount > amountToDrop) {
                    item.setAmount(currentAmount - amountToDrop);
                    ItemStack dropItem = item.clone();
                    dropItem.setAmount(amountToDrop);
                    player.getWorld().dropItem(player.getLocation(), dropItem);
                    amountToDrop = 0;
                } else {
                    player.getWorld().dropItem(player.getLocation(), item.clone());
                    player.getInventory().setItem(i, null);
                    amountToDrop -= currentAmount;
                }
            }

            if (amountToDrop <= 0) {
                break;
            }
        }
        player.updateInventory();
    }

    private String formatItemKeyName(String key) {
        if (key.contains("|")) {
            String[] parts = key.split("\\|");
            if (parts.length > 1) {
                String typeName = parts[1].replace("minecraft:", "").replace("_", " ");
                return "Potion of " + capitalize(typeName);
            }
        }
        return key;
    }

    @EventHandler
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        String key = getItemKey(item);
        if (!limits.containsKey(key)) {
            return;
        }

        int limit = limits.get(key);
        int count = getPlayerItemCount(player, key);
        int excess = count + item.getAmount() - limit;
        if (excess <= 0) {
            return;
        }

        int allowed = item.getAmount() - excess;
        if (allowed > 0) {
            ItemStack allowedStack = item.clone();
            allowedStack.setAmount(allowed);
            item.setAmount(excess);
            event.getItem().setItemStack(item);
            player.getInventory().addItem(allowedStack);
        } else {
            event.setCancelled(true);
        }

        if (!GameState.isCooldownActive(player.getUniqueId(), "message")) {
            String display = getItemDisplayName(item);
            player.sendMessage(ChatColor.RED + "You can only pick up " + allowed + " " + display + " (limit: " + limit + ")");
            GameState.setCooldown(player, "message", 5.0);
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getRecipe().getResult();
        String key = getItemKey(result);
        if (!limits.containsKey(key)) {
            return;
        }

        int limit = limits.get(key);
        int count = getPlayerItemCount(player, key);
        if (count + result.getAmount() > limit) {
            event.setCancelled(true);
            String display = getItemDisplayName(result);
            player.sendMessage(ChatColor.RED + "You've reached the limit for " + display + " (limit: " + limit + ")");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) {
            return;
        }

        // Prevent putting limited items in bundle
        if (clicked.getType().toString().contains("BUNDLE")) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                String key = getItemKey(cursor);
                if (limits.containsKey(key)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        String clickedKey = getItemKey(clicked);
        if (limits.containsKey(clickedKey)) {
            InventoryAction action = event.getAction();
            if (action == InventoryAction.PICKUP_ALL_INTO_BUNDLE ||
                action == InventoryAction.PICKUP_FROM_BUNDLE ||
                action == InventoryAction.PICKUP_SOME_INTO_BUNDLE ||
                action == InventoryAction.PLACE_FROM_BUNDLE ||
                action == InventoryAction.PLACE_ALL_INTO_BUNDLE ||
                action == InventoryAction.PLACE_SOME_INTO_BUNDLE) {
                event.setCancelled(true);
                return;
            }
        }

        // Handle GUI interaction if title matches GUI
        if (event.getView().getTitle().equals(guiTitle)) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);

            // Check if Go Back arrow is clicked
            if (clicked.getType() == Material.ARROW && clicked.getItemMeta() != null &&
                clicked.getItemMeta().getDisplayName().contains("GO BACK")) {
                SettingsCommand.b(player);
                return;
            }

            // Find matching limit entry to delete
            String matchKey = null;
            for (String key : limits.keySet()) {
                ItemStack rep = getGUIItemRepresentation(key, limits.get(key));
                if (rep.isSimilar(clicked)) {
                    matchKey = key;
                    break;
                }
            }

            if (matchKey != null) {
                limitsConfig.set(matchKey, null);
                saveLimitsConfig();
                limits.remove(matchKey);
                player.sendMessage(ChatColor.GREEN + "Removed limit for " + formatItemKeyName(matchKey));
                openItemLimitsGUI(player);
            }
        }
    }

    public void openItemLimitsGUI(Player player) {
        loadLimits();
        Inventory gui = Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacySection().deserialize(guiTitle));
        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            gui.addItem(getGUIItemRepresentation(entry.getKey(), entry.getValue()));
        }
        
        ItemStack goBack = new ItemStack(Material.ARROW);
        ItemMeta meta = goBack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "GO BACK");
            goBack.setItemMeta(meta);
        }
        gui.setItem(53, goBack);
        
        player.openInventory(gui);
    }

    private ItemStack getGUIItemRepresentation(String key, int limit) {
        ItemStack item;
        if (key.startsWith("POTION|")) {
            String[] parts = key.split("\\|");
            String typeName = parts[1];
            item = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (meta != null) {
                try {
                    meta.setBasePotionType(PotionType.valueOf(typeName.toUpperCase().replace("MINECRAFT:", "")));
                } catch (IllegalArgumentException e) {
                    // Ignore, set default
                }
                meta.setDisplayName(ChatColor.YELLOW + "Potion of " + capitalize(typeName.replace("minecraft:", "").replace("_", " ")));
                meta.setLore(List.of(
                    ChatColor.GRAY + "Current Limit: " + limit,
                    ChatColor.RED + "Click to remove limit"
                ));
                item.setItemMeta(meta);
            }
        } else {
            Material mat = Material.getMaterial(key);
            if (mat == null) {
                mat = Material.BARRIER;
            }
            item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + mat.name());
                meta.setLore(List.of(
                    ChatColor.GRAY + "Current Limit: " + limit,
                    ChatColor.RED + "Click to remove limit"
                ));
                item.setItemMeta(meta);
            }
        }
        return item;
    }
}
