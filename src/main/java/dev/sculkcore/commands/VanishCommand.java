package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VanishCommand implements CommandExecutor, Listener {
    private final SculkCorePlugin plugin;
    private static final Map<UUID, Boolean> vanishedPlayers = new HashMap<>();

    public VanishCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isVanished(UUID uuid) {
        return vanishedPlayers.getOrDefault(uuid, false);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("sculkcore.vanish")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        boolean vanished = vanishedPlayers.getOrDefault(uuid, false);

        if (vanished) {
            // Unvanish
            String joinMsg = plugin.getConfig().getString("vanish_join_message", "§e<player> joined the game")
                    .replace("<player>", player.getName());
            if (!joinMsg.isEmpty()) {
                Bukkit.broadcastMessage(joinMsg);
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showPlayer(plugin, player);
            }
            player.sendMessage("§cYou are no longer vanished");
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            vanishedPlayers.put(uuid, false);
        } else {
            // Vanish
            String leaveMsg = plugin.getConfig().getString("vanish_leave_message", "§e<player> left the game")
                    .replace("<player>", player.getName());
            if (!leaveMsg.isEmpty()) {
                Bukkit.broadcastMessage(leaveMsg);
            }
            player.sendMessage("§aYou are now vanished");
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false));
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hidePlayer(plugin, player);
            }
            vanishedPlayers.put(uuid, true);
        }

        return true;
    }

    @EventHandler
    public void onPlayerAttemptPickup(PlayerAttemptPickupItemEvent event) {
        if (isVanished(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        if (isVanished(joining.getUniqueId())) {
            event.setJoinMessage(null);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hidePlayer(plugin, joining);
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isVanished(p.getUniqueId())) {
                joining.hidePlayer(plugin, p);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player quitting = event.getPlayer();
        if (isVanished(quitting.getUniqueId())) {
            event.setQuitMessage(null);
        }
    }
}
