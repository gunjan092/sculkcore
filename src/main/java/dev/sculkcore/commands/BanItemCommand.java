package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.game.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BanItemCommand implements CommandExecutor, Listener {
    private final SculkCorePlugin plugin;
    private final String guiTitle = "Banned Items";

    public BanItemCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!").color(NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("sculkcore.banitem")) {
            player.sendMessage(Component.text("You do not have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        Material material = player.getInventory().getItemInMainHand().getType();
        if (material == Material.AIR) {
            player.sendMessage(Component.text("You must hold an item to ban it!").color(NamedTextColor.RED));
            return true;
        }

        if (material.toString().contains("POTION")) {
            player.sendMessage(Component.text("Potion banning settings can be managed in /settings.").color(NamedTextColor.RED));
            return true;
        }
        if (material == Material.ENCHANTED_BOOK) {
            player.sendMessage(Component.text("Enchantment banning settings can be managed in /settings.").color(NamedTextColor.RED));
            return true;
        }
        if (material == Material.TIPPED_ARROW) {
            player.sendMessage(Component.text("Arrow banning settings can be managed in /settings.").color(NamedTextColor.RED));
            return true;
        }

        List<String> bannedList = plugin.getConfig().getStringList("banned-items");
        if (bannedList.contains(material.name())) {
            player.sendMessage(Component.text(material.name() + " is already banned!").color(NamedTextColor.YELLOW));
            return true;
        }

        bannedList.add(material.name());
        plugin.getConfig().set("banned-items", bannedList);
        plugin.saveConfig();

        player.getInventory().setItemInMainHand(null);
        Bukkit.getServer().broadcast(Component.text(material.name() + " has been banned by " + player.getName() + "!").color(NamedTextColor.RED));
        return true;
    }

    private Set<Material> getBannedMaterials() {
        List<String> list = plugin.getConfig().getStringList("banned-items");
        Set<Material> banned = new HashSet<>();
        for (String s : list) {
            Material mat = Material.getMaterial(s);
            if (mat != null) {
                banned.add(mat);
            }
        }
        return banned;
    }

    private boolean isBanned(Material material) {
        return getBannedMaterials().contains(material);
    }

    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!plugin.getConfig().getBoolean("rules.ban_netherite", false)) {
            return;
        }

        ItemStack result = event.getResult();
        if (result != null && result.getType().name().contains("NETHERITE")) {
            event.setResult(null);
            HumanEntity human = event.getView().getPlayer();
            if (human instanceof Player player) {
                if (!GameState.isCooldownActive(player.getUniqueId(), "message")) {
                    player.sendMessage(Component.text("Netherite is not allowed").color(NamedTextColor.DARK_RED));
                    GameState.setCooldown(player, "message", 1.0);
                }
            }
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;
        if (result != null && isBanned(result.getType())) {
            event.getInventory().setResult(null);
            HumanEntity human = event.getView().getPlayer();
            if (human instanceof Player player) {
                player.sendMessage(Component.text("Crafting " + result.getType().name() + " is not allowed").color(NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onInventoryClickGeneral(InventoryClickEvent event) {
        // First check GUI click
        if (event.getView().getTitle().equals(guiTitle)) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                return;
            }

            if (clicked.getType() == Material.ARROW && clicked.getItemMeta() != null &&
                clicked.getItemMeta().getDisplayName().contains("GO BACK")) {
                SettingsCommand.b(player);
                return;
            }

            String matName = clicked.getType().name();
            List<String> list = plugin.getConfig().getStringList("banned-items");
            list.remove(matName);
            plugin.getConfig().set("banned-items", list);
            plugin.saveConfig();

            player.sendMessage(Component.text("Unbanned " + matName + "!").color(NamedTextColor.GREEN));
            openBannedItemsGUI(player);
            return;
        }

        // Cancel click on banned items in normal inventories
        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && isBanned(clicked.getType())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage(Component.text("You can't use " + clicked.getType().name() + " because it's banned.").color(NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkAndRemoveBannedItems(event.getPlayer());
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack item = event.getItem().getItemStack();
            if (isBanned(item.getType())) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You can't pick up " + item.getType().name() + " because it's banned.").color(NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && isBanned(item.getType())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.getInventory().remove(item);
            notifyRemovedBannedItem(item, player);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isBanned(item.getType())) {
            event.getItemDrop().remove();
            notifyRemovedBannedItem(item, event.getPlayer());
        }
    }

    public void checkAndRemoveBannedItems(Player player) {
        boolean changed = false;
        Set<Material> banned = getBannedMaterials();
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && banned.contains(item.getType())) {
                notifyRemovedBannedItem(item, player);
                player.getInventory().setItem(i, null);
                changed = true;
            }
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && banned.contains(offhand.getType())) {
            notifyRemovedBannedItem(offhand, player);
            player.getInventory().setItemInOffHand(null);
            changed = true;
        }

        if (changed) {
            player.updateInventory();
        }
    }

    private void notifyRemovedBannedItem(ItemStack item, Player player) {
        Bukkit.getServer().broadcast(Component.text(item.getType().name() + " was removed from " + player.getName() + " because it's banned on this server.").color(NamedTextColor.RED));
    }

    public void openBannedItemsGUI(Player player) {
        List<String> list = plugin.getConfig().getStringList("banned-items");
        Inventory gui = Bukkit.createInventory(null, 54, Component.text(guiTitle).color(NamedTextColor.RED));
        
        for (int i = 0; i < list.size() && i < 53; i++) {
            Material mat = Material.getMaterial(list.get(i));
            if (mat != null) {
                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text(mat.name()).color(NamedTextColor.RED));
                    item.setItemMeta(meta);
                }
                gui.setItem(i, item);
            }
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
}
