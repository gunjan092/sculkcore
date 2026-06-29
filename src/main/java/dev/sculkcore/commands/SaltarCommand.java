package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.altar.AltarManager;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SaltarCommand implements CommandExecutor, TabCompleter {
    private final SculkCorePlugin plugin;

    public SaltarCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /saltar <spawn|setitem|create|delete|toggle> <type>");
            return true;
        }

        String sub = args[0].toLowerCase();
        String type = args[1].toLowerCase();
        AltarManager altarManager = plugin.getAltarManager();

        // Check if type exists (except for create and toggle)
        if (!plugin.getConfigManager().altars.containsKey(type) && !sub.equals("create") && !sub.equals("toggle")) {
            player.sendMessage(ChatColor.RED + "Unknown altar type: " + type);
            return true;
        }

        switch (sub) {
            case "spawn":
                if (altarManager.createAltar(player.getLocation().getBlock().getLocation(), type)) {
                    player.sendMessage(ChatColor.GREEN + "Altar spawned!");
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to spawn altar!");
                }
                break;

            case "setitem":
                ItemStack handItem = player.getInventory().getItemInMainHand();
                if (handItem == null || handItem.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + "You need to be holding an item");
                    return true;
                }
                plugin.getConfig().set("altars." + type + ".reward", handItem);
                plugin.saveConfig();
                reloadAltarConfigs();
                player.sendMessage(ChatColor.GREEN + "Set reward item for altar " + type);
                break;

            case "create":
                ItemStack createItem = player.getInventory().getItemInMainHand();
                if (createItem == null || createItem.getType().isAir()) {
                    player.sendMessage(ChatColor.RED + "You need to be holding an item");
                    return true;
                }
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f, 1.0f);
                
                plugin.getConfig().set("altars." + type + ".reward", createItem);
                plugin.getConfig().set("altars." + type + ".name", type);
                plugin.getConfig().set("altars." + type + ".hologram-height", 2.5);
                plugin.getConfig().set("altars." + type + ".item_height", 1.75);
                plugin.getConfig().set("altars." + type + ".message", "&f&l<player> &6&lhas crafted the &e&l<item>&6!");
                plugin.getConfig().set("altars." + type + ".consume_materials", true);
                plugin.getConfig().set("altars." + type + ".enabled", true);
                
                plugin.saveConfig();
                reloadAltarConfigs();
                player.sendMessage(ChatColor.GREEN + "Created " + type + ", Use /saltar spawn to spawn it");
                break;

            case "delete":
                // Despawn any active altars of this type
                altarManager.cleanUpDisplays(); // Despawn active displays
                plugin.getConfig().set("altars." + type, null);
                plugin.saveConfig();
                reloadAltarConfigs();
                player.sendMessage(ChatColor.RED + "Deleted " + type);
                break;

            case "toggle":
                boolean enabled = plugin.getConfig().getBoolean("altars." + type + ".enabled", true);
                plugin.getConfig().set("altars." + type + ".enabled", !enabled);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f, 1.0f);
                player.sendMessage(ChatColor.GRAY + "Toggled " + type + " to " + (!enabled));
                plugin.saveConfig();
                reloadAltarConfigs();
                break;

            case "edit":
                player.sendMessage(ChatColor.YELLOW + "GUI Editing for " + type + " is coming soon!");
                break;

            default:
                player.sendMessage(ChatColor.RED + "Usage: /saltar <spawn|setitem|create|delete|toggle> <type>");
                break;
        }

        return true;
    }

    private void reloadAltarConfigs() {
        plugin.getAltarManager().cleanUpDisplays(); // despawns active
        plugin.getAltarManager().loadActiveAltars(); // respawns displays
        plugin.getConfigManager().reload(); // reloads config maps and plugin config
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.addAll(Arrays.asList("spawn", "setitem", "create", "delete", "toggle", "edit"));
        } else if (args.length == 2) {
            suggestions.addAll(plugin.getConfigManager().altars.keySet());
        }
        return suggestions;
    }
}
