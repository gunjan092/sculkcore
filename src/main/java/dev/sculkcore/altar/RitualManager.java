package dev.sculkcore.altar;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.tasks.RitualTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RitualManager {
    public static final List<Entity> activeEntities = new ArrayList<>();
    public static final List<BossBar> activeBossBars = new ArrayList<>();
    public static BossBar activeBossBar;

    public static Color getRitualParticleColor() {
        String colorStr = SculkCorePlugin.getInstance().getConfig().getString("config.ritual_particle_color", "#FF0000");
        try {
            if (colorStr.startsWith("#")) {
                int rgb = Integer.parseInt(colorStr.substring(1), 16);
                return Color.fromRGB(rgb);
            }
            switch (colorStr.toUpperCase()) {
                case "RED": return Color.RED;
                case "BLUE": return Color.BLUE;
                case "GREEN": return Color.GREEN;
                case "YELLOW": return Color.YELLOW;
                case "PINK": case "FUCHSIA": return Color.FUCHSIA;
                case "ORANGE": return Color.ORANGE;
                case "PURPLE": return Color.PURPLE;
                case "WHITE": return Color.WHITE;
                case "BLACK": return Color.BLACK;
                case "AQUA": return Color.AQUA;
                case "LIME": return Color.LIME;
                case "GRAY": case "GREY": return Color.GRAY;
            }
        } catch (Exception ignored) {}
        return Color.RED;
    }

    public static void startRitual(Player player, ItemStack itemStack, Location loc) {
        SculkCorePlugin plugin = SculkCorePlugin.getInstance();
        int durationTicks = plugin.getConfig().getInt("config.ritual_duration", 60) * 20;

        String worldName = loc.getWorld().getName();
        String worldDisplayName = ChatColor.GRAY + worldName;
        if (worldName.equalsIgnoreCase("world")) {
            worldDisplayName = ChatColor.GREEN + "Overworld";
        } else if (worldName.equalsIgnoreCase("world_nether")) {
            worldDisplayName = ChatColor.RED + "Nether";
        } else if (worldName.equalsIgnoreCase("world_the_end")) {
            worldDisplayName = ChatColor.LIGHT_PURPLE + "End";
        }

        String formattedCoords = "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
        
        String itemDisplayName = plugin.getAltarManager().getItemDisplayName(itemStack);

        String hexColor = "§x§9§D§0§0§B§5";
        String broadcastMsg = hexColor + player.getName() + hexColor + " has started a ritual at §6§l" + worldDisplayName + " §7: " + formattedCoords + hexColor + " for §d" + itemDisplayName;
        
        Bukkit.broadcastMessage(plugin.getConfigManager().translateColor(broadcastMsg));
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }

        BossBar bossBar = Bukkit.createBossBar(
                plugin.getConfigManager().translateColor("&d&lRITUAL: &7" + itemDisplayName),
                BarColor.PINK,
                BarStyle.SOLID
        );
        bossBar.setProgress(1.0);
        bossBar.setVisible(true);
        activeBossBar = bossBar;
        activeBossBars.add(bossBar);

        for (Player p : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(p);
        }

        Location itemLoc = loc.clone().add(0.5, 0.5, 0.5);
        Item floatingItem = loc.getWorld().dropItem(itemLoc, itemStack);
        floatingItem.setGravity(false);
        floatingItem.setNoPhysics(true);
        floatingItem.setVelocity(new Vector(0, 0, 0));
        floatingItem.setUnlimitedLifetime(true);
        floatingItem.setCanMobPickup(false);
        floatingItem.setCanPlayerPickup(false);
        floatingItem.setPickupDelay(Integer.MAX_VALUE);
        floatingItem.setInvulnerable(true);
        floatingItem.setMetadata("no_hopper", new FixedMetadataValue(plugin, "no_hopper1"));

        Location as1Loc = itemLoc.clone().add(0.0, 0.8, 0.0);
        ArmorStand as1 = (ArmorStand) loc.getWorld().spawnEntity(as1Loc, EntityType.ARMOR_STAND);
        as1.setGravity(false);
        as1.setVelocity(new Vector(0, 0, 0));
        as1.setSilent(true);
        as1.setInvisible(true);
        as1.setInvulnerable(true);
        as1.setMarker(true);
        as1.setCustomNameVisible(true);
        as1.setCustomName(ChatColor.LIGHT_PURPLE + itemDisplayName);

        Location as2Loc = itemLoc.clone().add(0.0, 0.5, 0.0);
        ArmorStand as2 = (ArmorStand) loc.getWorld().spawnEntity(as2Loc, EntityType.ARMOR_STAND);
        as2.setGravity(false);
        as2.setVelocity(new Vector(0, 0, 0));
        as2.setSilent(true);
        as2.setInvisible(true);
        as2.setInvulnerable(true);
        as2.setMarker(true);
        as2.setCustomNameVisible(true);
        as2.setCustomName("");

        activeEntities.add(floatingItem);
        activeEntities.add(as1);
        activeEntities.add(as2);

        new RitualTask(durationTicks, bossBar, as1, as2, floatingItem, itemDisplayName, loc).runTaskTimer(plugin, 0L, 1L);
    }

    public static void cleanUp() {
        for (Entity entity : activeEntities) {
            if (entity.isValid()) {
                entity.remove();
            }
        }
        activeEntities.clear();

        for (BossBar bar : activeBossBars) {
            bar.setVisible(false);
            bar.removeAll();
        }
        activeBossBars.clear();
        activeBossBar = null;
    }
}
