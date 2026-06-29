package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReplyCommand implements CommandExecutor, Listener {
    private final SculkCorePlugin plugin;
    private static final Map<UUID, UUID> replyTargets = new HashMap<>();

    public ReplyCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    public static void setReplyTarget(UUID player, UUID target) {
        replyTargets.put(player, target);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player playerSender)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length < 1) {
            playerSender.sendMessage("§cUsage: /r <message>");
            return true;
        }

        UUID targetUUID = replyTargets.get(playerSender.getUniqueId());
        if (targetUUID == null) {
            playerSender.sendMessage("§cNo one has messaged you recently!");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(targetUUID);
        if (targetPlayer == null || !targetPlayer.isOnline()) {
            playerSender.sendMessage("§cThat player is no longer online!");
            replyTargets.remove(playerSender.getUniqueId());
            return true;
        }

        String message = String.join(" ", args);
        
        // Update targets so target can reply back
        replyTargets.put(targetPlayer.getUniqueId(), playerSender.getUniqueId());

        String senderFormat = plugin.getConfig().getString("rules.reply_message_format_sender", "&7[&aMe &7-> &e<target>&7] &7<message>");
        String receiverFormat = plugin.getConfig().getString("rules.reply_message_format_receiver", "&7[&e<player> &7-> &aMe&7] &7<message>");

        String toSender = formatMsg(senderFormat, playerSender, targetPlayer, message);
        String toReceiver = formatMsg(receiverFormat, playerSender, targetPlayer, message);

        playerSender.sendMessage(toSender);
        targetPlayer.sendMessage(toReceiver);
        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        return true;
    }

    private String formatMsg(String template, Player sender, Player target, String msg) {
        if (template == null) {
            template = "&7[&a<player> &7-> &e<target>&7] &7<message>";
        }
        String formatted = template
                .replace("<player>", sender.getName())
                .replace("<target>", target.getName())
                .replace("<message>", msg);
        
        return (plugin.getConfigManager() != null)
                ? plugin.getConfigManager().translateColor(formatted)
                : ChatColor.translateAlternateColorCodes('&', formatted);
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player sender = event.getPlayer();
        String msg = event.getMessage();
        String lower = msg.toLowerCase();

        if (lower.startsWith("/msg ") || lower.startsWith("/tell ") || lower.startsWith("/w ") || lower.startsWith("/whisper ")) {
            String[] parts = msg.split(" ");
            if (parts.length >= 2) {
                String targetName = parts[1];
                Player target = Bukkit.getPlayer(targetName);
                if (target != null && !target.equals(sender)) {
                    replyTargets.put(target.getUniqueId(), sender.getUniqueId());
                }
            }
        }
    }
}
