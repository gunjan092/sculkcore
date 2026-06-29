package dev.sculkcore.tasks;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import dev.sculkcore.game.GameState;

public class LaunchTask extends BukkitRunnable {
    private final Player player;

    public LaunchTask(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }
        
        player.setNoDamageTicks(10);
        player.setFallDistance(0.0f);
        GameState.setCooldown(player, "gliding", 0.2);
        
        if (player.isOnGround() || player.isInWater() || player.isInLava()) {
            cancel();
        }
    }
}
