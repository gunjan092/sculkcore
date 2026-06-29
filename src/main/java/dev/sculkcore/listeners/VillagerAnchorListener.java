package dev.sculkcore.listeners;

import dev.sculkcore.SculkCorePlugin;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VillagerAnchorListener implements Listener {
    private final SculkCorePlugin plugin;
    private final NamespacedKey villagerKey;
    private final Map<UUID, Long> bypassMove = new HashMap<>();

    // Map for skin textures of professions
    private static final Map<Villager.Profession, String> SKIN_TEXTURES = new HashMap<>();
    private static final String DEFAULT_TEXTURE = "https://textures.minecraft.net/texture/b5504fa38bfe3733e0aec43db60a05b3dc803b233494ac9b2083cd523a8cc0cc";
    private static final String BABY_TEXTURE = "http://textures.minecraft.net/texture/3d7788826b9ac4deaf383b387947085211447ed50fdc21bf71c230048dd5986f";

    static {
        SKIN_TEXTURES.put(Villager.Profession.FISHERMAN, "https://textures.minecraft.net/texture/441ba44ca36882827814a65949c71f8180bdb85045025eebf399f621bab2f264");
        SKIN_TEXTURES.put(Villager.Profession.ARMORER, "http://textures.minecraft.net/texture/f522db92f188ebc7713cf35b4cbaed1cfe2642a5986c3bde993f5cfb3727664c");
        SKIN_TEXTURES.put(Villager.Profession.BUTCHER, "http://textures.minecraft.net/texture/c6774d2df515eceae9eed291c1b40f94adf71df0ab81c7191402e1a45b3a2087");
        SKIN_TEXTURES.put(Villager.Profession.CARTOGRAPHER, "http://textures.minecraft.net/texture/94248dd0680305ad73b214e8c6b00094e27a4ddd8034676921f905130b858bdb");
        SKIN_TEXTURES.put(Villager.Profession.CLERIC, "http://textures.minecraft.net/texture/a8856eaafad96d76fa3b5edd0e3b5f45ee49a3067306ad94df9ab3bd5b2d142d");
        SKIN_TEXTURES.put(Villager.Profession.FARMER, "http://textures.minecraft.net/texture/d01e035a3d8d6126072bcbe52a97913ace93552a99995b5d4070d6783a31e909");
        SKIN_TEXTURES.put(Villager.Profession.FLETCHER, "http://textures.minecraft.net/texture/d831830a7bd3b1ab05beb98dc2f9fc5ea550b3cf649fd94d483da7cd39f7c063");
        SKIN_TEXTURES.put(Villager.Profession.LEATHERWORKER, "http://textures.minecraft.net/texture/f76cf8b7378e889395d538e6354a17a3de6b294bb6bf8db9c701951c68d3c0e6");
        SKIN_TEXTURES.put(Villager.Profession.LIBRARIAN, "https://textures.minecraft.net/texture/4c21efc51caab45cbbfc0207aee116dbb0d394e85356f5f7a9639300fec9f149");
        SKIN_TEXTURES.put(Villager.Profession.MASON, "http://textures.minecraft.net/texture/2c02c3ffd5705ab488b305d57ff0168e26de70fd3f739e839661ab947dff37b1");
        SKIN_TEXTURES.put(Villager.Profession.NITWIT, "http://textures.minecraft.net/texture/35e799dbfaf98287dfbafce970612c8f075168977aacc30989d34a4a5fcdf429");
        SKIN_TEXTURES.put(Villager.Profession.SHEPHERD, "http://textures.minecraft.net/texture/19e04a752596f939f581930414561b175454d45a0506501e7d2488295a5d5de");
        SKIN_TEXTURES.put(Villager.Profession.TOOLSMITH, "http://textures.minecraft.net/texture/7dfa07fd1244eb8945f4ededd00426750b77ef5dfbaf03ed775633459ece415a");
        SKIN_TEXTURES.put(Villager.Profession.WEAPONSMITH, "http://textures.minecraft.net/texture/5e409b958bc4fe045e95d325e6e97a533137e33fec7042ac027b30bb693a9d42");
    }

    public VillagerAnchorListener(SculkCorePlugin plugin) {
        this.plugin = plugin;
        this.villagerKey = new NamespacedKey("clickvillager", "ballright");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        if (!plugin.getConfig().getBoolean("rules.clickVillager", false)) {
            return;
        }

        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Villager villager)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();

        // 1. Shears Anchor System
        if (handItem.getType() == Material.SHEARS) {
            if (!plugin.getConfig().getBoolean("rules.clickVillager_anchor", false)) {
                return;
            }
            event.setCancelled(true);
            toggleAnchor(player, villager);
            return;
        }

        // 2. Shovel Owning System
        if (handItem.getType().toString().contains("_SHOVEL")) {
            if (!plugin.getConfig().getBoolean("rules.clickVillager_owner", false)) {
                return;
            }
            event.setCancelled(true);
            toggleOwner(player, villager);
            return;
        }

        // 3. Regular pickup (with empty hand or other items)
        event.setCancelled(true);

        // Check ownership tag if owning system is enabled and villager has tags
        if (plugin.getConfig().getBoolean("rules.clickVillager_owner", false)) {
            boolean hasTags = false;
            boolean isOwner = false;
            for (String tag : villager.getScoreboardTags()) {
                if (tag.endsWith("_villager_smpcore_click")) {
                    hasTags = true;
                    if (tag.equals(player.getName() + "_villager_smpcore_click")) {
                        isOwner = true;
                    }
                }
            }
            if (hasTags && !isOwner) {
                player.sendMessage(ChatColor.RED + "You do not own this villager!");
                return;
            }
        }

        pickupVillager(player, villager);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("rules.clickVillager", false)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack handItem = event.getItemInHand();
        ItemMeta meta = handItem.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(villagerKey, PersistentDataType.BOOLEAN)) {
            return;
        }

        event.setCancelled(true);
        placeVillager(event.getBlock().getLocation().clone().add(0.5, 0.0, 0.5), handItem, player);
    }

    @EventHandler
    public void onVillagerMove(EntityMoveEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof Villager villager)) {
            return;
        }

        if (!villager.getScoreboardTags().contains("anchored")) {
            return;
        }

        // Check if under temporary bypass
        Long bypassTime = bypassMove.get(villager.getUniqueId());
        if (bypassTime != null && System.currentTimeMillis() < bypassTime) {
            return;
        }

        event.setCancelled(true);
    }

    private void toggleAnchor(Player player, Villager villager) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
        if (villager.getScoreboardTags().contains("anchored")) {
            villager.removeScoreboardTag("anchored");
            player.sendMessage(ChatColor.RED + "⚓ Unanchored");
        } else {
            villager.addScoreboardTag("anchored");
            player.sendMessage(ChatColor.GREEN + "⚓ Anchored");
        }
    }

    private void toggleOwner(Player player, Villager villager) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        String ownerTag = player.getName() + "_villager_smpcore_click";
        if (villager.getScoreboardTags().contains(ownerTag)) {
            villager.removeScoreboardTag(ownerTag);
            player.sendMessage(ChatColor.RED + "👑 You no longer own this villager");
        } else {
            // Remove any other owner tags first
            List<String> tagsToRemove = new ArrayList<>();
            for (String tag : villager.getScoreboardTags()) {
                if (tag.endsWith("_villager_smpcore_click")) {
                    tagsToRemove.add(tag);
                }
            }
            for (String tag : tagsToRemove) {
                villager.removeScoreboardTag(tag);
            }
            villager.addScoreboardTag(ownerTag);
            player.sendMessage(ChatColor.GREEN + "👑 You now own this villager");
        }
    }

    private void pickupVillager(Player player, Villager villager) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 2.0f);
        
        ItemStack head = createVillagerHead(villager);
        player.getInventory().addItem(head);

        // Safe location at Y=70000
        Location location = new Location(player.getWorld(), 0.0, 70000.0, 0.0);
        forceLoadChunk(location);

        bypassMove.put(villager.getUniqueId(), System.currentTimeMillis() + 1000);
        villager.teleport(location);
        villager.setAI(false);
        villager.setGravity(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
    }

    private void placeVillager(Location location, ItemStack item, Player player) {
        List<String> lore = item.getLore();
        if (lore == null || lore.size() < 4) {
            return;
        }

        String uuidStr = ChatColor.stripColor(lore.get(3));
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return;
        }

        Entity entity = Bukkit.getEntity(uuid);
        if (entity instanceof Villager villager) {
            item.setAmount(item.getAmount() - 1);
            bypassMove.put(villager.getUniqueId(), System.currentTimeMillis() + 1000);
            
            villager.teleport(location);
            villager.setAI(true);
            villager.setGravity(true);
            villager.setInvulnerable(false);
            villager.setSilent(false);
            
            player.getWorld().playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
        }
    }

    private void forceLoadChunk(Location location) {
        World world = location.getWorld();
        if (world != null) {
            Chunk chunk = location.getChunk();
            chunk.setForceLoaded(true);
            chunk.load(true);
        }
    }

    private ItemStack createVillagerHead(Villager villager) {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
        if (skullMeta == null) {
            return itemStack;
        }

        // Apply profession skin
        String texture = BABY_TEXTURE;
        if (villager.isAdult()) {
            texture = SKIN_TEXTURES.getOrDefault(villager.getProfession(), DEFAULT_TEXTURE);
        }
        
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            profile.getTextures().setSkin(new URL(texture));
            skullMeta.setOwnerProfile(profile);
        } catch (MalformedURLException e) {
            // Ignored
        }

        // Set Display Name
        String profName = villager.getProfession().getKey().getKey().replace("_", " ");
        if (profName.equals("none")) profName = "unemployed";
        profName = capitalize(profName) + " Villager";
        skullMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.BOLD + profName);

        // Find Owner Name
        String owner = "None";
        for (String tag : villager.getScoreboardTags()) {
            if (tag.endsWith("_villager_smpcore_click")) {
                owner = tag.replace("_villager_smpcore_click", "");
                break;
            }
        }

        boolean isAnchored = villager.getScoreboardTags().contains("anchored");

        // Lore lines
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Right click to place the villager back");
        lore.add(ChatColor.YELLOW + "👑 Owner: " + ChatColor.GRAY + owner);
        lore.add(ChatColor.BLUE + "⚓ Anchored: " + ChatColor.GRAY + isAnchored);
        lore.add(ChatColor.BLACK + villager.getUniqueId().toString());
        lore.add(ChatColor.DARK_GRAY + "💎 " + ChatColor.GRAY + "Trades:");

        for (MerchantRecipe recipe : villager.getRecipes()) {
            StringBuilder ingredients = new StringBuilder();
            for (ItemStack ingredient : recipe.getIngredients()) {
                if (ingredient.getType() != Material.AIR) {
                    ingredients.append(getFriendlyName(ingredient)).append(" + ");
                }
            }
            if (ingredients.length() > 3) {
                ingredients.setLength(ingredients.length() - 3);
            }
            String result = getFriendlyName(recipe.getResult());
            lore.add("  " + ChatColor.GRAY + "- " + ingredients + " " + ChatColor.DARK_GRAY + "➡ " + ChatColor.GRAY + result);
        }

        skullMeta.setLore(lore);
        skullMeta.getPersistentDataContainer().set(villagerKey, PersistentDataType.BOOLEAN, true);
        itemStack.setItemMeta(skullMeta);

        return itemStack;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String getFriendlyName(ItemStack item) {
        String name = item.getType().toString().toLowerCase().replace("_", " ");
        name = capitalize(name);
        int amount = item.getAmount();
        if (amount > 1) {
            return amount + " " + name;
        }
        return name;
    }
}
