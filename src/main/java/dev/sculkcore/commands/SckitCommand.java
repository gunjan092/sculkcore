package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SckitCommand implements CommandExecutor, TabCompleter, Listener {
    private final SculkCorePlugin plugin;

    public SckitCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sender.sendMessage("§cYou didn't specify a kit name or subcommand.");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        if (subcommand.equals("resetplayers")) {
            plugin.getConfig().set("players", null);
            plugin.saveConfig();
            player.sendMessage("§aKit cleared and player data deleted successfully!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cYou didn't specify a kit name");
            return true;
        }

        String kitName = args[1];

        switch (subcommand) {
            case "save":
                saveKit(player, kitName);
                player.sendMessage("§aKit saved successfully!");
                break;

            case "join":
                if (kitName.equalsIgnoreCase("none")) {
                    plugin.getConfig().set("config.kit_on_join", "none");
                    plugin.saveConfig();
                    player.sendMessage("§cPlayers will no longer receive any kit when they first join");
                } else {
                    plugin.getConfig().set("config.kit_on_join", kitName);
                    plugin.saveConfig();
                    player.sendMessage("§aPlayers will now receive the §b" + kitName + " §akit when they first join");
                }
                break;

            case "view":
                viewKit(player, kitName);
                break;

            case "load":
                String targetName = args.length >= 3 ? args[2] : null;
                if (targetName != null && targetName.equalsIgnoreCase("@a")) {
                    for (Player target : Bukkit.getOnlinePlayers()) {
                        loadKit(target, kitName);
                    }
                    sender.sendMessage("§aKit loaded for all players!");
                } else {
                    Player target = targetName != null ? Bukkit.getPlayer(targetName) : player;
                    if (target != null) {
                        loadKit(target, kitName);
                        sender.sendMessage("§aKit loaded for " + target.getName() + "!");
                    } else {
                        sender.sendMessage("§cPlayer not found!");
                    }
                }
                break;

            case "clear":
                plugin.getConfig().set("kit." + kitName, null);
                plugin.saveConfig();
                player.sendMessage("§aKit cleared successfully!");
                break;

            default:
                sender.sendMessage("§cUnknown subcommand. Use save, load, clear, resetplayers, join, or view.");
                break;
        }

        return true;
    }

    private void saveKit(Player player, String kitName) {
        PlayerInventory inventory = player.getInventory();
        plugin.getConfig().set("kit." + kitName, null);
        ConfigurationSection kitSection = plugin.getConfig().createSection("kit." + kitName);
        
        ConfigurationSection mainSection = kitSection.createSection("main");
        ItemStack[] storage = inventory.getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            mainSection.set(Integer.toString(i), storage[i]);
        }

        ConfigurationSection armorSection = kitSection.createSection("armor");
        ItemStack[] armor = inventory.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            armorSection.set(Integer.toString(i), armor[i]);
        }

        ConfigurationSection offhandSection = kitSection.createSection("offhand");
        offhandSection.set("0", inventory.getItemInOffHand());

        plugin.saveConfig();
    }

    private void viewKit(Player player, String kitName) {
        ConfigurationSection kitSection = plugin.getConfig().getConfigurationSection("kit." + kitName);
        if (kitSection == null) {
            player.sendMessage("§cKit not found!");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacySection().deserialize("§bSMP Core Kit: §f" + kitName));
        ItemStack divider = createDivider();

        // 1. Populate main inventory contents
        ConfigurationSection mainSection = kitSection.getConfigurationSection("main");
        if (mainSection != null) {
            // Hotbar (slots 0-8 in PlayerInventory) mapped to GUI slots 45-53
            for (int i = 0; i < 9; i++) {
                ItemStack item = mainSection.getItemStack(Integer.toString(i));
                if (item != null) {
                    gui.setItem(i + 45, item);
                }
            }
            // Main storage (slots 9-35 in PlayerInventory) mapped to GUI slots 18-44
            for (int i = 9; i < 36; i++) {
                ItemStack item = mainSection.getItemStack(Integer.toString(i));
                if (item != null) {
                    gui.setItem(i + 9, item);
                }
            }
        }

        // 2. Populate armor contents
        ConfigurationSection armorSection = kitSection.getConfigurationSection("armor");
        if (armorSection != null) {
            int[] armorSlots = {3, 4, 5, 6};
            for (int i = 0; i < 4; i++) {
                ItemStack item = armorSection.getItemStack(Integer.toString(i));
                if (item != null) {
                    gui.setItem(armorSlots[i], item);
                }
            }
        }

        // 3. Populate offhand item
        ConfigurationSection offhandSection = kitSection.getConfigurationSection("offhand");
        if (offhandSection != null) {
            ItemStack offhandItem = offhandSection.getItemStack("0");
            if (offhandItem != null) {
                gui.setItem(2, offhandItem);
            }
        }

        // 4. Fill divider panels
        for (int i = 9; i < 18; i++) {
            gui.setItem(i, divider);
        }
        gui.setItem(0, divider);
        gui.setItem(1, divider);
        gui.setItem(7, divider);
        gui.setItem(8, divider);

        player.openInventory(gui);
    }

    private void loadKit(Player player, String kitName) {
        ConfigurationSection kitSection = plugin.getConfig().getConfigurationSection("kit." + kitName);
        if (kitSection == null) {
            return;
        }

        PlayerInventory inventory = player.getInventory();

        ConfigurationSection mainSection = kitSection.getConfigurationSection("main");
        if (mainSection != null) {
            ItemStack[] storage = new ItemStack[36];
            for (int i = 0; i < 36; i++) {
                storage[i] = mainSection.getItemStack(Integer.toString(i));
            }
            inventory.setStorageContents(storage);
        }

        ConfigurationSection armorSection = kitSection.getConfigurationSection("armor");
        if (armorSection != null) {
            ItemStack[] armor = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                armor[i] = armorSection.getItemStack(Integer.toString(i));
            }
            inventory.setArmorContents(armor);
        }

        ConfigurationSection offhandSection = kitSection.getConfigurationSection("offhand");
        if (offhandSection != null) {
            inventory.setItemInOffHand(offhandSection.getItemStack("0"));
        }
    }

    private ItemStack createDivider() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setHideTooltip(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().contains("§bSMP Core Kit:")) {
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                if (event.getAction() != InventoryAction.CLONE_STACK) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<String> playersReceived = plugin.getConfig().getStringList("players");
        String uuidStr = player.getUniqueId().toString();
        String joinKit = plugin.getConfig().getString("config.kit_on_join", "none");

        if (joinKit.equalsIgnoreCase("none")) {
            return;
        }

        if (playersReceived.contains(uuidStr) || !plugin.getConfig().contains("kit." + joinKit)) {
            return;
        }

        loadKit(player, joinKit);
        playersReceived.add(uuidStr);
        plugin.getConfig().set("players", playersReceived);
        plugin.saveConfig();
        player.sendMessage("§aYou received the §b" + joinKit + " §akit");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("save", "load", "clear", "resetplayers", "join", "view");
        }

        if (args.length == 2) {
            ConfigurationSection kitSection = plugin.getConfig().getConfigurationSection("kit");
            List<String> kits = kitSection != null ? new ArrayList<>(kitSection.getKeys(false)) : new ArrayList<>();
            if (args[0].equalsIgnoreCase("join")) {
                kits.add(0, "none");
            }
            return kits;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("load")) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("@a");
            String search = args[2].toLowerCase();
            Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(search))
                    .sorted()
                    .forEach(suggestions::add);
            return suggestions;
        }

        return List.of();
    }
}
