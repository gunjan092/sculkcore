package dev.sculkcore.items;

import dev.sculkcore.SculkCorePlugin;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.datacomponent.item.UseCooldown;
import io.papermc.paper.datacomponent.item.consumable.ConsumeEffect;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpecialItems {

    /**
     * Creates the special Warden Heart drop item.
     */
    public static ItemStack createWardenDrop() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§1Warden Heart");
            meta.setCustomModelData(9999);
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates the custom Golden Head item.
     */
    public static ItemStack createGoldenHead() {
        SculkCorePlugin plugin = SculkCorePlugin.getInstance();
        if (plugin == null) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        
        // Set profile UUID to golden head texture skin
        ResolvableProfile profile = ResolvableProfile.resolvableProfile()
                .uuid(UUID.fromString("bd307a7d-e13a-491b-ba4d-15b1493d4339"))
                .build();
        item.setData(DataComponentTypes.PROFILE, profile);
        item.unsetData(DataComponentTypes.USE_REMAINDER);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Golden Head §7(§c❤❤❤❤§e❤❤§7)");
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }

        int speedAmp = plugin.getConfig().getInt("goldenhead.speed_amplifier", 1);
        int speedDur = plugin.getConfig().getInt("goldenhead.speed_duration", 5);
        int absAmp = plugin.getConfig().getInt("goldenhead.absorption_amplifier", 1);
        int absDur = plugin.getConfig().getInt("goldenhead.absorption_duration", 120);
        int regAmp = plugin.getConfig().getInt("goldenhead.regeneration_amplifier", 2);
        int regDur = plugin.getConfig().getInt("goldenhead.regeneration_duration", 10);
        double consumeSecs = plugin.getConfig().getDouble("goldenhead.consume_seconds", 1.0);
        int cooldownSecs = plugin.getConfig().getInt("goldenhead.cooldown", 10);

        List<PotionEffect> effects = new ArrayList<>();
        effects.add(new PotionEffect(PotionEffectType.SPEED, speedDur * 20, speedAmp - 1));
        effects.add(new PotionEffect(PotionEffectType.ABSORPTION, absDur * 20, absAmp - 1));
        effects.add(new PotionEffect(PotionEffectType.REGENERATION, regDur * 20, regAmp - 1));

        List<ConsumeEffect> consumeEffects = List.of(
                ConsumeEffect.applyStatusEffects(effects, 1.0f)
        );

        Consumable consumable = Consumable.consumable()
                .consumeSeconds((float) consumeSecs)
                .addEffects(consumeEffects)
                .hasConsumeParticles(false)
                .build();

        item.setData(DataComponentTypes.CONSUMABLE, consumable);

        UseCooldown useCooldown = UseCooldown.useCooldown((float) cooldownSecs).build();
        item.setData(DataComponentTypes.USE_COOLDOWN, useCooldown);

        return item;
    }
}
