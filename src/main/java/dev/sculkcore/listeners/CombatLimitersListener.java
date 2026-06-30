package dev.sculkcore.listeners;

import dev.sculkcore.SculkCorePlugin;
import dev.sculkcore.game.GameState;
import io.papermc.paper.event.block.VaultChangeStateEvent;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PlayerItemCooldownEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;

public class CombatLimitersListener implements Listener {
    private final SculkCorePlugin plugin;
    private final File maceFile;
    private final YamlConfiguration maceConfig;

    public CombatLimitersListener(SculkCorePlugin plugin) {
        this.plugin = plugin;
        this.maceFile = new File(plugin.getDataFolder(), "mace.yml");
        this.maceConfig = YamlConfiguration.loadConfiguration(maceFile);
        if (!maceFile.exists()) {
            maceConfig.set("mace_crafted", false);
            saveMaceConfig();
        }
    }

    private void saveMaceConfig() {
        try {
            maceConfig.save(maceFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save mace.yml: " + e.getMessage());
        }
    }
    // 1. Cooldown Adjustments & Consumption

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerItemCooldown(PlayerItemCooldownEvent event) {
        if (event.getType() == Material.SHIELD) {
            int shieldCd = plugin.getConfig().getInt("rules.shield_cooldown", 60);
            event.setCooldown(shieldCd);
        } else if (event.getType() == Material.ENDER_PEARL) {
            int pearlCd = plugin.getConfig().getInt("rules.ender_pearl", 1) * 20;
            event.setCooldown(pearlCd);
        } else if (event.getType() == Material.WIND_CHARGE) {
            double windCd = plugin.getConfig().getDouble("rules.wind_charge", 0.5) * 20.0;
            event.setCooldown((int) windCd);
        }
    }

    @EventHandler
    public void onPlayerRiptide(PlayerRiptideEvent event) {
        int tridentCd = plugin.getConfig().getInt("rules.trident", 1) * 20;
        event.getPlayer().setCooldown(Material.TRIDENT, tridentCd);
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.ENCHANTED_GOLDEN_APPLE) {
            int gapCd = plugin.getConfig().getInt("rules.gap", 1) * 20;
            event.getPlayer().setCooldown(Material.ENCHANTED_GOLDEN_APPLE, gapCd);
        }
    }
    // 2. Mace Cooldown & Attack Handler

