package dev.sculkcore;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

import dev.sculkcore.altar.AltarManager;
import dev.sculkcore.altar.AltarListener;
import dev.sculkcore.config.ConfigManager;
import dev.sculkcore.recipes.RecipeRegistry;
import dev.sculkcore.commands.SaltarCommand;
import dev.sculkcore.commands.RitualCommand;
import dev.sculkcore.commands.StartCommand;
import dev.sculkcore.commands.StopGraceCommand;
import dev.sculkcore.commands.SckitCommand;
import dev.sculkcore.commands.ItemLimitCommand;
import dev.sculkcore.commands.SettingsCommand;
import dev.sculkcore.commands.BanItemCommand;
import dev.sculkcore.commands.SbroadcastCommand;
import dev.sculkcore.commands.WhitelistPlusCommand;
import dev.sculkcore.commands.EnchantCommand;
import dev.sculkcore.commands.VanishCommand;
import dev.sculkcore.commands.InvseeCommand;
import dev.sculkcore.commands.SpawnCommand;
import dev.sculkcore.commands.ReplyCommand;
import dev.sculkcore.commands.WorldTpCommand;
import dev.sculkcore.listeners.CombatListener;
import dev.sculkcore.listeners.GraceListener;
import dev.sculkcore.listeners.PlayerListener;
import dev.sculkcore.listeners.MinimapBlocker;
import dev.sculkcore.listeners.PacketListenerImpl;
import dev.sculkcore.listeners.CombatLimitersListener;
import dev.sculkcore.listeners.BanListsListener;
import dev.sculkcore.listeners.EnchantmentLimiter;
import dev.sculkcore.listeners.VillagerRestockListener;
import dev.sculkcore.listeners.VillagerProtectionListener;
import dev.sculkcore.listeners.VillagerAnchorListener;

public final class SculkCorePlugin extends JavaPlugin implements Listener {
    private static SculkCorePlugin instance;
    public static NamespacedKey glowing;
    public int beforeStartBorder;
    private ConfigManager configManager;
    private AltarManager altarManager;
    private ItemLimitCommand itemLimitCommand;
    private BanItemCommand banItemCommand;

    public static SculkCorePlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AltarManager getAltarManager() {
        return altarManager;
    }

    public ItemLimitCommand getItemLimitCommand() {
        return itemLimitCommand;
    }

    public BanItemCommand getBanItemCommand() {
        return banItemCommand;
    }

