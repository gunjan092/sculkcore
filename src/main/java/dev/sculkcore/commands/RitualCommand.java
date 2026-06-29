package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.altar.RitualManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RitualCommand implements CommandExecutor {
    private final SculkCorePlugin plugin;

    public RitualCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can run this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("sculkcore.ritual") && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "You do not have permission to start a ritual.");
            return true;
        }

        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack == null || itemStack.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You're not holding any item!");
            return true;
        }

        Location location = player.getLocation();
        RitualManager.startRitual(player, itemStack, location);
        return true;
    }
}
