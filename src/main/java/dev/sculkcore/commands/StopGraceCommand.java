package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.game.GameState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StopGraceCommand implements CommandExecutor {
    private final SculkCorePlugin plugin;

    public StopGraceCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sculkcore.start")) {
            sender.sendMessage(plugin.getConfigManager().translateColor("&cYou do not have permission to run this command."));
            return true;
        }

        if (GameState.isGlobalCooldownActive("grace")) {
            GameState.setGlobalCooldown("grace", 0.0);
            sender.sendMessage(plugin.getConfigManager().translateColor("&aGrace period has been stopped early."));
        } else {
            sender.sendMessage(plugin.getConfigManager().translateColor("&cGrace period is not currently active."));
        }
        return true;
    }
}
