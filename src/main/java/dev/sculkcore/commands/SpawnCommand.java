package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpawnCommand implements CommandExecutor, TabCompleter, Listener {
    private final SculkCorePlugin plugin;

    public SpawnCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        String cmdName = command.getName().toLowerCase();
        if (cmdName.equals("setcustomspawn")) {
            if (!player.hasPermission("sculkcore.setcustomspawn")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length == 0) {
                Location loc = player.getLocation();
                plugin.getConfig().set("custom_spawn", loc);
                plugin.saveConfig();
                player.sendMessage("§7Set spawn to §b" + formatLoc(loc));
            } else {
                plugin.getConfig().set("custom_spawn", null);
                plugin.saveConfig();
                player.sendMessage("§7Removed custom spawn, Players will now be teleported to the normal spawn");
            }
            return true;
        } else if (cmdName.equals("setrespawnspawn")) {
            if (!player.hasPermission("sculkcore.setrespawnspawn")) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length == 0) {
                Location loc = player.getLocation();
                plugin.getConfig().set("respawn_spawn", loc);
                plugin.saveConfig();
                player.sendMessage("§7Set respawn spawn to §b" + formatLoc(loc));
            } else {
                plugin.getConfig().set("respawn_spawn", null);
                plugin.saveConfig();
                player.sendMessage("§7Removed respawn spawn, Players will now be teleported to the normal or custom spawn");
            }
            return true;
        }

        return false;
    }

    private String formatLoc(Location loc) {
        if (loc == null) return "null";
        return String.format("%s, %.1f, %.1f, %.1f", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of("remove");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) {
            return;
        }

        Location customSpawn = plugin.getConfig().getLocation("custom_spawn");
        if (customSpawn != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.teleport(customSpawn), 1L);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location respawnSpawn = plugin.getConfig().getLocation("respawn_spawn");
        if (respawnSpawn != null) {
            event.setRespawnLocation(respawnSpawn);
            // Teleport 1 tick later to be safe
            Bukkit.getScheduler().runTaskLater(plugin, () -> event.getPlayer().teleport(respawnSpawn), 1L);
            return;
        }

        Location customSpawn = plugin.getConfig().getLocation("custom_spawn");
        if (customSpawn != null) {
            event.setRespawnLocation(customSpawn);
            Bukkit.getScheduler().runTaskLater(plugin, () -> event.getPlayer().teleport(customSpawn), 1L);
        }
    }
}