    @Override
    public void onLoad() {
        try {
            if (getServer().getPluginManager().getPlugin("packetevents") != null) {
                PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
                PacketEvents.getAPI().getSettings().checkForUpdates(true).bStats(false);
                PacketEvents.getAPI().load();
            }
        } catch (Exception e) {
            getLogger().warning("Failed to load PacketEvents integration: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        glowing = new NamespacedKey(this, "glowing");
        
        saveDefaultConfig();
        reloadConfig();
        
        configManager = new ConfigManager(this);
        altarManager = new AltarManager(this);
        
        beforeStartBorder = getConfig().getInt("config.before_start_border", 1000);
        
        // Register core plugin listener
        getServer().getPluginManager().registerEvents(this, this);
        
        // Register custom listeners
        getServer().getPluginManager().registerEvents(new AltarListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new GraceListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new MinimapBlocker(this), this);
        getServer().getPluginManager().registerEvents(new CombatLimitersListener(this), this);
        getServer().getPluginManager().registerEvents(new BanListsListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantmentLimiter(this), this);
        getServer().getPluginManager().registerEvents(new VillagerRestockListener(this), this);
        getServer().getPluginManager().registerEvents(new VillagerProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new VillagerAnchorListener(this), this);
        
        // Register custom recipes
        RecipeRegistry.registerAll(this);
        
        // Register commands
        getCommand("start").setExecutor(new StartCommand(this));
        getCommand("stopgrace").setExecutor(new StopGraceCommand(this));
        getCommand("ritual").setExecutor(new RitualCommand(this));
        
        SaltarCommand saltarCommand = new SaltarCommand(this);
        getCommand("saltar").setExecutor(saltarCommand);
        getCommand("saltar").setTabCompleter(saltarCommand);
        
        SckitCommand sckitCommand = new SckitCommand(this);
        getServer().getPluginManager().registerEvents(sckitCommand, this);
        getCommand("sckit").setExecutor(sckitCommand);
        getCommand("sckit").setTabCompleter(sckitCommand);
        
        this.itemLimitCommand = new ItemLimitCommand(this);
        getServer().getPluginManager().registerEvents(this.itemLimitCommand, this);
        getCommand("itemlimit").setExecutor(this.itemLimitCommand);
        
        SettingsCommand settingsCommand = new SettingsCommand(this);
        getCommand("settings").setExecutor(settingsCommand);
        getServer().getPluginManager().registerEvents(settingsCommand, this);
        
        this.banItemCommand = new BanItemCommand(this);
        getServer().getPluginManager().registerEvents(this.banItemCommand, this);
        getCommand("banitem").setExecutor(this.banItemCommand);
        
        getCommand("whitelistplus").setExecutor(new WhitelistPlusCommand(this));
        getCommand("sbroadcast").setExecutor(new SbroadcastCommand(this));
        
        EnchantCommand enchantCommand = new EnchantCommand(this);
        getCommand("enchant").setExecutor(enchantCommand);
        getCommand("enchant").setTabCompleter(enchantCommand);
        
        VanishCommand vanishCommand = new VanishCommand(this);
        getServer().getPluginManager().registerEvents(vanishCommand, this);
        getCommand("vanish").setExecutor(vanishCommand);
        
        InvseeCommand invseeCommand = new InvseeCommand(this);
        getServer().getPluginManager().registerEvents(invseeCommand, this);
        getCommand("invsee").setExecutor(invseeCommand);
        getCommand("endersee").setExecutor(invseeCommand);
        
        SpawnCommand spawnCommand = new SpawnCommand(this);
        getServer().getPluginManager().registerEvents(spawnCommand, this);
        getCommand("setcustomspawn").setExecutor(spawnCommand);
        getCommand("setcustomspawn").setTabCompleter(spawnCommand);
        getCommand("setrespawnspawn").setExecutor(spawnCommand);
        getCommand("setrespawnspawn").setTabCompleter(spawnCommand);
        
        ReplyCommand replyCommand = new ReplyCommand(this);
        getServer().getPluginManager().registerEvents(replyCommand, this);
        getCommand("reply").setExecutor(replyCommand);
        
        WorldTpCommand worldTpCommand = new WorldTpCommand(this);
        getCommand("worldtp").setExecutor(worldTpCommand);
        getCommand("worldtp").setTabCompleter(worldTpCommand);
        
        // Apply Paper settings checks
        hideItemMeta();
        attributeSwapping();
        antiXray();
        
        // Force load pocket chunk for villagers
        try {
            if (!Bukkit.getWorlds().isEmpty()) {
                org.bukkit.Location hiddenLoc = new org.bukkit.Location(Bukkit.getWorlds().getFirst(), 0.0, 70000.0, 0.0);
                org.bukkit.Chunk chunk = hiddenLoc.getChunk();
                chunk.setForceLoaded(true);
                chunk.load(true);
            }
        } catch (Exception e) {
            getLogger().warning("Failed to force load villager pocket chunk: " + e.getMessage());
        }

        // Start Combat BossBar Task
        new org.bukkit.scheduler.BukkitRunnable() {
            private final java.util.Map<java.util.UUID, org.bukkit.boss.BossBar> bars = new java.util.HashMap<>();

            @Override
            public void run() {
                if (!getConfig().getBoolean("rules.combat_system", false)) {
                    for (org.bukkit.boss.BossBar bar : bars.values()) {
                        bar.removeAll();
                        bar.setVisible(false);
                    }
                    bars.clear();
                    return;
                }

                String displayMode = getConfig().getString("config.combat_display", "BOSSBAR");
                boolean useBossBar = displayMode.equalsIgnoreCase("BOSSBAR") || displayMode.equalsIgnoreCase("BOTH");

                for (Player player : Bukkit.getOnlinePlayers()) {
                    java.util.UUID uuid = player.getUniqueId();
                    double remaining = dev.sculkcore.game.GameState.getCooldownRemaining(uuid, "combat");

                    if (remaining > 0 && useBossBar) {
                        org.bukkit.boss.BossBar bar = bars.computeIfAbsent(uuid, k -> {
                            org.bukkit.boss.BossBar b = Bukkit.createBossBar(
                                    "",
                                    org.bukkit.boss.BarColor.RED,
                                    org.bukkit.boss.BarStyle.SEGMENTED_20
                            );
                            b.addPlayer(player);
                            b.setVisible(true);
                            return b;
                        });

                        int totalTime = getConfig().getInt("config.combat_tag_time", 30);
                        double progress = Math.min(1.0, Math.max(0.0, remaining / (double) totalTime));
                        bar.setProgress(progress);
                        
                        long minutes = (long) remaining / 60;
                        long secs = (long) remaining % 60;
                        String timeStr = String.format("%d:%02d", minutes, secs);
                        String titlePattern = getConfig().getString("rules.combat_bar_title", "&cCombat Tag: &f&l<combat_time>");
                        String title = titlePattern.replace("<combat_time>", timeStr);
                        bar.setTitle(getConfigManager().translateColor(title));
                    } else {
                        org.bukkit.boss.BossBar bar = bars.remove(uuid);
                        if (bar != null) {
                            bar.removePlayer(player);
                            bar.setVisible(false);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 5L);

        if (getServer().getPluginManager().getPlugin("packetevents") != null) {
            try {
                PacketEvents.getAPI().init();
                PacketListenerImpl packetListener = new PacketListenerImpl(this);
                PacketEvents.getAPI().getEventManager().registerListener(packetListener);
                getServer().getPluginManager().registerEvents(packetListener, this);
            } catch (Exception e) {
                getLogger().warning("Failed to initialize PacketEvents API: " + e.getMessage());
            }
        }
        
        getLogger().info("SculkCore has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (altarManager != null) {
            altarManager.cleanUpDisplays();
        }
        if (getServer().getPluginManager().getPlugin("packetevents") != null) {
            try {
                PacketEvents.getAPI().terminate();
            } catch (Exception e) {
                getLogger().warning("Failed to terminate PacketEvents API: " + e.getMessage());
            }
        }
        getLogger().info("SculkCore has been successfully disabled!");
    }

    /**
     * Obfuscator-proof implementation of original paper config modifier for hiding item metadata
     */
    public void hideItemMeta() {
        File file = new File(Bukkit.getServer().getWorldContainer(), "config/paper-world-defaults.yml");
        if (!file.exists()) {
            getLogger().warning("[HideDurability] paper-world-defaults.yml not found in config folder!");
            return;
        }
        try {
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
            String path = "anticheat.obfuscation.items.hide-durability";
            boolean value = getConfig().getBoolean(path, false);
            boolean current = yamlConfiguration.getBoolean(path, true);
            if (current != value) {
                yamlConfiguration.set(path, value);
                yamlConfiguration.save(file);
                getLogger().info("[HideDurability] " + path + " updated to " + value + ". Triggering server restart.");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
            }
        } catch (Exception e) {
            getLogger().warning("[HideDurability] Failed to modify paper-world-defaults.yml: " + e.getMessage());
        }
    }

    /**
     * Obfuscator-proof implementation of original paper config modifier for attribute swapping protection
     */
    public void attributeSwapping() {
        File file = new File(Bukkit.getServer().getWorldContainer(), "config/paper-global.yml");
        if (!file.exists()) {
            getLogger().warning("[AttributeSwapping] paper-global.yml not found in config folder!");
            return;
        }
        try {
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
            String path = "unsupported-settings.update-equipment-on-player-actions";
            boolean value = getConfig().getBoolean(path, false);
            boolean current = yamlConfiguration.getBoolean(path, false);
            if (current != value) {
                yamlConfiguration.set(path, value);
                yamlConfiguration.save(file);
                getLogger().info("[AttributeSwapping] " + path + " updated to " + value + ". Triggering server restart.");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
            }
        } catch (Exception e) {
            getLogger().warning("[AttributeSwapping] Failed to modify paper-global.yml: " + e.getMessage());
        }
    }

    /**
     * Obfuscator-proof implementation of original paper config modifier for anti-xray
     */
    public void antiXray() {
        File file = new File(Bukkit.getServer().getWorldContainer(), "config/paper-world-defaults.yml");
        if (!file.exists()) {
            getLogger().warning("[AntiXray] paper-world-defaults.yml not found in config folder!");
            return;
        }
        try {
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
            String pathEnable = "anticheat.anti-xray.enabled";
            String pathEngine = "anticheat.anti-xray.engine-mode";
            
            boolean valueEnable = getConfig().getBoolean(pathEnable, true);
            int valueEngine = getConfig().getInt(pathEngine, 1);
            
            boolean modified = false;
            if (yamlConfiguration.getBoolean(pathEnable, true) != valueEnable) {
                yamlConfiguration.set(pathEnable, valueEnable);
                modified = true;
            }
            if (yamlConfiguration.getInt(pathEngine, 1) != valueEngine) {
                yamlConfiguration.set(pathEngine, valueEngine);
                modified = true;
            }
            if (modified) {
                yamlConfiguration.save(file);
                getLogger().info("[AntiXray] Config updated. Triggering server restart.");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
            }
        } catch (Exception e) {
            getLogger().warning("[AntiXray] Failed to modify paper-world-defaults.yml: " + e.getMessage());
        }
    }

    public void sendTellrawAsync(String message, Player player) {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " \"" + message + "\"");
            }
        }, 10L);
    }
}
