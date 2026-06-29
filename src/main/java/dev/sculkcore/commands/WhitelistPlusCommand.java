package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WhitelistPlusCommand implements CommandExecutor {
    private final SculkCorePlugin plugin;

    public WhitelistPlusCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("op")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /whitelistplus <add|remove>");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("add")) {
            FileConfiguration config = plugin.getConfig();
            List<String> list = config.getStringList("whitelist");
            if (list.isEmpty()) {
                sender.sendMessage("§cNo players found in whitelist.yml!");
                return true;
            }
            for (String name : list) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(name);
                op.setWhitelisted(true);
            }
            sender.sendMessage("§aWhitelisted all players from whitelist.yml!");
            Bukkit.setWhitelist(true);
            return true;
        } else if (sub.equals("remove")) {
            for (OfflinePlayer op : Bukkit.getWhitelistedPlayers()) {
                op.setWhitelisted(false);
            }
            sender.sendMessage("§cRemoved whitelist for all players in whitelist.yml!");
            return true;
        }

        return false;
    }
}