    @EventHandler(priority = EventPriority.HIGH)
    public void onMaceAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || event.getFinalDamage() == 0.0) {
            return;
        }

        if (event.getDamager() instanceof Player player) {
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (weapon.getType() == Material.MACE) {
                if (plugin.getConfig().getBoolean("rules.ban_mace", false)) {
                    event.setCancelled(true);
                    player.sendMessage("§cMaces are completely banned on this server!");
                    return;
                }

                int maceCdSec = plugin.getConfig().getInt("rules.mace", 0);
                if (maceCdSec > 0) {
                    if (player.hasCooldown(Material.MACE)) {
                        event.setCancelled(true);
                        player.setVelocity(new Vector(0, 0, 0));
                        player.setFallDistance(0.0f);
                        player.sendMessage("§cYour mace is still on cooldown");
                        return;
                    }
                    player.setCooldown(Material.MACE, maceCdSec * 20);
                }
            }
        }
    }
    // 3. Spear Arm Swing Lunge

    @EventHandler
    public void onPlayerArmSwing(PlayerArmSwingEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held != null && held.getType().toString().contains("_SPEAR")) {
            // Retrieve Lunge enchantment dynamically by key name
            Enchantment lungeEnchant = null;
            for (Enchantment enchant : held.getEnchantments().keySet()) {
                if (enchant.getKey().getKey().equalsIgnoreCase("lunge")) {
                    lungeEnchant = enchant;
                    break;
                }
            }
            if (lungeEnchant != null) {
                int lungeCd = plugin.getConfig().getInt("rules.lunge_cooldown", 2);
                if (GameState.isCooldownActive(player.getUniqueId(), "lunge")) {
                    event.setCancelled(true);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.setVelocity(new Vector(0, 0, 0)), 1L);
                    player.sendMessage("§cYour spear is still on cooldown");
                    return;
                }
                
                GameState.setCooldown(player.getUniqueId(), "lunge", lungeCd);
                player.setCooldown(held.getType(), lungeCd * 20);
                player.clearActiveItem();
                player.updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpearAttack(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || event.getFinalDamage() <= 1.0) {
            return;
        }

        if (event.getDamager() instanceof Player player) {
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (weapon.getType().toString().contains("_SPEAR")) {
                if (player.hasCooldown(weapon.getType())) {
                    event.setCancelled(true);
                    player.sendMessage("§cYour spear is still on cooldown");
                    return;
                }

                int spearCdSec = plugin.getConfig().getInt("rules.spear_cooldown", 1);
                player.setCooldown(weapon.getType(), spearCdSec * 20);
                player.clearActiveItem();
                player.updateInventory();
            }
        }
    }
    // 4. Mace Crafting Limit & Restrictions

    @EventHandler
    public void onCraftMace(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (result.getType() == Material.MACE) {
            if (plugin.getConfig().getBoolean("rules.ban_mace", false)) {
                event.setCancelled(true);
                return;
            }

            if (plugin.getConfig().getBoolean("rules.mace_limit", true)) {
                if (maceConfig.getBoolean("mace_crafted", false)) {
                    event.getWhoClicked().sendMessage("§cThe One and Only Mace has already been crafted!");
                    event.setCancelled(true);
                    return;
                }
                maceConfig.set("mace_crafted", true);
                saveMaceConfig();
                event.getWhoClicked().sendMessage("§aYou have crafted the only Mace on this server!");
            }
        }
    }

    @EventHandler
    public void onPrepareCraftMace(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        ItemStack result = event.getRecipe().getResult();
        if (result.getType() == Material.MACE && plugin.getConfig().getBoolean("rules.ban_mace", false)) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onCrafterCraftMace(CrafterCraftEvent event) {
        ItemStack result = event.getResult();
        if (result.getType() == Material.MACE && plugin.getConfig().getBoolean("rules.ban_mace", false)) {
            event.setCancelled(true);
        }
    }
    // 5. Damage Limiters (Mace, Spear, TNT, Crystal, Arrow, Fall, Carts)

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageByEntityLimiters(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player victim)) {
            return;
        }

        Entity damager = event.getDamager();

        // 1. Attacker is Player (Mace or Spear)
        if (damager instanceof Player attacker) {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            if (weapon.getType() == Material.MACE) {
                int limit = plugin.getConfig().getInt("rules.mace_damage_limiter", 0);
                if (limit > 0 && event.getFinalDamage() > limit) {
                    event.setCancelled(true);
                    applyDirectDamage(victim, limit, attacker);
                }
            } else if (weapon.getType().toString().contains("_SPEAR")) {
                int limit = plugin.getConfig().getInt("rules.spear_damage_limiter", 0);
                if (limit > 0 && event.getFinalDamage() > limit) {
                    event.setCancelled(true);
                    applyDirectDamage(victim, limit, attacker);
                }
            }
        }
        // 2. Damager is Explosive Minecart
        else if (damager instanceof ExplosiveMinecart) {
            int limit = plugin.getConfig().getInt("rules.cart_damage_limiter", 0);
            if (limit > 0 && event.getFinalDamage() > limit) {
                event.setCancelled(true);
                applyDirectDamage(victim, limit, null);
            }
        }
        // 3. Damager is TNT
        else if (damager instanceof TNTPrimed) {
            int limit = plugin.getConfig().getInt("rules.tnt_damage_limiter", 0);
            if (limit > 0 && event.getFinalDamage() > limit) {
                event.setCancelled(true);
                applyDirectDamage(victim, limit, null);
            }
        }
        // 4. Damager is End Crystal
        else if (damager instanceof EnderCrystal) {
            int limit = plugin.getConfig().getInt("rules.crystal_damage_limiter", 0);
            if (limit > 0 && event.getFinalDamage() > limit) {
                event.setCancelled(true);
                applyDirectDamage(victim, limit, null);
            }
        }
        // 5. Damager is Arrow
        else if (damager instanceof Arrow) {
            int limit = plugin.getConfig().getInt("rules.arrow_damage_limiter", 0);
            if (limit > 0 && event.getFinalDamage() > limit) {
                event.setCancelled(true);
                applyDirectDamage(victim, limit, null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageLimiters(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player victim)) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();
        String typeKey = event.getDamageSource().getDamageType().getKey().getKey().toLowerCase();
        boolean isFall = (cause == EntityDamageEvent.DamageCause.FALL)
                || typeKey.contains("stalactite")
                || typeKey.contains("stalagmite");

        if (isFall) {
            int limit = plugin.getConfig().getInt("rules.fall_damage_limiter", 0);
            if (limit > 0 && event.getFinalDamage() > limit) {
                event.setCancelled(true);
                applyDirectDamage(victim, limit, null);
            }
        }
    }
    // 6. Direct Damage Application Bypass (Totem safe)

    private void applyDirectDamage(LivingEntity entity, double damage, Player attacker) {
        int currentHealth = (int) entity.getHealth();
        if (entity instanceof Player victim) {
            if (victim.isBlocking()) {
                return;
            }
            if (victim.getGameMode() == GameMode.SPECTATOR || victim.getGameMode() == GameMode.CREATIVE) {
                return;
            }

            ItemStack main = victim.getInventory().getItemInMainHand();
            ItemStack off = victim.getInventory().getItemInOffHand();
            boolean hasTotem = (main.getType() == Material.TOTEM_OF_UNDYING) || (off.getType() == Material.TOTEM_OF_UNDYING);

            if (hasTotem && currentHealth <= damage) {
                // Instantly pop totem or kill player by dealing lethal damage
                victim.damage(200.0, attacker);
                return;
            }
        }

        double ab = entity.getAbsorptionAmount();
        if (ab > 0) {
            entity.setAbsorptionAmount(Math.max(ab - damage, 0.0));
            damage = Math.max(damage - ab, 0.0);
        }
        entity.setHealth(Math.max(entity.getHealth() - damage, 0.0));
        entity.playHurtAnimation(0.0f);
    }
    // 7. Vault Hook Guard

    @EventHandler
    public void onVaultChange(VaultChangeStateEvent event) {
        if (plugin.getConfig().getBoolean("rules.disable_vaults", false)) {
            event.setCancelled(true);
        }
    }
}
