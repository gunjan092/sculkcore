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
            plugin.sendTellrawAsync("\u00a7f\u00a7a\u00a7i\u00a7r\u00a7x\u00a7a\u00a7e\u00a7r\u00a7o", player);
        } else {
            // Sends the total minimap disable sequence
            plugin.sendTellrawAsync("\u00a7n\u00a7o\u00a7m\u00a7i\u00a7n\u00a7i\u00a7m\u00a7a\u00a7p", player);
        }
    }
}
