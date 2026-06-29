package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SettingsCommand implements CommandExecutor, Listener {
    private final SculkCorePlugin plugin;
    private final NamespacedKey itemKey;

    // Titles
    public static final String TITLE = "      §x§8§6§8§6§8§6§lꜱ§x§8§D§8§D§8§D§lᴍ§x§9§3§9§3§9§3§lᴘ §x§A§0§A§0§A§0§lᴄ§x§A§7§A§7§A§7§lᴏ§x§A§D§A§D§A§D§lʀ§x§B§4§B§4§B§4§lᴇ §x§C§1§C§1§C§1§lꜱ§x§C§7§C§7§C§7§lᴇ§x§C§E§C§E§C§E§lᴛ§x§D§4§D§4§D§4§lᴛ§x§D§B§D§B§D§B§lɪ§x§E§1§E§1§E§1§lɴ§x§E§8§E§8§E§8§lɢ§x§E§E§E§E§E§E§lꜱ";
    public static final String RITUAL_TITLE = "§lRitual Items";
    public static final String ONE_TIME_TITLE = "§lOnly one on the server items";
    public static final String ARROW_TITLE = "§lBan Tipped Arrow";
    public static final String POTION_TITLE = "§lPotion Ban";
    public static final String ENCHANT_TITLE = "§lEnchant Ban";

    private static SettingsCommand instance;

    public SettingsCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "settings_item");
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        openPage(player, 1);
        return true;
    }

    public static void b(Player player) {
        if (instance != null) {
            instance.openPage(player, 1);
        }
    }
    // GUI Pages Construction

    public void openPage(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacySection().deserialize(TITLE));
        fillBorders(inv);

        // Navigation & Indicators
        inv.setItem(48, createNavButton("§c§lPrevious Page", Material.FEATHER));
        inv.setItem(49, createPageIndicator(page));
        inv.setItem(50, createNavButton("§a§lNext Page", Material.FEATHER));

        switch (page) {
            case 1:
                drawPage1(inv);
                break;
            case 2:
                drawPage2(inv);
                break;
            case 3:
                drawPage3(inv, player);
                break;
            case 4:
                drawPage4(inv);
                break;
        }

        player.openInventory(inv);
    }

    private void fillBorders(Inventory inv) {
        ItemStack pane = createSpacer();
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);
        for (int i = 9; i <= 36; i += 9) inv.setItem(i, pane);
        for (int i = 17; i <= 44; i += 9) inv.setItem(i, pane);

        // Decorative border glass pane elements
        inv.setItem(18, createSpacerMaterial(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(26, createSpacerMaterial(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(27, createSpacerMaterial(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(35, createSpacerMaterial(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(36, createSpacerMaterial(Material.BLACK_STAINED_GLASS_PANE));
        inv.setItem(44, createSpacerMaterial(Material.BLACK_STAINED_GLASS_PANE));
    }

    private ItemStack createSpacer() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSpacerMaterial(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavButton(String name, Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPageIndicator(int page) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = switch (page) {
                case 1 -> "rules";
                case 2 -> "balancing";
                case 3 -> "others";
                case 4 -> "customer suggestions";
                default -> "rules";
            };
            meta.setDisplayName("§6§l" + name);
            meta.setLore(List.of("§7PAGE: §7" + page));
            item.setItemMeta(meta);
        }
        return item;
    }
    // Page Drawing Logic

    private void drawPage1(Inventory inv) {
        // Items placed using inv.addItem starting at first free slot (10)
        inv.addItem(createToggleItem("rules.pvp", "PvP", Material.DIAMOND_SWORD));
        inv.addItem(createToggleItem("rules.naked_protection", "Naked Protection", Material.LEATHER_CHESTPLATE));
        inv.addItem(createToggleItem("rules.afk_protection", "AFK Protection", Material.BEDROCK));
        inv.addItem(createToggleItem("rules.infinite_restock", "Villager Restock", Material.EMERALD));
        inv.addItem(createToggleItem("rules.string_dupers", "String Dupers exploit", Material.STRING));

        // Fixed slots
        inv.setItem(19, createSpectatorDeathBanItem());
        inv.setItem(20, createDimensionsItem());
        inv.setItem(21, createMaceItem());
        inv.setItem(22, createToggleItem("rules.ban_carts", "Ban Carts", Material.MINECART));
        inv.setItem(23, createToggleItem("rules.ban_bed_bombing", "Ban Bed Bombing", Material.RED_BED));
        inv.setItem(24, createToggleItem("rules.ban_crystal_pvp", "Ban Crystal PvP", Material.END_CRYSTAL));
        inv.setItem(25, createToggleItem("rules.ban_netherite", "Ban Netherite Gear", Material.NETHERITE_INGOT));

        inv.setItem(28, createToggleItem("rules.ban_killing_villagers", "Ban Killing Villagers", Material.VILLAGER_SPAWN_EGG));
        inv.setItem(29, createSubmenuItem("Enchant Ban", Material.ENCHANTED_BOOK));
        inv.setItem(30, createSubmenuItem("Potion Ban", Material.POTION));
        inv.setItem(31, createSubmenuItem("Ban Tipped Arrow", Material.TIPPED_ARROW));
        inv.setItem(32, createSpacerMaterial(Material.GRAY_STAINED_GLASS_PANE)); // s.ac placeholder/unused
        inv.setItem(33, createToggleItem("rules.anti_draining", "Anti Draining", Material.BUCKET));
        inv.setItem(34, createToggleItem("rules.ban_breach_swapping", "Ban Breach Swapping", Material.ANVIL));

        inv.setItem(37, createAntiXrayItem());
        inv.setItem(38, createToggleItem("rules.combat_system", "Combat System", Material.IRON_SWORD));
        inv.setItem(39, createHealthIndicatorsItem());
        inv.setItem(40, createToggleItem("rules.ban_seed_cracking", "Ban Seed Cracking", Material.WHEAT_SEEDS));
        inv.setItem(41, createToggleItem("unsupported-settings.update-equipment-on-player-actions", "Update Equipment", Material.ARMOR_STAND));
        inv.setItem(42, createToggleItem("anticheat.obfuscation.items.hide-durability", "Hide Durability", Material.GOLDEN_HOE));
        inv.setItem(43, createToggleItem("rules.ban_minimap", "Ban Minimap", Material.MAP));
    }

    private void drawPage2(Inventory inv) {
        inv.setItem(10, createNumericItem("rules.lunge_cooldown", "Lunge Cooldown", Material.DIAMOND_SWORD, "rules.spear_cooldown", "Spear Cooldown", 40, 20));
        inv.setItem(11, createSingleNumericItem("rules.mace", "Mace Cooldown", Material.MACE, 0, 1.0));
        inv.setItem(12, createSingleNumericItem("rules.shield_cooldown", "Shield Cooldown", Material.SHIELD, 60, 1.0));
        inv.setItem(13, createSingleNumericItem("rules.ender_pearl", "Ender Pearl Cooldown", Material.ENDER_PEARL, 1, 1.0));
        inv.setItem(14, createSingleNumericItem("rules.wind_charge", "Wind Charge Cooldown", Material.WIND_CHARGE, 0.5, 0.5));
        inv.setItem(15, createSingleNumericItem("rules.trident", "Trident Cooldown", Material.TRIDENT, 0, 1.0));
        inv.setItem(16, createSingleNumericItem("rules.gap", "Gap Cooldown", Material.GOLDEN_APPLE, 0, 1.0));

        inv.setItem(19, createSingleNumericItem("rules.spear_damage_limiter", "Spear Damage Limit", Material.DIAMOND_SWORD, 0, 1.0));
        inv.setItem(20, createSingleNumericItem("rules.mace_damage_limiter", "Mace Damage Limit", Material.MACE, 0, 1.0));
        inv.setItem(21, createSingleNumericItem("rules.cart_damage_limiter", "Cart Damage Limit", Material.TNT_MINECART, 0, 1.0));
        inv.setItem(22, createSingleNumericItem("rules.tnt_damage_limiter", "TNT Damage Limit", Material.TNT, 0, 1.0));
        inv.setItem(23, createSingleNumericItem("rules.crystal_damage_limiter", "Crystal Damage Limit", Material.END_CRYSTAL, 0, 1.0));
        inv.setItem(24, createSingleNumericItem("rules.arrow_damage_limiter", "Arrow Damage Limit", Material.ARROW, 0, 1.0));
        inv.setItem(25, createSingleNumericItem("rules.fall_damage_limiter", "Fall Damage Limit", Material.FEATHER, 0, 1.0));

        inv.addItem(createSpacerMaterial(Material.GRAY_STAINED_GLASS_PANE)); // s.ab placeholder
        inv.addItem(createSingleNumericItem("rules.extra_damage", "Extra Damage", Material.DIAMOND_SWORD, 0.0, 1.0));
    }

    private void drawPage3(Inventory inv, Player player) {
        inv.addItem(createSubmenuItem("Ritual Items", Material.ENCHANTING_TABLE));
        inv.addItem(createToggleItem("rules.make_custom_items_glow", "Make Custom Items Glow", Material.GLOW_INK_SAC));
        inv.addItem(createToggleItem("rules.immortal_item", "Immortal Item", Material.TOTEM_OF_UNDYING));
        inv.addItem(createChestStorageItem());
        inv.addItem(createVanishItem(player));
        inv.addItem(createHelpItem("Inventory See", Material.CHEST, "§7/invsee <type> <player>"));
        inv.addItem(createSpacerMaterial(Material.GRAY_STAINED_GLASS_PANE)); // s.ap placeholder
        inv.addItem(createSpacerMaterial(Material.GRAY_STAINED_GLASS_PANE)); // s.aq placeholder
        inv.addItem(createSingleNumericItem("rules.happyGhastSpeed", "Happy Ghast Speed", Material.GHAST_TEAR, 1.0, 0.05));
        inv.addItem(createSingleNumericItem("rules.death_sound_pitch", "Death Sound Pitch", Material.NOTE_BLOCK, 1.0, 0.1));
        inv.addItem(createClickVillagerItem());
        inv.addItem(createShieldTweaksItem());
        inv.addItem(createToggleItem("rules.drops", "Drops Settings", Material.CHEST_MINECART));
        inv.addItem(createToggleItem("rules.clumps", "Clump Drops", Material.EXPERIENCE_BOTTLE));
        inv.addItem(createHideKillerItem());
        inv.addItem(createSpacerMaterial(Material.GRAY_STAINED_GLASS_PANE)); // s.K placeholder
        inv.addItem(createSubmenuItem("Only one on the server items", Material.GOLD_BLOCK));
        inv.addItem(createHelpItem("First Join Kit", Material.LEATHER_HELMET, "§7/sckit <save/load/join/clear/resetplayers/view> <kitname>"));
        inv.addItem(createWardenHeartItem());
        inv.addItem(createSpacerMaterial(Material.GRAY_STAINED_GLASS_PANE)); // s.ad placeholder
        inv.addItem(createToggleItem("rules.string_dupers", "String Duping", Material.STRING));
    }

    private void drawPage4(Inventory inv) {
        inv.addItem(createToggleItem("rules.disable_vaults", "Disable Vaults hook", Material.IRON_DOOR));
    }
    // Item Creators for Configurations

    private ItemStack createToggleItem(String path, String name, Material mat) {
        boolean val = plugin.getConfig().getBoolean(path, false);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l" + name.toUpperCase());
            meta.setLore(List.of(val ? "§7Status:§a Enabled" : "§7Status:§c Disabled", "", "§eClick to toggle"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSubmenuItem(String title, Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l" + title.toUpperCase());
            meta.setLore(List.of("§7Click to open submenu"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHelpItem(String name, Material mat, String cmd) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l" + name.toUpperCase());
            meta.setLore(List.of(cmd));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSpectatorDeathBanItem() {
        boolean spec = plugin.getConfig().getBoolean("rules.spectator", false);
        boolean ban = plugin.getConfig().getBoolean("rules.death_ban", false);
        ItemStack item = new ItemStack(Material.SKELETON_SKULL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lDEATH BAN");
            meta.setLore(List.of(
                    spec ? "§7SPECTATOR AFTER DEATH:§a True" : "§7SPECTATOR AFTER DEATH:§c False",
                    ban ? "§7BAN AFTER DEATH:§a True" : "§7BAN AFTER DEATH:§c False",
                    "",
                    "§a▌§7▌ §7LMB to Toggle Spectator",
                    "§7▌§a▌ §7RMB to Toggle Death Ban"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDimensionsItem() {
        boolean nether = plugin.getConfig().getBoolean("dimensions.allow_nether", true);
        boolean end = plugin.getConfig().getBoolean("dimensions.allow_end", true);
        ItemStack item = new ItemStack(Material.NETHER_PORTAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lDIMENSIONS");
            meta.setLore(List.of(
                    nether ? "§7ALLOW NETHER:§a True" : "§7ALLOW NETHER:§c False",
                    end ? "§7ALLOW END:§a True" : "§7ALLOW END:§c False",
                    "",
                    "§a▌§7▌ §7LMB to Toggle Nether",
                    "§7▌§a▌ §7RMB to Toggle End"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMaceItem() {
        boolean limit = plugin.getConfig().getBoolean("rules.mace_limit", true);
        boolean ban = plugin.getConfig().getBoolean("rules.ban_mace", false);
        boolean shield = plugin.getConfig().getBoolean("rules.mace_stun_shield", true);
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lMACE SETTINGS");
            meta.setLore(List.of(
                    limit ? "§7MACE LIMIT:§a True" : "§7MACE LIMIT:§c False",
                    ban ? "§7BAN MACE:§a True" : "§7BAN MACE:§c False",
                    shield ? "§7MACE SHIELD STUN:§a True" : "§7MACE SHIELD STUN:§c False",
                    "",
                    "§a▌§7▌ §7LMB to Toggle Mace Limit",
                    "§7▌§a▌ §7RMB to Toggle Ban Mace",
                    "§a◎ §7Drop Key to Toggle Shield Stun"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createChestStorageItem() {
        boolean store = plugin.getConfig().getBoolean("rules.stop_storring_custom_items", false);
        boolean drop = plugin.getConfig().getBoolean("rules.allow_dropping_no_store_items", true);
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lCHEST STORAGE");
            meta.setLore(List.of(
                    store ? "§7STOP CHEST STORE:§a True" : "§7STOP CHEST STORE:§c False",
                    drop ? "§7ALLOW DROP NO-STORE:§a True" : "§7ALLOW DROP NO-STORE:§c False",
                    "",
                    "§a▌§7▌ §7LMB to Toggle Chest Store Option",
                    "§7▌§a▌ §7RMB to Toggle Allow Drop Option"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createVanishItem(Player player) {
        boolean vanished = player.hasMetadata("vanished"); // Simple check or similar placeholder
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lVANISH");
            meta.setLore(List.of(
                    "§a▌§7▌ §7LMB TO VANISH OR UNVANISH",
                    vanished ? "§7You are currently: §aVanished" : "§7You are currently: §cNot vanished",
                    "§7/vanish"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createClickVillagerItem() {
        boolean click = plugin.getConfig().getBoolean("rules.clickVillager", false);
        boolean anchor = plugin.getConfig().getBoolean("rules.clickVillager_anchor", false);
        boolean owner = plugin.getConfig().getBoolean("rules.clickVillager_owner", false);
        ItemStack item = new ItemStack(Material.VILLAGER_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lCLICK VILLAGER");
            meta.setLore(List.of(
                    click ? "§7CLICK VILLAGER:§a Enabled" : "§7CLICK VILLAGER:§c Disabled",
                    anchor ? "§7ANCHOR SYSTEM:§a True" : "§7ANCHOR SYSTEM:§c False",
                    owner ? "§7OWNING SYSTEM:§a True" : "§7OWNING SYSTEM:§c False",
                    "",
                    "§a▌§7▌ §7LMB to Toggle Click Villager",
                    "§7▌§a▌ §7RMB to Toggle Anchor System",
                    "§a◎ §7Drop Key to Toggle Owning System"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createShieldTweaksItem() {
        boolean sfx = plugin.getConfig().getBoolean("shield_tweaks.sound_fix", false);
        boolean tick = plugin.getConfig().getBoolean("shield_tweaks.5_tick_delay_fix", false);
        boolean block = plugin.getConfig().getBoolean("shield_tweaks.skip-vanilla-damage-tick-when-shield-blocked", false);
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lSHIELD TWEAKS");
            meta.setLore(List.of(
                    sfx ? "§7SOUND FIX:§a True" : "§7SOUND FIX:§c False",
                    tick ? "§75 TICK DELAY FIX:§a True" : "§75 TICK DELAY FIX:§c False",
                    block ? "§7SKIP TICK ON BLOCK:§a True" : "§7SKIP TICK ON BLOCK:§c False",
                    "",
                    "§a▌§7▌ §7LMB to Toggle Sound Fix",
                    "§7▌§a▌ §7RMB to Toggle Delay Fix",
                    "§a◎ §7Drop Key to Toggle Skip Tick"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHideKillerItem() {
        boolean killer = plugin.getConfig().getBoolean("rules.hide_killer", false);
        boolean killed = plugin.getConfig().getBoolean("rules.hide_killed", false);
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lDEATH MESSAGES");
            meta.setLore(List.of(
                    killer ? "§7HIDE KILLER NAME:§a True" : "§7HIDE KILLER NAME:§c False",
                    killed ? "§7HIDE KILLED NAME:§a True" : "§7HIDE KILLED NAME:§c False",
                    "",
                    "§a▌§7▌ §7LMB to Toggle Hide Killer",
                    "§7▌§a▌ §7RMB to Toggle Hide Killed"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createWardenHeartItem() {
        boolean warden = plugin.getConfig().getBoolean("rules.warden", false);
        ItemStack item = new ItemStack(Material.WARDEN_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lWARDEN HEART");
            meta.setLore(List.of(
                    warden ? "§7WARDEN DROP HEART:§a Enabled" : "§7WARDEN DROP HEART:§c Disabled",
                    "",
                    "§a▌§7▌ §7LMB to Toggle Drop Heart",
                    "§7▌§a▌ §7RMB to GET WARDEN HEART ITEM"
            ));
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createAntiXrayItem() {
        int xray = plugin.getConfig().getInt("anticheat.anti-xray.enabled", 1);
        ItemStack item = new ItemStack(Material.SMOOTH_STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lANTI-XRAY");
            meta.setLore(List.of(
                    "§7ANTI-XRAY STATUS:§a " + xray,
                    "",
                    "§a▌§7▌ §7LMB to Toggle/Increment (1-3)",
                    "§7▌§a▌ §7RMB to Toggle/Increment (1-3)"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHealthIndicatorsItem() {
        boolean anti = plugin.getConfig().getBoolean("rules.anti_health_indicators", false);
        boolean always = plugin.getConfig().getBoolean("rules.always_health_indicators", false);
        ItemStack item = new ItemStack(Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lHEALTH INDICATORS");
            meta.setLore(List.of(
                    anti ? "§7ANTI HEALTH INDICATORS:§a True" : "§7ANTI HEALTH INDICATORS:§c False",
                    always ? "§7ALWAYS HEALTH INDICATORS:§a True" : "§7ALWAYS HEALTH INDICATORS:§c False",
                    "",
                    "§a▌§7▌ §7LMB to Toggle Anti Indicators",
                    "§7▌§a▌ §7RMB to Toggle Always Indicators"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNumericItem(String path1, String label1, Material mat, String path2, String label2, int def1, int def2) {
        int val1 = plugin.getConfig().getInt(path1, def1);
        int val2 = plugin.getConfig().getInt(path2, def2);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lNUMERIC SETTINGS");
            meta.setLore(List.of(
                    "§7" + label1 + ": §a" + val1,
                    "§7" + label2 + ": §a" + val2,
                    "",
                    "§a▌§7▌ §7LMB to adjust " + label1 + " (+1/-1)",
                    "§7▌§a▌ §7RMB to adjust " + label2 + " (+1/-1)"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSingleNumericItem(String path, String label, Material mat, double def, double step) {
        double val = plugin.getConfig().getDouble(path, def);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l" + label.toUpperCase());
            meta.setLore(List.of(
                    "§7Value: §a" + val,
                    "",
                    "§a▌§7▌ §7LMB to Increase by " + step,
                    "§7▌§a▌ §7RMB to Decrease by " + step
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
    // Inventory Click Handling

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(TITLE) && !title.equals(RITUAL_TITLE) && !title.equals(ONE_TIME_TITLE)
                && !title.equals(ARROW_TITLE) && !title.equals(POTION_TITLE) && !title.equals(ENCHANT_TITLE)) {
            return;
        }

        if (title.equals(RITUAL_TITLE) || title.equals(ONE_TIME_TITLE)) {
            // Let them edit inventory slots 0-52, block slot 53 (back arrow)
            if (event.getSlot() == 53) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();
                player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f, 1.0f);
                openPage(player, 3);
            }
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            if (clicked.getItemMeta() != null && clicked.getItemMeta().getDisplayName().equals(" ")) {
                return;
            }
        }

        player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f, 1.0f);

        // Submenus Click Router
        if (title.equals(ARROW_TITLE)) {
            handleBanArrowClick(clicked, event.getSlot(), player);
            return;
        }
        if (title.equals(POTION_TITLE)) {
            handleBanPotionClick(clicked, event.getSlot(), event.getClick(), player);
            return;
        }
        if (title.equals(ENCHANT_TITLE)) {
            handleEnchantLimitClick(clicked, event.getSlot(), event.getClick(), player);
            return;
        }

        // Get open page number
        ItemStack pageIndicator = event.getInventory().getItem(49);
        int page = 1;
        if (pageIndicator != null && pageIndicator.getItemMeta() != null && pageIndicator.getItemMeta().hasLore()) {
            String pageLore = pageIndicator.getItemMeta().getLore().get(0);
            pageLore = ChatColor.stripColor(pageLore).replace("PAGE: ", "");
            try {
                page = Integer.parseInt(pageLore);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // Page Navigation
        if (event.getSlot() == 48) { // Prev page
            int prevPage = page - 1;
            if (prevPage < 1) prevPage = 4;
            openPage(player, prevPage);
            return;
        }
        if (event.getSlot() == 50) { // Next page
            int nextPage = page + 1;
            if (nextPage > 4) nextPage = 1;
            openPage(player, nextPage);
            return;
        }

        // Handle page specific items
        switch (page) {
            case 1:
                handlePage1Click(event.getSlot(), event.getClick(), event.getAction(), player);
                break;
            case 2:
                handlePage2Click(event.getSlot(), event.getClick(), player);
                break;
            case 3:
                handlePage3Click(event.getSlot(), event.getClick(), player);
                break;
            case 4:
                handlePage4Click(event.getSlot(), player);
                break;
        }
    }

    private void handlePage1Click(int slot, ClickType click, InventoryAction action, Player player) {
        switch (slot) {
            // Toggles
            case 10:
                toggleBoolean("rules.pvp");
                break;
            case 11:
                toggleBoolean("rules.naked_protection");
                break;
            case 12:
                toggleBoolean("rules.afk_protection");
                break;
            case 13:
                toggleBoolean("rules.infinite_restock");
                break;
            case 14:
                toggleBoolean("rules.string_dupers");
                break;

            // Multitoggles / Submenus
            case 19: // Spectator & Death Ban
                if (click.isLeftClick()) {
                    toggleBoolean("rules.spectator");
                } else if (click.isRightClick()) {
                    toggleBoolean("rules.death_ban");
                }
                break;
            case 20: // Dimensions
                if (click.isLeftClick()) {
                    toggleBoolean("dimensions.allow_nether");
                } else if (click.isRightClick()) {
                    toggleBoolean("dimensions.allow_end");
                }
                break;
            case 21: // Mace settings
                if (click.isLeftClick()) {
                    toggleBoolean("rules.mace_limit");
                } else if (click.isRightClick()) {
                    toggleBoolean("rules.ban_mace");
                } else if (action == InventoryAction.DROP_ONE_SLOT || click == ClickType.DROP) {
                    toggleBoolean("rules.mace_stun_shield");
                }
                break;
            case 22:
                toggleBoolean("rules.ban_carts");
                break;
            case 23:
                toggleBoolean("rules.ban_bed_bombing");
                break;
            case 24:
                toggleBoolean("rules.ban_crystal_pvp");
                break;
            case 25:
                toggleBoolean("rules.ban_netherite");
                break;
            case 28:
                toggleBoolean("rules.ban_killing_villagers");
                break;
            case 29:
                openEnchantLimitGUI(player);
                return;
            case 30:
                openBanPotionGUI(player);
                return;
            case 31:
                openBanArrowGUI(player);
                return;
            case 33:
                toggleBoolean("rules.anti_draining");
                break;
            case 34:
                toggleBoolean("rules.ban_breach_swapping");
                break;
            case 37: // Anti xray
                int xray = plugin.getConfig().getInt("anticheat.anti-xray.enabled", 1);
                xray = (xray % 3) + 1;
                plugin.getConfig().set("anticheat.anti-xray.enabled", xray);
                plugin.saveConfig();
                break;
            case 38:
                toggleBoolean("rules.combat_system");
                break;
            case 39: // Health Indicators
                if (click.isLeftClick()) {
                    toggleBoolean("rules.anti_health_indicators");
                } else if (click.isRightClick()) {
                    toggleBoolean("rules.always_health_indicators");
                }
                break;
            case 40:
                toggleBoolean("rules.ban_seed_cracking");
                break;
            case 41:
                toggleBoolean("unsupported-settings.update-equipment-on-player-actions");
                break;
            case 42:
                toggleBoolean("anticheat.obfuscation.items.hide-durability");
                break;
            case 43:
                toggleBoolean("rules.ban_minimap");
                break;
        }

        openPage(player, 1);
    }

    private void handlePage2Click(int slot, ClickType click, Player player) {
        switch (slot) {
            case 10: // Lunge / Spear cooldown
                if (click.isLeftClick()) {
                    adjustInt("rules.lunge_cooldown", 40, 1, click);
                } else if (click.isRightClick()) {
                    adjustInt("rules.spear_cooldown", 20, 1, click);
                }
                break;
            case 11:
                adjustDouble("rules.mace", 0.0, 1.0, click);
                break;
            case 12:
                adjustDouble("rules.shield_cooldown", 60.0, 1.0, click);
                break;
            case 13:
                adjustDouble("rules.ender_pearl", 1.0, 1.0, click);
                break;
            case 14:
                adjustDouble("rules.wind_charge", 0.5, 0.5, click);
                break;
            case 15:
                adjustDouble("rules.trident", 0.0, 1.0, click);
                break;
            case 16:
                adjustDouble("rules.gap", 0.0, 1.0, click);
                break;

            case 19:
                adjustDouble("rules.spear_damage_limiter", 0.0, 1.0, click);
                break;
            case 20:
                adjustDouble("rules.mace_damage_limiter", 0.0, 1.0, click);
                break;
            case 21:
                adjustDouble("rules.cart_damage_limiter", 0.0, 1.0, click);
                break;
            case 22:
                adjustDouble("rules.tnt_damage_limiter", 0.0, 1.0, click);
                break;
            case 23:
                adjustDouble("rules.crystal_damage_limiter", 0.0, 1.0, click);
                break;
            case 24:
                adjustDouble("rules.arrow_damage_limiter", 0.0, 1.0, click);
                break;
            case 25:
                adjustDouble("rules.fall_damage_limiter", 0.0, 1.0, click);
                break;

            case 28: // Extra damage
                adjustDouble("rules.extra_damage", 0.0, 1.0, click);
                break;
        }

        openPage(player, 2);
    }

    private void handlePage3Click(int slot, ClickType click, Player player) {
        switch (slot) {
            case 10: // Ritual items
                openRitualItemsList(player);
                return;
            case 11:
                toggleBoolean("rules.make_custom_items_glow");
                break;
            case 12:
                toggleBoolean("rules.immortal_item");
                break;
            case 13: // Chest storage
                if (click.isLeftClick()) {
                    toggleBoolean("rules.stop_storring_custom_items");
                } else if (click.isRightClick()) {
                    toggleBoolean("rules.allow_dropping_no_store_items");
                }
                break;
            case 14: // Vanish
                player.closeInventory();
                player.performCommand("vanish");
                return;
            case 15: // Invsee info
                break;
            case 18: // happyGhastSpeed
                adjustDouble("rules.happyGhastSpeed", 1.0, 0.05, click);
                break;
            case 19: // death sound pitch
                adjustDouble("rules.death_sound_pitch", 1.0, 0.1, click);
                break;
            case 20: // click villager rules
                if (click.isLeftClick()) {
                    toggleBoolean("rules.clickVillager");
                } else if (click.isRightClick()) {
                    toggleBoolean("rules.clickVillager_anchor");
                } else if (click == ClickType.DROP) {
                    toggleBoolean("rules.clickVillager_owner");
                }
                break;
            case 21: // Shield tweaks
                if (click.isLeftClick()) {
                    toggleBoolean("shield_tweaks.sound_fix");
                } else if (click.isRightClick()) {
                    toggleBoolean("shield_tweaks.5_tick_delay_fix");
                } else if (click == ClickType.DROP) {
                    toggleBoolean("shield_tweaks.skip-vanilla-damage-tick-when-shield-blocked");
                }
                break;
            case 22:
                toggleBoolean("rules.drops");
                break;
            case 23:
                toggleBoolean("rules.clumps");
                break;
            case 24: // Hide killer / Hide killed
                if (click.isLeftClick()) {
                    toggleBoolean("rules.hide_killer");
                } else if (click.isRightClick()) {
                    toggleBoolean("rules.hide_killed");
                }
                break;
            case 26: // One-time craft items
                openOneTimeCraftItemsList(player);
                return;
            case 28: // Warden Heart
                if (click.isLeftClick()) {
                    toggleBoolean("rules.warden");
                } else if (click.isRightClick()) {
                    // Give Warden Heart item
                    player.getInventory().addItem(dev.sculkcore.items.SpecialItems.createWardenDrop());
                    player.sendMessage("§aReceived Warden Heart!");
                }
                break;
            case 30:
                toggleBoolean("rules.string_dupers");
                break;
        }

        openPage(player, 3);
    }

    private void handlePage4Click(int slot, Player player) {
        if (slot == 10) {
            toggleBoolean("rules.disable_vaults");
        }
        openPage(player, 4);
    }
    // Toggle & Adjust Helpers

    private void toggleBoolean(String path) {
        boolean val = plugin.getConfig().getBoolean(path, false);
        plugin.getConfig().set(path, !val);
        plugin.saveConfig();
    }

    private void adjustInt(String path, int def, int step, ClickType click) {
        int val = plugin.getConfig().getInt(path, def);
        if (click.isLeftClick()) {
            val += step;
        } else if (click.isRightClick()) {
            val -= step;
        }
        if (val < 0) val = 0;
        plugin.getConfig().set(path, val);
        plugin.saveConfig();
    }

    private void adjustDouble(String path, double def, double step, ClickType click) {
        double val = plugin.getConfig().getDouble(path, def);
        if (click.isLeftClick()) {
            val += step;
        } else if (click.isRightClick()) {
            val -= step;
        }
        if (val < 0.0) val = 0.0;
        // Round to 2 decimal places to avoid float errors
        val = Math.round(val * 100.0) / 100.0;
        plugin.getConfig().set(path, val);
        plugin.saveConfig();
    }
    // Submenus: Banned Tipped Arrows

    private void openBanArrowGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacySection().deserialize(ARROW_TITLE));
        List<String> banned = plugin.getConfig().getStringList("banned-tipped");
        int slot = 0;
        for (PotionEffectType type : Registry.EFFECT) {
            if (type == null || slot >= 45) break;
            String key = type.getKey().getKey();
            boolean isBanned = banned.contains(key);
            ItemStack item = new ItemStack(Material.TIPPED_ARROW);
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                meta.setDisplayName("§6" + prettify(key));
                meta.setLore(List.of(isBanned ? "§c§lBANNED" : "§a§lNOT BANNED", "", "§eClick to toggle"));
                meta.setColor(isBanned ? Color.RED : Color.LIME);
                meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, key);
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack to Settings", List.of("§7Return to settings")));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
    }

    private void handleBanArrowClick(ItemStack clicked, int slot, Player player) {
        if (slot == 53) {
            openPage(player, 1);
            return;
        }
        if (clicked.getType() != Material.TIPPED_ARROW) return;
        String key = readItemKey(clicked);
        if (key == null) return;
        toggleInList("banned-tipped", key);
        openBanArrowGUI(player);
    }
    // Submenus: Banned Potions

    private void openBanPotionGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacySection().deserialize(POTION_TITLE));
        List<String> tier1 = plugin.getConfig().getStringList("banned-tier1-effects");
        List<String> tier2 = plugin.getConfig().getStringList("banned-tier2-effects");
        int slot = 0;
        for (PotionEffectType type : Registry.EFFECT) {
            if (type == null || slot >= 51) break;
            String key = type.getKey().getKey();
            boolean b1 = tier1.contains(key);
            boolean b2 = tier2.contains(key);
            ItemStack item = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                meta.setDisplayName("§6" + prettify(key));
                meta.setLore(List.of(
                        "§6TIER 1: " + (b1 ? "§c§lBANNED" : "§a§lNOT BANNED"),
                        "§6TIER 2: " + (b2 ? "§c§lBANNED" : "§a§lNOT BANNED"),
                        "",
                        "§eLMB §7toggle Tier 1   §eRMB §7toggle Tier 2"));
                meta.setColor((b1 || b2) ? Color.RED : Color.LIME);
                meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, key);
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }
        inv.setItem(51, createItem(Material.REDSTONE, "§c§lToggle ALL — Tier 1", List.of("§7Ban/unban every effect at Tier 1")));
        inv.setItem(52, createItem(Material.GLOWSTONE_DUST, "§e§lToggle ALL — Tier 2", List.of("§7Ban/unban every effect at Tier 2")));
        inv.setItem(53, createItem(Material.ARROW, "§cBack to Settings", List.of("§7Return to settings")));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
    }

    private void handleBanPotionClick(ItemStack clicked, int slot, ClickType click, Player player) {
        if (slot == 53) {
            openPage(player, 1);
            return;
        }
        if (slot == 51) {
            toggleAllEffects("banned-tier1-effects");
            openBanPotionGUI(player);
            return;
        }
        if (slot == 52) {
            toggleAllEffects("banned-tier2-effects");
            openBanPotionGUI(player);
            return;
        }
        if (clicked.getType() != Material.POTION) return;
        String key = readItemKey(clicked);
        if (key == null) return;
        if (click.isLeftClick()) {
            toggleInList("banned-tier1-effects", key);
        } else if (click.isRightClick()) {
            toggleInList("banned-tier2-effects", key);
        }
        openBanPotionGUI(player);
    }

    private void toggleAllEffects(String path) {
        List<String> list = plugin.getConfig().getStringList(path);
        for (PotionEffectType type : Registry.EFFECT) {
            if (type == null) continue;
            String key = type.getKey().getKey();
            if (list.contains(key)) {
                list.remove(key);
            } else {
                list.add(key);
            }
        }
        plugin.getConfig().set(path, list);
        plugin.saveConfig();
    }
    // Submenus: Enchant Ban

    private void openEnchantLimitGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacySection().deserialize(ENCHANT_TITLE));
        List<Enchantment> ordered = new ArrayList<>();
        ordered.add(Enchantment.SHARPNESS);
        ordered.add(Enchantment.PROTECTION);
        List<Enchantment> rest = new ArrayList<>();
        for (Enchantment ench : Registry.ENCHANTMENT) {
            if (ench == null || ench.equals(Enchantment.SHARPNESS) || ench.equals(Enchantment.PROTECTION)) continue;
            rest.add(ench);
        }
        rest.sort(Comparator.comparing(e -> e.getKey().getKey()));
        ordered.addAll(rest);

        int slot = 0;
        for (Enchantment ench : ordered) {
            if (slot >= 52) break;
            inv.setItem(slot++, buildEnchantItem(ench));
        }
        inv.setItem(52, createItem(Material.BOOK, "§b§lHow it works", List.of(
                "§7LMB §8» §7increase limit by 1",
                "§7RMB §8» §7decrease limit by 1",
                "",
                "§a-1 / ALLOWED §8» §7no limit",
                "§c0 / BANNED §8» §7stripped from items",
                "§en §8» §7capped at level n")));
        inv.setItem(53, createItem(Material.ARROW, "§cBack to Settings", List.of("§7Return to settings")));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
    }

    private ItemStack buildEnchantItem(Enchantment ench) {
        String key = ench.getKey().getKey();
        int max = ench.getMaxLevel();
        int limit = plugin.getConfig().getInt("enchant-limits." + key, -1);
        ItemStack item = new ItemStack(enchantIcon(key));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6" + prettify(key));
            List<String> lore = new ArrayList<>();
            if (limit <= -1) {
                lore.add("§a§lALLOWED");
            } else if (limit == 0) {
                lore.add("§c§lBANNED");
            } else {
                lore.add("§eLimit: §a" + limit + "§7/§e" + max);
            }
            lore.add("");
            lore.add("§a▌§7▌ §7LMB to increase by 1");
            lore.add("§7▌§a▌ §7RMB to decrease by 1");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, key);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void handleEnchantLimitClick(ItemStack clicked, int slot, ClickType click, Player player) {
        if (slot == 53) {
            openPage(player, 1);
            return;
        }
        if (slot == 52) return;
        String key = readItemKey(clicked);
        if (key == null) return;
        Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
        int max = (ench != null) ? ench.getMaxLevel() : 5;
        int limit = plugin.getConfig().getInt("enchant-limits." + key, -1);
        if (click.isLeftClick()) {
            limit = Math.min(max, limit + 1);
        } else if (click.isRightClick()) {
            limit = Math.max(-1, limit - 1);
        } else {
            return;
        }
        plugin.getConfig().set("enchant-limits." + key, limit);
        plugin.saveConfig();
        player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f, 1.0f);
        openEnchantLimitGUI(player);
    }

    private static Material enchantIcon(String key) {
        return switch (key) {
            case "sharpness", "smite", "bane_of_arthropods", "sweeping_edge" -> Material.DIAMOND_SWORD;
            case "protection", "blast_protection", "fire_protection", "projectile_protection" -> Material.DIAMOND_CHESTPLATE;
            case "power", "punch", "flame", "infinity" -> Material.BOW;
            case "multishot", "quick_charge", "piercing" -> Material.CROSSBOW;
            case "efficiency", "fortune", "silk_touch" -> Material.DIAMOND_PICKAXE;
            case "unbreaking", "mending" -> Material.ANVIL;
            case "feather_falling", "depth_strider", "soul_speed" -> Material.DIAMOND_BOOTS;
            case "thorns" -> Material.CACTUS;
            case "knockback" -> Material.PISTON;
            case "fire_aspect" -> Material.FLINT_AND_STEEL;
            case "looting", "luck_of_the_sea", "lure" -> Material.FISHING_ROD;
            case "loyalty", "channeling", "riptide", "impaling" -> Material.TRIDENT;
            case "breach", "density", "wind_burst" -> Material.MACE;
            case "respiration", "aqua_affinity" -> Material.DIAMOND_HELMET;
            case "swift_sneak" -> Material.DIAMOND_LEGGINGS;
            default -> Material.ENCHANTED_BOOK;
        };
    }
    // List Editors: Ritual Items & One-Time Items

    private void openRitualItemsList(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacySection().deserialize(RITUAL_TITLE));
        List<?> list = plugin.getConfig().getList("ritual_items");
        if (list != null) {
            int slot = 0;
            for (Object obj : list) {
                if (slot >= 52) break;
                if (obj instanceof ItemStack item) {
                    inv.setItem(slot++, item);
                }
            }
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack to Settings", List.of("§7Return to settings")));
        player.openInventory(inv);
    }

    private void openOneTimeCraftItemsList(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacySection().deserialize(ONE_TIME_TITLE));
        List<?> list = plugin.getConfig().getList("one_time_craft_items");
        if (list != null) {
            int slot = 0;
            for (Object obj : list) {
                if (slot >= 52) break;
                if (obj instanceof ItemStack item) {
                    inv.setItem(slot++, item);
                }
            }
        }
        inv.setItem(53, createItem(Material.ARROW, "§cBack to Settings", List.of("§7Return to settings")));
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(RITUAL_TITLE) && !title.equals(ONE_TIME_TITLE)) {
            return;
        }

        Inventory inv = event.getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 53; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                items.add(item);
            }
        }

        if (title.equals(RITUAL_TITLE)) {
            plugin.getConfig().set("ritual_items", items);
        } else {
            plugin.getConfig().set("one_time_craft_items", items);
        }
        plugin.saveConfig();
    }
    // Submenu Utilities

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String readItemKey(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    private void toggleInList(String path, String value) {
        List<String> list = plugin.getConfig().getStringList(path);
        if (list.contains(value)) {
            list.remove(value);
        } else {
            list.add(value);
        }
        plugin.getConfig().set(path, list);
        plugin.saveConfig();
    }

    private static String prettify(String key) {
        String s = key.replace('_', ' ');
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

