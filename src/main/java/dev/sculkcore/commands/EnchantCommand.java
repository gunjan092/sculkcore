package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EnchantCommand implements CommandExecutor, TabCompleter {
    private final SculkCorePlugin plugin;

    public EnchantCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /enchant <player> <enchant> <level|remove> [hand|armor]");
            return true;
        }

        boolean allPlayers = false;
        Player target = null;

        if (args[0].equals("@a")) {
            allPlayers = true;
        } else if (args[0].equals("@s")) {
            if (sender instanceof Player playerSender) {
                target = playerSender;
            } else {
                sender.sendMessage("§cYou are not a player");
                return true;
            }
        } else {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }
        }

        // Get enchantment
        String enchantStr = args[1].toLowerCase();
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantStr));
        if (enchantment == null) {
            sender.sendMessage(ChatColor.RED + "Unknown enchantment: " + args[1]);
            return true;
        }

        boolean isRemove = false;
        int level = enchantment.getMaxLevel();

        if (args.length >= 3) {
            if (args[2].equalsIgnoreCase("remove")) {
                isRemove = true;
            } else {
                try {
                    level = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Level must be a number or 'remove'!");
                    return true;
                }
            }
        }

        String targetSlot = args.length >= 4 ? args[3].toLowerCase() : "hand";
        if (!targetSlot.equals("hand") && !targetSlot.equals("armor")) {
            targetSlot = "hand";
        }

        Player senderPlayer = sender instanceof Player ? (Player) sender : null;

        if (allPlayers) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                applyEnchantment(onlinePlayer, enchantment, isRemove, targetSlot, level, senderPlayer);
            }
            sender.sendMessage("§aSuccess!");
        } else {
            applyEnchantment(target, enchantment, isRemove, targetSlot, level, senderPlayer);
            if (senderPlayer != null && senderPlayer != target) {
                sender.sendMessage("§aApplied modification to player " + target.getName());
            }
        }

        return true;
    }

    public static void applyEnchantment(Player target, Enchantment enchantment, boolean isRemove, String targetSlot, int level, @Nullable Player sender) {
        if (targetSlot.equalsIgnoreCase("hand")) {
            ItemStack item = target.getInventory().getItemInMainHand();
            if (item.getType().isAir()) {
                if (sender != null) {
                    sender.sendMessage(ChatColor.RED + target.getName() + " is not holding any item!");
                }
                return;
            }

            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            if (isRemove) {
                meta.removeEnchant(enchantment);
                item.setItemMeta(meta);
                target.sendMessage(ChatColor.GREEN + "Removed enchantment " + enchantment.getKey().getKey() + " from your held item.");
            } else {
                meta.addEnchant(enchantment, level, true);
                item.setItemMeta(meta);
                target.sendMessage(ChatColor.GREEN + "Added enchantment " + enchantment.getKey().getKey() + " (Level " + level + ") to your held item.");
            }
        } else if (targetSlot.equalsIgnoreCase("armor")) {
            ItemStack[] armor = target.getInventory().getArmorContents();
            boolean altered = false;

            for (ItemStack item : armor) {
                if (item != null && !item.getType().isAir()) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        if (isRemove) {
                            meta.removeEnchant(enchantment);
                        } else {
                            meta.addEnchant(enchantment, level, true);
                        }
                        item.setItemMeta(meta);
                        altered = true;
                    }
                }
            }

            if (altered) {
                target.getInventory().setArmorContents(armor);
                if (isRemove) {
                    target.sendMessage(ChatColor.GREEN + "Removed enchantment " + enchantment.getKey().getKey() + " from all your armor.");
                } else {
                    target.sendMessage(ChatColor.GREEN + "Added enchantment " + enchantment.getKey().getKey() + " (Level " + level + ") to all your armor.");
                }
            }
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            players.add("@a");
            players.add("@s");
            for (String p : players) {
                if (p.toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(p);
                }
            }
        } else if (args.length == 2) {
            for (Enchantment enchantment : Enchantment.values()) {
                String name = enchantment.getKey().getKey();
                if (name.toLowerCase().startsWith(args[1].toLowerCase())) {
                    list.add(name);
                }
            }
        } else if (args.length == 3) {
            list.add("remove");
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(args[1].toLowerCase()));
            if (enchantment != null) {
                for (int i = enchantment.getStartLevel(); i <= enchantment.getMaxLevel(); i++) {
                    list.add(String.valueOf(i));
                }
            }
        } else if (args.length == 4) {
            if ("hand".startsWith(args[3].toLowerCase())) {
                list.add("hand");
            }
            if ("armor".startsWith(args[3].toLowerCase())) {
                list.add("armor");
            }
        }
        return list;
    }
}
