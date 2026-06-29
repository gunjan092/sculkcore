package dev.sculkcore.tasks;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class GraceTask extends BukkitRunnable {
    private final SculkCorePlugin plugin;

    public GraceTask(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        double d2 = GameState.getGlobalCooldownRemaining("grace");
        if (d2 <= 0.0) {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective objective = scoreboard.getObjective("grace");
            if (objective != null) {
                objective.unregister();
            }

            String title = plugin.getConfig().getString("config.grace_end_title", "&aGRACE IS OVER!");
            String subtitle = plugin.getConfig().getString("config.grace_end_subtitle", "&7PvP has been enabled");
            String message = plugin.getConfig().getString("config.grace_end_message", "");

            String coloredTitle = plugin.getConfigManager().translateColor(title);
            String coloredSubtitle = plugin.getConfigManager().translateColor(subtitle);
            String coloredMsg = message.isEmpty() ? "" : plugin.getConfigManager().translateColor(message);

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                player.sendTitle(coloredTitle, coloredSubtitle, 10, 70, 20);
                if (!coloredMsg.isEmpty()) {
                    player.sendMessage(coloredMsg);
                }
            }
            cancel();
            return;
        }

        int minutes = (int) (d2 / 60.0);
        int seconds = (int) (d2 % 60.0);
        String formatted = String.format("%02d:%02d", minutes, seconds);

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = scoreboard.getObjective("grace");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("grace", "dummy", plugin.getConfigManager().translateColor("&a&lGRACE"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            for (String entry : scoreboard.getEntries()) {
                scoreboard.resetScores(entry);
            }
        }
        objective.getScore(org.bukkit.ChatColor.WHITE + "⌚ " + formatted).setScore(0);
    }
}
