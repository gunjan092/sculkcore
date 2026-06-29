package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WorldTpCommand implements CommandExecutor, TabCompleter {
    private final SculkCorePlugin plugin;

    public WorldTpCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player playerSender)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!playerSender.hasPermission("sculkcore.worldtp")) {
            playerSender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            playerSender.sendMessage("§cYou didn't specify a world");
            return true;
        }

        String worldName = args[0];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            File worldDir = new File(Bukkit.getWorldContainer(), worldName);
            if (worldDir.exists() && worldDir.isDirectory()) {
                playerSender.sendMessage("§7Loading world " + worldName + "...");
                try {
                    world = new WorldCreator(worldName).createWorld();
                } catch (Exception e) {
                    playerSender.sendMessage("§cFailed to load world: " + worldName);
                    return true;
                }
            } else {
                playerSender.sendMessage("§cWorld not found: " + worldName);
                return true;
            }
        }

        if (world == null) {
            playerSender.sendMessage("§cFailed to load world: " + worldName);
            return true;
        }

        Player target = playerSender;
        if (args.length >= 2) {
            Player p = Bukkit.getPlayer(args[1]);
            if (p == null || !p.isOnline()) {
                playerSender.sendMessage("§cThat player isn't online");
                return true;
            }
            target = p;
        }

        target.teleport(world.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        if (target != playerSender) {
            playerSender.sendMessage("§aTeleported " + target.getName() + " to world " + world.getName());
        } else {
            playerSender.sendMessage("§aTeleported to world " + world.getName());
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> worlds = new ArrayList<>();
            File[] files = Bukkit.getWorldContainer().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && new File(file, "level.dat").exists()) {
                        String name = file.getName();
                        if (name.toLowerCase().startsWith(args[0].toLowerCase())) {
                            worlds.add(name);
                        }
                    }
                }
            }
            return worlds;
        } else if (args.length == 2) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    players.add(player.getName());
                }
            }
            return players;
        }
        return null;
    }
}
