package dev.sculkcore.listeners;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class MinimapBlocker implements Listener {
    private final SculkCorePlugin plugin;

    public MinimapBlocker(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("rules.ban_minimap", false)) {
            return;
        }

        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("rules.minimap_fair", false)) {
            // Sends the fair-play minimap disable sequence
            plugin.sendTellrawAsync("§f§a§i§r§x§a§e§r§o", player);
        } else {
            // Sends the total minimap disable sequence
            plugin.sendTellrawAsync("§n§o§m§i§n§i§m§a§p", player);
        }
    }
}
