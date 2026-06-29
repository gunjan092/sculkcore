package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SbroadcastCommand implements CommandExecutor {
    private final SculkCorePlugin plugin;

    public SbroadcastCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("sculkcore.sbroadcast")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cYou need to add a message");
            return true;
        }

        String message = String.join(" ", args);
        String template = plugin.getConfig().getString("rules.broadcast_message", "§4[📢 ʙʀᴏᴀᴅᴄᴀsᴛ] <message>");
        String formatted = template.replace("<message>", message);
        
        String colored = (plugin.getConfigManager() != null)
                ? plugin.getConfigManager().translateColor(formatted)
                : ChatColor.translateAlternateColorCodes('&', formatted);

        Bukkit.broadcast(Component.text(colored));
        return true;
    }
}
