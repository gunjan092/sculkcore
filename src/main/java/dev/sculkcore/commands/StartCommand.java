package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.game.GameState;
import dev.sculkcore.tasks.GraceTask;
import dev.sculkcore.tasks.LaunchTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

public class StartCommand implements CommandExecutor {
    private final SculkCorePlugin plugin;

    public StartCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sculkcore.start")) {
            sender.sendMessage(plugin.getConfigManager().translateColor("&cYou do not have permission to run this command."));
            return true;
        }

        boolean launch = plugin.getConfig().getBoolean("config.launch", false);
        int launchStrength = plugin.getConfig().getInt("config.launch_strength", 4);
        int graceDuration = plugin.getConfig().getInt("config.grace_duration", 40);
        int startBorderSize = plugin.getConfig().getInt("config.start_border_size", 40);
        int startBorderSpeed = plugin.getConfig().getInt("config.start_border_speed", 60);
        
        List<String> startCommands = plugin.getConfig().getStringList("config.start_commands");
        for (String cmd : startCommands) {
            String processed = cmd.replace("<sender>", sender.getName());
            Bukkit.dispatchCommand(sender, processed);
        }

        String title = plugin.getConfig().getString("config.start_smp_title", "&aGRACE").replace("%time%", String.valueOf(graceDuration));
        String subtitle = plugin.getConfig().getString("config.start_smp_subtitle", "&aGRACE").replace("%time%", String.valueOf(graceDuration));
        String msg = plugin.getConfig().getString("config.start_smp_message", "");

        GameState.setGlobalCooldown("grace", graceDuration * 60);
        new GraceTask(plugin).runTaskTimer(plugin, 0L, 10L);

        String coloredTitle = plugin.getConfigManager().translateColor(title);
        String coloredSubtitle = plugin.getConfigManager().translateColor(subtitle);
        String coloredMsg = msg.isEmpty() ? "" : plugin.getConfigManager().translateColor(msg);

        int steakAmount = plugin.getConfig().getInt("config.start_steak", 0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (steakAmount > 0) {
                player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, steakAmount));
            }
            if (!coloredMsg.isEmpty()) {
                player.sendMessage(coloredMsg);
            }
            if (launch) {
                player.setVelocity(new Vector(0, launchStrength, 0));
                new LaunchTask(player).runTaskTimer(plugin, 5L, 1L);
            }
            player.getWorld().getWorldBorder().setSize(startBorderSize, startBorderSpeed);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
            player.sendTitle(coloredTitle, coloredSubtitle, 10, 70, 20);
        }
        return true;
    }
}
