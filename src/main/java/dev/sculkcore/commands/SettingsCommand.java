package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.GameRule;
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
import org.bukkit.inventory.InventoryHolder;
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
    public static final String COMBAT_TITLE = "§lCombat Options";
    public static final String RITUAL_SETTINGS_TITLE = "§lRitual Duration";
    public static final String GOLDEN_HEAD_TITLE = "§lGolden Head Options";
    public static final String START_OPTIONS_TITLE = "§l/Start Options";
    public static final String START_COMMANDS_TITLE = "§e§l/Start Commands";

    // Cycle lists (from ab/ar reference classes)
    private static final List<String> COMBAT_DISPLAY_CYCLE = List.of("BOSSBAR", "ACTIONBAR", "WEAPON_COOLDOWN", "MESSAGES", "NONE");
    private static final List<String> RITUAL_COLOR_CYCLE = List.of("red", "green", "blue", "yellow", "aqua", "fuchsia", "lime", "orange", "purple", "white", "black", "gray");

    private static SettingsCommand instance;

    // Custom Inventory Holder to identify settings menus without string case/formatting issues
    public static class SettingsInventoryHolder implements InventoryHolder {
        public enum Type {
            MAIN_PAGE_1,
            MAIN_PAGE_2,
            MAIN_PAGE_3,
            MAIN_PAGE_4,
            RITUAL_ITEMS,
            ONE_TIME_CRAFT_ITEMS,
            BANNED_TIPPED_ARROWS,
            POTION_BAN,
            ENCHANT_LIMITS,
            COMBAT_OPTIONS,
            RITUAL_SETTINGS,
            GOLDEN_HEAD,
            START_OPTIONS,
            START_COMMANDS
        }

        private final Type type;

        public SettingsInventoryHolder(Type type) {
            this.type = type;
        }

        public Type getType() {
            return type;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return Bukkit.createInventory(this, 9);
        }
    }

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
        SettingsInventoryHolder.Type type = switch (page) {
            case 1 -> SettingsInventoryHolder.Type.MAIN_PAGE_1;
            case 2 -> SettingsInventoryHolder.Type.MAIN_PAGE_2;
            case 3 -> SettingsInventoryHolder.Type.MAIN_PAGE_3;
            case 4 -> SettingsInventoryHolder.Type.MAIN_PAGE_4;
            default -> SettingsInventoryHolder.Type.MAIN_PAGE_1;
        };

        Inventory inv = Bukkit.createInventory(new SettingsInventoryHolder(type), 54, LegacyComponentSerializer.legacySection().deserialize(TITLE));
        fillBorders(inv);

        // Navigation & Indicators
        inv.setItem(48, createNavButton("§c§lPrevious Page", Material.SPECTRAL_ARROW));
        inv.setItem(49, createPageIndicator(page));
        inv.setItem(50, createNavButton("§a§lNext Page", Material.SPECTRAL_ARROW));

        switch (page) {
            case 1:
                drawPage1(inv, player);
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
            meta.setHideTooltip(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSpacerMaterial(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.setHideTooltip(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavButton(String name, Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setHideTooltip(true);
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

    private void drawPage1(Inventory inv, Player player) {
        // Row 1 (slots 10-16)
        inv.setItem(10, createToggleItem("rules.pvp", "PvP", Material.NETHERITE_SWORD));
        inv.setItem(11, createAntiXrayItem());
        inv.setItem(12, createSingleNumericItem("rules.gap", "Gap Cooldown", Material.GOLDEN_APPLE, 0.0, 1.0));
        inv.setItem(13, createSubmenuItem("Rituals", Material.ECHO_SHARD));
        inv.setItem(14, createSpectatorDeathBanItem());
        inv.setItem(15, createToggleItem("rules.ban_carts", "Ban Carts", Material.TNT_MINECART));
        inv.setItem(16, createToggleItem("rules.ban_bed_bombing", "Ban Bed Bombing", Material.RED_BED));

        // Row 2 (slots 19-25)
        inv.setItem(19, createMaceItem());
        inv.setItem(20, createSingleNumericItem("rules.ender_pearl", "Ender Pearl Cooldown", Material.ENDER_PEARL, 1.0, 1.0));
        inv.setItem(21, createSingleNumericItem("rules.wind_charge", "Wind Charge Cooldown", Material.WIND_CHARGE, 0.5, 0.5));
        inv.setItem(22, createSingleNumericItem("rules.trident", "Trident Cooldown", Material.TRIDENT, 0.0, 1.0));
        inv.setItem(23, createSpearLungeCooldownItem());
        inv.setItem(24, createToggleItem("rules.naked_protection", "Naked Protection", Material.LEATHER_HELMET));
        inv.setItem(25, createToggleItem("rules.afk_protection", "AFK Protection", Material.CLOCK));

        // Row 3 (slots 28-34)
        inv.setItem(28, createCombatRulesItem());
        
        Material goldenSpear = Material.getMaterial("GOLDEN_SPEAR");
        if (goldenSpear == null) goldenSpear = Material.GOLDEN_SWORD;
        inv.setItem(29, createSingleNumericItem("rules.spear_damage_limiter", "Spear Damage Limit", goldenSpear, 0.0, 1.0));
        
        inv.setItem(30, createSingleNumericItem("rules.fall_damage_limiter", "Fall Damage Limit", Material.DIAMOND_CHESTPLATE, 0.0, 1.0));
        inv.setItem(31, createToggleItem("rules.ban_netherite", "Ban Netherite Gear", Material.NETHERITE_INGOT));
        inv.setItem(32, createToggleItem("rules.ban_killing_villagers", "Ban Killing Villagers", Material.VILLAGER_SPAWN_EGG));
        inv.setItem(33, createToggleItem("rules.infinite_restock", "Villager Restock", Material.EMERALD));
        inv.setItem(34, createHealthIndicatorsItem());

        // Row 4 (slots 37-43)
        inv.setItem(37, createToggleItem("rules.make_custom_items_glow", "Make Custom Items Glow", Material.GLOW_ITEM_FRAME));
        inv.setItem(38, createDimensionsItem());
        inv.setItem(39, createToggleItem("rules.clumps", "Clump Drops", Material.EXPERIENCE_BOTTLE));
        inv.setItem(40, createSubmenuItem("Enchant Ban", Material.BOOK));
        inv.setItem(41, createToggleItem("rules.ban_minimap", "Ban Minimap", Material.GREEN_BANNER));
        inv.setItem(42, createOnePlayerSleepItem(player));
        inv.setItem(43, createSubmenuItem("Potion Ban", Material.POTION));
    }

    private void drawPage2(Inventory inv) {
        inv.setItem(10, createSingleNumericItem("rules.shield_cooldown", "Shield Cooldown", Material.SHIELD, 60.0, 1.0));
        inv.setItem(11, createSingleNumericItem("rules.mace", "Mace Cooldown", Material.MACE, 0.0, 1.0));
        inv.setItem(12, createSingleNumericItem("rules.cart_damage_limiter", "Cart Damage Limit", Material.TNT_MINECART, 0.0, 1.0));
        inv.setItem(13, createSingleNumericItem("rules.tnt_damage_limiter", "TNT Damage Limit", Material.TNT, 0.0, 1.0));
        inv.setItem(14, createSingleNumericItem("rules.crystal_damage_limiter", "Crystal Damage Limit", Material.END_CRYSTAL, 0.0, 1.0));
        inv.setItem(15, createSingleNumericItem("rules.arrow_damage_limiter", "Arrow Damage Limit", Material.ARROW, 0.0, 1.0));
        inv.setItem(16, createSingleNumericItem("rules.extra_damage", "Extra Damage", Material.DIAMOND_SWORD, 0.0, 1.0));
    }

    private void drawPage3(Inventory inv, Player player) {
        inv.setItem(10, createHelpItem("Custom Recipes", Material.CRAFTING_TABLE, "§7Manage server custom recipes."));
        inv.setItem(11, createSubmenuItem("Only one on the server items", Material.GOLD_BLOCK));
        inv.setItem(12, createHelpItem("First Join Kit", Material.BUNDLE, "§7/sckit <save/load/join/clear/resetplayers/view> <kitname>"));
        inv.setItem(13, createHelpItem("Start settings", Material.FILLED_MAP, "§7/start to start the SMP event."));
        inv.setItem(14, createVanishItem(player));
        inv.setItem(15, createHelpItem("Inventory See", Material.CHEST, "§7/invsee <type> <player>"));
        inv.setItem(16, createShieldTweaksItem());

        inv.setItem(19, createClickVillagerItem());
        inv.setItem(20, createWardenHeartItem());
        inv.setItem(21, createToggleItem("rules.string_dupers", "String Duping", Material.STRING));
        inv.setItem(22, createToggleItem("rules.drops", "Drops Settings", Material.CHEST_MINECART));
        inv.setItem(23, createHideKillerItem());
        inv.setItem(24, createSingleNumericItem("rules.death_sound_pitch", "Death Sound Pitch", Material.NOTE_BLOCK, 1.0, 0.1));
        inv.setItem(25, createSingleNumericItem("rules.happyGhastSpeed", "Happy Ghast Speed", Material.GHAST_TEAR, 1.0, 0.05));
        
        inv.setItem(28, createToggleItem("rules.immortal_item", "Immortal Item", Material.TOTEM_OF_UNDYING));
        inv.setItem(29, createChestStorageItem());

        // Submenu entry points (slots 31-34)
        inv.setItem(31, createRitualSettingsButton());
        inv.setItem(32, createGoldenHeadButton());
        inv.setItem(33, createStartOptionsButton());
        inv.setItem(34, createSubmenuItem("Combat Options", Material.DIAMOND_AXE));
    }

    private void drawPage4(Inventory inv) {
        inv.setItem(10, createToggleItem("rules.disable_vaults", "Disable Vaults hook", Material.IRON_DOOR));

        // Extra ban rules + paper optimizations (backend syncs on enable)
        inv.setItem(11, createToggleItem("rules.ban_breach_swapping", "Ban Breach Swapping", Material.NETHERITE_CHESTPLATE));
        inv.setItem(12, createToggleItem("rules.anti_draining", "Ban Draining", Material.BUCKET));
        inv.setItem(13, createRestartToggleItem("rules.ban_seed_cracking", "Seed Cracker", Material.WHEAT_SEEDS, true));
        inv.setItem(14, createRestartToggleItem("anticheat.obfuscation.items.hide-durability", "Hide Durability", Material.NETHERITE_HELMET, false));
        inv.setItem(15, createAttributeSwappingItem());
        inv.setItem(16, createLocatorBarItem());
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
        ItemStack item = new ItemStack(Material.END_STONE);
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
        boolean vanished = player.hasMetadata("vanished");
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
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
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
        ItemStack item = new ItemStack(Material.NETHER_STAR);
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

    private ItemStack createSpearLungeCooldownItem() {
        int spear = plugin.getConfig().getInt("rules.spear_cooldown", 20);
        int lunge = plugin.getConfig().getInt("rules.lunge_cooldown", 40);
        Material mat = Material.getMaterial("DIAMOND_SPEAR");
        if (mat == null) mat = Material.PAPER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lSPEAR & LUNGE COOLDOWN");
            meta.setLore(List.of(
                    "§7Spear Cooldown: §a" + spear,
                    "§7Lunge Cooldown: §a" + lunge,
                    "",
                    "§a▌§7▌ §7LMB to adjust Spear Cooldown (+1/-1)",
                    "§7▌§a▌ §7RMB to adjust Lunge Cooldown (+1/-1)"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createOnePlayerSleepItem(Player player) {
        int pct = player.getWorld().getGameRuleValue(GameRule.PLAYERS_SLEEPING_PERCENTAGE);
        boolean val = (pct == 1);
        ItemStack item = new ItemStack(Material.WHITE_BED);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lONE PLAYER SLEEP");
            meta.setLore(List.of(val ? "§7Status:§a Enabled" : "§7Status:§c Disabled", "", "§eClick to toggle"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void toggleOnePlayerSleep(Player player) {
        org.bukkit.World world = player.getWorld();
        Integer pct = world.getGameRuleValue(GameRule.PLAYERS_SLEEPING_PERCENTAGE);
        if (pct == null) pct = 100;
        int newPct = (pct == 1) ? 100 : 1;
        world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, newPct);
    }

    private ItemStack createCombatRulesItem() {
        boolean on = plugin.getConfig().getBoolean("rules.combat_system", true);
        ItemStack item = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lCOMBAT RULES");
            meta.setLore(List.of(
                    on ? "§7COMBAT LOG SYSTEM:§a Enabled" : "§7COMBAT LOG SYSTEM:§c Disabled",
                    "",
                    "§a▌§7▌ §7LMB to toggle the combat system",
                    "§7▌§a▌ §7RMB for more options"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRitualSettingsButton() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lRITUAL SETTINGS");
            meta.setLore(List.of(
                    "§7Duration: §a" + plugin.getConfig().getInt("config.ritual_duration", 60) + "s",
                    "§7Radius: §a" + plugin.getConfig().getInt("config.ritual_radius", 5),
                    "§7Color: §a" + capitalize(plugin.getConfig().getString("config.ritual_particle_color", "white")),
                    "",
                    "§eClick to open ritual settings"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createGoldenHeadButton() {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lGOLDEN HEAD OPTIONS");
            meta.setLore(List.of(
                    "§7Configure the §6Golden Head §7effects",
                    "",
                    "§eClick to open golden head options"
            ));
            meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createStartOptionsButton() {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lSTART OPTIONS");
            meta.setLore(List.of(
                    "§7Border, grace, launch & start commands",
                    "",
                    "§eClick to open /start options"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Toggle item that warns a restart is required (paper-sync features). */
    private ItemStack createRestartToggleItem(String path, String name, Material mat, boolean def) {
        boolean val = plugin.getConfig().getBoolean(path, def);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l" + name.toUpperCase());
            meta.setLore(List.of(
                    val ? "§7Status:§a Enabled" : "§7Status:§c Disabled",
                    "",
                    "§eClick to toggle",
                    "§cNOTE: requires a server restart to take effect"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createAttributeSwappingItem() {
        // Display is inverted: enabled when the paper auto-update flag is FALSE
        boolean autoUpdate = plugin.getConfig().getBoolean("unsupported-settings.update-equipment-on-player-actions", false);
        boolean enabled = !autoUpdate;
        ItemStack item = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lATTRIBUTE SWAPPING");
            meta.setLore(List.of(
                    enabled ? "§7Status:§a Enabled" : "§7Status:§c Disabled",
                    "",
                    "§eClick to toggle",
                    "§7- Server must be Paper",
                    "§cNOTE: requires a server restart to take effect"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLocatorBarItem() {
        boolean val = false;
        GameRule<Boolean> locator = resolveLocatorBarRule();
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lLOCATOR BAR");
            if (locator == null) {
                meta.setLore(List.of("§cUnsupported on this server version", "§7(requires 1.21.6+)"));
            } else {
                meta.setLore(List.of(val ? "§7Status:§a Enabled" : "§7Status:§c Disabled", "", "§eClick to toggle"));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    private static GameRule<Boolean> resolveLocatorBarRule() {
        try {
            GameRule<?> rule = GameRule.getByName("locatorBar");
            return (GameRule<Boolean>) rule;
        } catch (Throwable t) {
            return null;
        }
    }

    private ItemStack createSingleNumericItem(String path, String label, Material mat, double def, double step) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l" + label.toUpperCase());
            boolean isInt = (path.contains("cooldown") || path.contains("limit") || path.equals("rules.gap") || path.equals("rules.mace")) && !path.contains("wind_charge");
            if (isInt) {
                int val = plugin.getConfig().getInt(path, (int) def);
                meta.setLore(List.of(
                        "§7Value: §a" + val,
                        "",
                        "§a▌§7▌ §7LMB to Increase by " + (int) step,
                        "§7▌§a▌ §7RMB to Decrease by " + (int) step
                ));
            } else {
                double val = plugin.getConfig().getDouble(path, def);
                meta.setLore(List.of(
                        "§7Value: §a" + val,
                        "",
                        "§a▌§7▌ §7LMB to Increase by " + step,
                        "§7▌§a▌ §7RMB to Decrease by " + step
                ));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    // Inventory Click Handling

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder rawHolder = event.getInventory().getHolder();
        if (!(rawHolder instanceof SettingsInventoryHolder holder)) {
            return;
        }

        SettingsInventoryHolder.Type type = holder.getType();

        if (type == SettingsInventoryHolder.Type.RITUAL_ITEMS || type == SettingsInventoryHolder.Type.ONE_TIME_CRAFT_ITEMS) {
            // Let them edit slots 0-52, block slot 53 (back button)
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
        if (type == SettingsInventoryHolder.Type.BANNED_TIPPED_ARROWS) {
            handleBanArrowClick(clicked, event.getSlot(), player);
            return;
        }
        if (type == SettingsInventoryHolder.Type.POTION_BAN) {
            handleBanPotionClick(clicked, event.getSlot(), event.getClick(), player);
            return;
        }
        if (type == SettingsInventoryHolder.Type.ENCHANT_LIMITS) {
            handleEnchantLimitClick(clicked, event.getSlot(), event.getClick(), player);
            return;
        }
        if (type == SettingsInventoryHolder.Type.COMBAT_OPTIONS) {
            handleCombatOptionsClick(event.getSlot(), event.getClick(), player);
            return;
        }
        if (type == SettingsInventoryHolder.Type.RITUAL_SETTINGS) {
            handleRitualSettingsClick(event.getSlot(), event.getClick(), player);
            return;
        }
        if (type == SettingsInventoryHolder.Type.GOLDEN_HEAD) {
            handleGoldenHeadClick(event.getSlot(), event.getClick(), player);
            return;
        }
        if (type == SettingsInventoryHolder.Type.START_OPTIONS) {
            handleStartOptionsClick(event.getSlot(), event.getClick(), player);
            return;
        }
        if (type == SettingsInventoryHolder.Type.START_COMMANDS) {
            handleStartCommandsClick(clicked, event.getSlot(), event.getClick(), player);
            return;
        }

        // Main Page Router
        int page = switch (type) {
            case MAIN_PAGE_1 -> 1;
            case MAIN_PAGE_2 -> 2;
            case MAIN_PAGE_3 -> 3;
            case MAIN_PAGE_4 -> 4;
            default -> 1;
        };

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
            // Row 1 (slots 10-16)
            case 10:
                toggleBoolean("rules.pvp");
                break;
            case 11:
                int xray = plugin.getConfig().getInt("anticheat.anti-xray.enabled", 1);
                xray = (xray % 3) + 1;
                plugin.getConfig().set("anticheat.anti-xray.enabled", xray);
                plugin.saveConfig();
                break;
            case 12:
                adjustInt("rules.gap", 0, 1, click);
                break;
            case 13:
                openRitualItemsList(player);
                return;
            case 14:
                if (click.isLeftClick()) {
                    toggleBoolean("rules.spectator");
                } else if (click.isRightClick()) {
                    toggleBoolean("rules.death_ban");
                }
                break;
            case 15:
                toggleBoolean("rules.ban_carts");
                break;
            case 16:
                toggleBoolean("rules.ban_bed_bombing");
                break;

            // Row 2 (slots 19-25)
            case 19:
                if (click.isLeftClick()) {
                    toggleBoolean("rules.mace_limit");
                } else if (click.isRightClick()) {
                    toggleBoolean("rules.ban_mace");
                } else if (action == InventoryAction.DROP_ONE_SLOT || click == ClickType.DROP) {
                    toggleBoolean("rules.mace_stun_shield");
                }
                break;
            case 20:
                adjustInt("rules.ender_pearl", 15, 1, click);
                break;
            case 21:
                adjustDouble("rules.wind_charge", 0.5, 0.5, click);
                break;
            case 22:
                adjustInt("rules.trident", 0, 1, click);
                break;
            case 23:
                if (click.isShiftClick()) {
                    adjustInt("rules.lunge_cooldown", 40, 1, click);
                } else {
                    adjustInt("rules.spear_cooldown", 20, 1, click);
                }
                break;
            case 24:
                toggleBoolean("rules.naked_protection");
                break;
            case 25:
                toggleBoolean("rules.afk_protection");
                break;

            // Row 3 (slots 28-34)
            case 28:
                if (click.isRightClick()) {
                    openCombatOptions(player);
                    return;
                }
                toggleBoolean("rules.combat_system");
                break;
            case 29:
                adjustInt("rules.spear_damage_limiter", 0, 1, click);
                break;
            case 30:
                adjustDouble("rules.fall_damage_limiter", 0.0, 1.0, click);
                break;
            case 31:
                toggleBoolean("rules.ban_netherite");
                break;
            case 32:
                toggleBoolean("rules.ban_killing_villagers");
                break;
            case 33:
                toggleBoolean("rules.infinite_restock");
                break;
            case 34:
                if (click.isLeftClick()) {
                    toggleBoolean("rules.anti_health_indicators");
                } else if (click.isRightClick()) {
                    toggleBoolean("rules.always_health_indicators");
                }
                break;

            // Row 4 (slots 37-43)
            case 37:
                toggleBoolean("rules.make_custom_items_glow");
                break;
            case 38:
                if (click.isLeftClick()) {
                    toggleBoolean("dimensions.allow_nether");
                } else if (click.isRightClick()) {
                    toggleBoolean("dimensions.allow_end");
                }
                break;
            case 39:
                toggleBoolean("rules.clumps");
                break;
            case 40:
                openEnchantLimitGUI(player);
                return;
            case 41:
                toggleBoolean("rules.ban_minimap");
                break;
            case 42:
                toggleOnePlayerSleep(player);
                break;
            case 43:
                openBanPotionGUI(player);
                return;
        }

        openPage(player, 1);
    }

    private void handlePage2Click(int slot, ClickType click, Player player) {
        switch (slot) {
            case 10:
                adjustInt("rules.shield_cooldown", 60, 1, click);
                break;
            case 11:
                adjustInt("rules.mace", 0, 1, click);
                break;
            case 12:
                adjustInt("rules.cart_damage_limiter", 0, 1, click);
                break;
            case 13:
                adjustInt("rules.tnt_damage_limiter", 0, 1, click);
                break;
            case 14:
                adjustInt("rules.crystal_damage_limiter", 0, 1, click);
                break;
            case 15:
                adjustInt("rules.arrow_damage_limiter", 0, 1, click);
                break;
            case 16:
                adjustDouble("rules.extra_damage", 0.0, 1.0, click);
                break;
        }

        openPage(player, 2);
    }

    private void handlePage3Click(int slot, ClickType click, Player player) {
        switch (slot) {
            case 11: // One-time craft items
                openOneTimeCraftItemsList(player);
                return;
            case 14: // Vanish
                player.closeInventory();
                player.performCommand("vanish");
                return;
            case 16: // Shield tweaks
                if (click.isLeftClick()) {
                    toggleBoolean("shield_tweaks.sound_fix");
                } else if (click.isRightClick()) {
                    toggleBoolean("shield_tweaks.5_tick_delay_fix");
                } else if (click == ClickType.DROP) {
                    toggleBoolean("shield_tweaks.skip-vanilla-damage-tick-when-shield-blocked");
                }
                break;
            case 19: // click villager rules
                if (click.isLeftClick()) {
                    toggleBoolean("rules.clickVillager");
                } else if (click.isRightClick()) {
                    toggleBoolean("rules.clickVillager_anchor");
                } else if (click == ClickType.DROP) {
                    toggleBoolean("rules.clickVillager_owner");
                }
                break;
            case 20: // Warden Heart
                if (click.isLeftClick()) {
                    toggleBoolean("rules.warden");
                } else if (click.isRightClick()) {
                    player.getInventory().addItem(dev.sculkcore.items.SpecialItems.createWardenDrop());
                    player.sendMessage("§aReceived Warden Heart!");
                }
                break;
            case 21: // String Dupers
                toggleBoolean("rules.string_dupers");
                break;
            case 22: // Drops settings
                toggleBoolean("rules.drops");
                break;
            case 23: // Hide killer / Hide killed
                if (click.isLeftClick()) {
                    toggleBoolean("rules.hide_killer");
                } else if (click.isRightClick()) {
                    toggleBoolean("rules.hide_killed");
                }
                break;
            case 24: // death sound pitch
                adjustDouble("rules.death_sound_pitch", 1.0, 0.1, click);
                break;
            case 25: // happyGhastSpeed
                adjustDouble("rules.happyGhastSpeed", 1.0, 0.05, click);
                break;
            case 28: // Immortal Item
                toggleBoolean("rules.immortal_item");
                break;
            case 29: // Chest storage
                if (click.isLeftClick()) {
                    toggleBoolean("rules.stop_storring_custom_items");
                } else if (click.isRightClick()) {
                    toggleBoolean("rules.allow_dropping_no_store_items");
                }
                break;
            case 31: // Ritual settings submenu
                openRitualSettings(player);
                return;
            case 32: // Golden head options submenu
                openGoldenHeadOptions(player);
                return;
            case 33: // Start options submenu
                openStartOptions(player);
                return;
            case 34: // Combat options submenu
                openCombatOptions(player);
                return;
        }

        openPage(player, 3);
    }

    private void handlePage4Click(int slot, Player player) {
        switch (slot) {
            case 10 -> toggleBoolean("rules.disable_vaults");
            case 11 -> toggleBoolean("rules.ban_breach_swapping");
            case 12 -> toggleBoolean("rules.anti_draining");
            case 13 -> {
                toggleBoolean("rules.ban_seed_cracking");
                player.sendMessage("§eSeed Cracker setting changed — restart the server for it to take effect.");
            }
            case 14 -> {
                toggleBoolean("anticheat.obfuscation.items.hide-durability");
                player.sendMessage("§eHide Durability changed — it will sync to paper-world-defaults.yml on next restart.");
            }
            case 15 -> {
                // Inverted: toggling "attribute swapping on" means setting the paper auto-update flag off
                boolean autoUpdate = plugin.getConfig().getBoolean("unsupported-settings.update-equipment-on-player-actions", false);
                plugin.getConfig().set("unsupported-settings.update-equipment-on-player-actions", !autoUpdate);
                plugin.saveConfig();
                player.sendMessage("§eAttribute Swapping changed — it will sync to paper-global.yml on next restart.");
            }
            case 16 -> toggleLocatorBar(player);
        }
        openPage(player, 4);
    }

    private void toggleLocatorBar(Player player) {
        GameRule<Boolean> rule = resolveLocatorBarRule();
        if (rule == null) {
            player.sendMessage("§cLocator Bar requires a 1.21.6+ server.");
            return;
        }
        org.bukkit.World world = player.getWorld();
        Boolean cur = world.getGameRuleValue(rule);
        boolean newVal = (cur == null) || !cur;
        world.setGameRule(rule, newVal);
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
        val = Math.round(val * 100.0) / 100.0;
        plugin.getConfig().set(path, val);
        plugin.saveConfig();
    }

    // Submenus: Banned Tipped Arrows

    private void openBanArrowGUI(Player player) {
        Inventory inv = Bukkit.createInventory(new SettingsInventoryHolder(SettingsInventoryHolder.Type.BANNED_TIPPED_ARROWS), 54, LegacyComponentSerializer.legacySection().deserialize(ARROW_TITLE));
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
        Inventory inv = Bukkit.createInventory(new SettingsInventoryHolder(SettingsInventoryHolder.Type.POTION_BAN), 54, LegacyComponentSerializer.legacySection().deserialize(POTION_TITLE));
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
        Inventory inv = Bukkit.createInventory(new SettingsInventoryHolder(SettingsInventoryHolder.Type.ENCHANT_LIMITS), 54, LegacyComponentSerializer.legacySection().deserialize(ENCHANT_TITLE));
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
        Inventory inv = Bukkit.createInventory(new SettingsInventoryHolder(SettingsInventoryHolder.Type.RITUAL_ITEMS), 54, LegacyComponentSerializer.legacySection().deserialize(RITUAL_TITLE));
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
        Inventory inv = Bukkit.createInventory(new SettingsInventoryHolder(SettingsInventoryHolder.Type.ONE_TIME_CRAFT_ITEMS), 54, LegacyComponentSerializer.legacySection().deserialize(ONE_TIME_TITLE));
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
        InventoryHolder rawHolder = event.getInventory().getHolder();
        if (!(rawHolder instanceof SettingsInventoryHolder holder)) {
            return;
        }

        SettingsInventoryHolder.Type type = holder.getType();
        if (type != SettingsInventoryHolder.Type.RITUAL_ITEMS && type != SettingsInventoryHolder.Type.ONE_TIME_CRAFT_ITEMS) {
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

        if (type == SettingsInventoryHolder.Type.RITUAL_ITEMS) {
            plugin.getConfig().set("ritual_items", items);
        } else {
            plugin.getConfig().set("one_time_craft_items", items);
        }
        plugin.saveConfig();
    }

    // ============================================================
    // Submenu: Combat Options (ref class ab)
    // ============================================================

    private void openCombatOptions(Player player) {
        Inventory inv = Bukkit.createInventory(new SettingsInventoryHolder(SettingsInventoryHolder.Type.COMBAT_OPTIONS), 27, LegacyComponentSerializer.legacySection().deserialize(COMBAT_TITLE));
        for (int i = 0; i < 27; i++) inv.setItem(i, createSpacer());

        int tagTime = plugin.getConfig().getInt("config.combat_tag_time", 30);
        inv.setItem(10, createItem(Material.CLOCK, "§6§lCOMBAT TAG TIME", List.of(
                "§7Time: §a" + formatTime(tagTime),
                "",
                "§a▌§7▌ §7LMB to increase by 1s",
                "§7▌§a▌ §7RMB to decrease by 1s",
                "§7SHIFT to change by 10s")));

        boolean noRestock = plugin.getConfig().getBoolean("config.combat_log_no_restock", false);
        inv.setItem(11, createItem(Material.CHEST, "§6§lBAN RESTOCKING WHILE IN COMBAT", List.of(
                noRestock ? "§7Status:§a Enabled" : "§7Status:§c Disabled", "", "§eClick to toggle")));

        String logMode = plugin.getConfig().getString("config.combat_log_mode", "ANNOUNCE");
        inv.setItem(12, createItem(Material.REDSTONE, "§6§lCOMBAT LOG PUNISHMENT", List.of(
                "§7Mode: §a" + logMode, "", "§7LMB TO CYCLE §8(ANNOUNCE / KILL)")));

        String display = plugin.getConfig().getString("config.combat_display", "BOSSBAR");
        inv.setItem(13, createItem(Material.ITEM_FRAME, "§6§lCOMBAT TIMER", List.of(
                "§7Shown as: §a" + display,
                "§7Where it should be shown",
                "",
                "§7LMB TO CYCLE")));

        boolean banTrident = plugin.getConfig().getBoolean("config.combat_trident", false);
        inv.setItem(14, createItem(Material.TRIDENT, "§6§lBAN RIPTIDE WHILE IN COMBAT", List.of(
                banTrident ? "§7Status:§a Enabled" : "§7Status:§c Disabled", "", "§eClick to toggle")));

        int minDmg = plugin.getConfig().getInt("config.combat_damage", 1);
        inv.setItem(15, createItem(Material.DIAMOND_SWORD, "§6§lMINIMUM DAMAGE TO TAG", List.of(
                "§7Damage: §a" + minDmg,
                "§7Players need to deal at least this to combat tag",
                "",
                "§a▌§7▌ §7LMB to increase by 1",
                "§7▌§a▌ §7RMB to decrease by 1")));

        boolean banElytra = plugin.getConfig().getBoolean("config.combat_elytra", false);
        inv.setItem(16, createItem(Material.ELYTRA, "§6§lBAN ELYTRA WHILE IN COMBAT", List.of(
                banElytra ? "§7Status:§a Enabled" : "§7Status:§c Disabled", "", "§eClick to toggle")));

        inv.setItem(26, createItem(Material.ARROW, "§cBack to Settings", List.of("§7Return to settings")));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
    }

    private void handleCombatOptionsClick(int slot, ClickType click, Player player) {
        switch (slot) {
            case 10 -> {
                int step = click.isShiftClick() ? 10 : 1;
                adjustIntStep("config.combat_tag_time", 30, step, click);
            }
            case 11 -> toggleBoolean("config.combat_log_no_restock");
            case 12 -> cycleString("config.combat_log_mode", List.of("ANNOUNCE", "KILL"), "ANNOUNCE");
            case 13 -> cycleString("config.combat_display", COMBAT_DISPLAY_CYCLE, "BOSSBAR");
            case 14 -> toggleBoolean("config.combat_trident");
            case 15 -> adjustIntStep("config.combat_damage", 1, 1, click);
            case 16 -> toggleBoolean("config.combat_elytra");
            case 26 -> { openPage(player, 1); return; }
            default -> { return; }
        }
        openCombatOptions(player);
    }

    // ============================================================
    // Submenu: Ritual Settings (ref class ar)
    // ============================================================

    private void openRitualSettings(Player player) {
        Inventory inv = Bukkit.createInventory(new SettingsInventoryHolder(SettingsInventoryHolder.Type.RITUAL_SETTINGS), org.bukkit.event.inventory.InventoryType.HOPPER, LegacyComponentSerializer.legacySection().deserialize(RITUAL_SETTINGS_TITLE));

        int radius = plugin.getConfig().getInt("config.ritual_radius", 5);
        inv.setItem(0, createItem(Material.SNOWBALL, "§6§lRITUAL RADIUS", List.of(
                "§7Radius: §a" + radius,
                "",
                "§a▌§7▌ §7LMB to increase by 1",
                "§7▌§a▌ §7RMB to decrease by 1",
                "§7SHIFT to change by 10")));

        int duration = plugin.getConfig().getInt("config.ritual_duration", 60);
        inv.setItem(1, createItem(Material.CLOCK, "§6§lRITUAL DURATION", List.of(
                "§7Duration: §a" + formatTime(duration),
                "",
                "§a▌§7▌ §7LMB to increase by 1m",
                "§7▌§a▌ §7RMB to decrease by 1m",
                "§7SHIFT to change by 10m")));

        String color = plugin.getConfig().getString("config.ritual_particle_color", "white");
        inv.setItem(2, createItem(Material.INK_SAC, "§6§lRITUAL PARTICLE COLOR", List.of(
                "§7Color: §a" + capitalize(color), "", "§7LMB TO CYCLE")));

        inv.setItem(4, createItem(Material.ARROW, "§cBack", List.of("§7Return to settings")));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
    }

    private void handleRitualSettingsClick(int slot, ClickType click, Player player) {
        switch (slot) {
            case 0 -> adjustIntStep("config.ritual_radius", 5, click.isShiftClick() ? 10 : 1, click);
            case 1 -> adjustIntStep("config.ritual_duration", 60, click.isShiftClick() ? 600 : 60, click);
            case 2 -> cycleString("config.ritual_particle_color", RITUAL_COLOR_CYCLE, "white");
            case 4 -> { openPage(player, 3); return; }
            default -> { return; }
        }
        openRitualSettings(player);
    }

    // ============================================================
    // Submenu: Golden Head Options (ref class a_)
    // ============================================================

    private void openGoldenHeadOptions(Player player) {
        Inventory inv = Bukkit.createInventory(new SettingsInventoryHolder(SettingsInventoryHolder.Type.GOLDEN_HEAD), 27, LegacyComponentSerializer.legacySection().deserialize(GOLDEN_HEAD_TITLE));
        for (int i = 0; i < 27; i++) inv.setItem(i, createSpacer());

        inv.setItem(10, goldenHeadItem(Material.FEATHER, "§6§lSPEED",
                "goldenhead.speed_duration", 0, "goldenhead.speed_amplifier", 0, "§7SHIFT FOR AMPLIFIER"));
        inv.setItem(11, goldenHeadItem(Material.GHAST_TEAR, "§6§lREGENERATION",
                "goldenhead.regeneration_duration", 10, "goldenhead.regeneration_amplifier", 2, "§7SHIFT FOR AMPLIFIER"));
        inv.setItem(12, goldenHeadItem(Material.GOLDEN_APPLE, "§6§lABSORPTION",
                "goldenhead.absorption_duration", 120, "goldenhead.absorption_amplifier", 1, "§7SHIFT FOR AMPLIFIER"));
        inv.setItem(13, goldenHeadItem(Material.CLOCK, "§6§lCOOLDOWN AND CONSUME TIME",
                "goldenhead.cooldown", 10, "goldenhead.consume_seconds", 1, "§7SHIFT FOR CONSUME TIME"));

        inv.setItem(26, createItem(Material.ARROW, "§6§lGO BACK", List.of("§7Return to settings")));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
    }

    private ItemStack goldenHeadItem(Material mat, String name, String durationKey, int durDef, String ampKey, int ampDef, String shiftHint) {
        int duration = plugin.getConfig().getInt(durationKey, durDef);
        int amp = plugin.getConfig().getInt(ampKey, ampDef);
        boolean isCooldown = name.contains("COOLDOWN");
        return createItem(mat, name, List.of(
                (isCooldown ? "§7COOLDOWN: §a" : "§7DURATION: §a") + duration,
                (isCooldown ? "§7CONSUME TIME: §a" : "§7AMPLIFIER: §a") + amp,
                "",
                "§a▌§7▌ §7LMB to increase by 1",
                "§7▌§a▌ §7RMB to decrease by 1",
                shiftHint));
    }

    private void handleGoldenHeadClick(int slot, ClickType click, Player player) {
        String durationKey;
        int durDef;
        String ampKey;
        int ampDef;
        switch (slot) {
            case 10 -> { durationKey = "goldenhead.speed_duration"; durDef = 0; ampKey = "goldenhead.speed_amplifier"; ampDef = 0; }
            case 11 -> { durationKey = "goldenhead.regeneration_duration"; durDef = 10; ampKey = "goldenhead.regeneration_amplifier"; ampDef = 2; }
            case 12 -> { durationKey = "goldenhead.absorption_duration"; durDef = 120; ampKey = "goldenhead.absorption_amplifier"; ampDef = 1; }
            case 13 -> { durationKey = "goldenhead.cooldown"; durDef = 10; ampKey = "goldenhead.consume_seconds"; ampDef = 1; }
            case 26 -> { openPage(player, 3); return; }
            default -> { return; }
        }
        String target = click.isShiftClick() ? ampKey : durationKey;
        int def = click.isShiftClick() ? ampDef : durDef;
        adjustIntStep(target, def, 1, click);
        openGoldenHeadOptions(player);
    }

    // ============================================================
    // Submenu: /Start Options (ref class bq)
    // ============================================================

    private void openStartOptions(Player player) {
        Inventory inv = Bukkit.createInventory(new SettingsInventoryHolder(SettingsInventoryHolder.Type.START_OPTIONS), 9, LegacyComponentSerializer.legacySection().deserialize(START_OPTIONS_TITLE));

        int beforeBorder = plugin.getConfig().getInt("config.before_start_border", 100);
        inv.setItem(0, createItem(Material.DIAMOND_HELMET, "§6§lBEFORE START BORDER SIZE", List.of(
                "§7Size: §a" + beforeBorder,
                "§7If border ≤ this, players take no hunger/damage",
                "",
                "§a▌§7▌ §7LMB +1   §7▌§a▌ §7RMB -1   §7SHIFT ±10")));

        boolean immunity = plugin.getConfig().getBoolean("config.immunity_during_grace", true);
        inv.setItem(1, createItem(Material.DIAMOND_SWORD, "§6§lGRACE IMMUNITY", List.of(
                immunity ? "§7Immunity during grace:§a True" : "§7Immunity during grace:§c False",
                "§cNOTE: during grace PvP is off in both cases",
                "",
                "§eClick to toggle")));

        int startBorder = plugin.getConfig().getInt("config.start_border_size", 2000);
        inv.setItem(2, createItem(Material.BARRIER, "§6§lSTART BORDER SIZE", List.of(
                "§7Size: §a" + startBorder,
                "§7Border size right after /start",
                "",
                "§a▌§7▌ §7LMB +100   §7▌§a▌ §7RMB -100   §7SHIFT ±1000")));

        int steak = plugin.getConfig().getInt("config.start_steak", 0);
        inv.setItem(3, createItem(Material.COOKED_BEEF, "§6§lSTART STEAK", List.of(
                "§7Steak given: §a" + steak,
                "",
                "§a▌§7▌ §7LMB +1   §7▌§a▌ §7RMB -1   §7SHIFT ±16")));

        boolean launch = plugin.getConfig().getBoolean("config.launch", true);
        inv.setItem(4, createItem(Material.ELYTRA, "§6§lLAUNCH & GLIDE", List.of(
                launch ? "§7Enabled:§a True" : "§7Enabled:§c False",
                "§7On /start all players are launched into the air",
                "",
                "§eClick to toggle")));

        List<String> cmds = plugin.getConfig().getStringList("config.start_commands");
        inv.setItem(5, createItem(Material.COMMAND_BLOCK_MINECART, "§6§lSTART COMMANDS", List.of(
                "§7Commands run on /start: §a" + cmds.size(),
                "",
                "§7LMB TO EDIT")));

        inv.setItem(8, createItem(Material.ARROW, "§cBack", List.of("§7Return to settings")));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.0f);
    }

    private void handleStartOptionsClick(int slot, ClickType click, Player player) {
        switch (slot) {
            case 0 -> adjustIntStep("config.before_start_border", 100, click.isShiftClick() ? 10 : 1, click);
            case 1 -> toggleBoolean("config.immunity_during_grace");
            case 2 -> adjustIntStep("config.start_border_size", 2000, click.isShiftClick() ? 1000 : 100, click);
            case 3 -> adjustIntStep("config.start_steak", 0, click.isShiftClick() ? 16 : 1, click);
            case 4 -> toggleBoolean("config.launch");
            case 5 -> { openStartCommands(player); return; }
            case 8 -> { openPage(player, 3); return; }
            default -> { return; }
        }
        openStartOptions(player);
    }

    private void openStartCommands(Player player) {
        Inventory inv = Bukkit.createInventory(new SettingsInventoryHolder(SettingsInventoryHolder.Type.START_COMMANDS), 54, LegacyComponentSerializer.legacySection().deserialize(START_COMMANDS_TITLE));
        List<String> cmds = plugin.getConfig().getStringList("config.start_commands");
        int slot = 0;
        for (String cmd : cmds) {
            if (slot >= 53) break;
            inv.setItem(slot++, createItem(Material.COMMAND_BLOCK_MINECART, "§6" + cmd, List.of(
                    "§a▌§7▌ §7LMB to edit", "§7▌§a▌ §7RMB to delete")));
        }
        inv.setItem(53, createItem(Material.GREEN_STAINED_GLASS_PANE, "§a§lCreate a new command", List.of("§7Adds a 'New Command' entry")));
        player.openInventory(inv);
    }

    private void handleStartCommandsClick(ItemStack clicked, int slot, ClickType click, Player player) {
        List<String> cmds = new ArrayList<>(plugin.getConfig().getStringList("config.start_commands"));
        if (slot == 53) {
            cmds.add("New Command");
            plugin.getConfig().set("config.start_commands", cmds);
            plugin.saveConfig();
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            openStartCommands(player);
            return;
        }
        if (clicked == null || clicked.getType() != Material.COMMAND_BLOCK_MINECART) return;
        if (slot < 0 || slot >= cmds.size()) return;
        if (click.isRightClick()) {
            cmds.remove(slot);
            plugin.getConfig().set("config.start_commands", cmds);
            plugin.saveConfig();
            openStartCommands(player);
        } else if (click.isLeftClick()) {
            // Edit via chat prompt (AnvilGUI not required)
            player.closeInventory();
            player.sendMessage("§eEditing start command #" + (slot + 1) + ": §7" + cmds.get(slot));
            player.sendMessage("§7Type the new command in chat (without leading /), or 'cancel'.");
            startCommandEdits.put(player.getUniqueId(), slot);
        }
    }

    // Chat-based start command editor state
    private final java.util.Map<java.util.UUID, Integer> startCommandEdits = new java.util.HashMap<>();

    @EventHandler
    public void onChatEditStartCommand(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Integer index = startCommandEdits.remove(player.getUniqueId());
        if (index == null) return;
        event.setCancelled(true);
        String msg = event.getMessage();
        if (msg.equalsIgnoreCase("cancel")) {
            player.sendMessage("§cCancelled.");
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<String> cmds = new ArrayList<>(plugin.getConfig().getStringList("config.start_commands"));
            if (index >= 0 && index < cmds.size()) {
                cmds.set(index, msg);
                plugin.getConfig().set("config.start_commands", cmds);
                plugin.saveConfig();
                player.sendMessage("§aStart command #" + (index + 1) + " set to: §7" + msg);
            }
            openStartCommands(player);
        });
    }

    // ============================================================
    // Shared helpers
    // ============================================================

    /** Adjust an int by a fixed step, clamped at 0. */
    private void adjustIntStep(String path, int def, int step, ClickType click) {
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

    private void cycleString(String path, List<String> cycle, String def) {
        String cur = plugin.getConfig().getString(path, def);
        int idx = cycle.indexOf(cur);
        idx = (idx + 1) % cycle.size();
        plugin.getConfig().set(path, cycle.get(idx));
        plugin.saveConfig();
    }

    private static String formatTime(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
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
