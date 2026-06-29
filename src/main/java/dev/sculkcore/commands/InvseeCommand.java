package dev.sculkcore.commands;

import dev.sculkcore.SculkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

public class InvseeCommand implements CommandExecutor, Listener {
    private final SculkCorePlugin plugin;

    public InvseeCommand(SculkCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length < 1) {
            admin.sendMessage("§cYou need to specify a player");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            admin.sendMessage("§cThat player is not online");
            return true;
        }

        String cmdName = command.getName().toLowerCase();
        if (cmdName.equals("invsee")) {
            if (!admin.hasPermission("sculkcore.invsee")) {
                admin.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }
            openInvsee(admin, target);
        } else if (cmdName.equals("endersee")) {
            if (!admin.hasPermission("sculkcore.endersee")) {
                admin.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }
            openEndersee(admin, target);
        }

        return true;
    }

    private void openInvsee(Player admin, Player target) {
        Inventory gui = Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacySection().deserialize("§b" + target.getName() + "'s Inventory §b§l(InvSee)"));
        ItemStack[] targetContents = target.getInventory().getContents();
        ItemStack[] targetArmor = target.getInventory().getArmorContents();
        ItemStack targetOffhand = target.getInventory().getItemInOffHand();

        // 1. Map Main Inventory (target 9-35) -> GUI 18-44
        for (int i = 9; i < 36; i++) {
            if (targetContents[i] != null) {
                gui.setItem(i + 9, targetContents[i]);
            }
        }

        // 2. Map Hotbar (target 0-8) -> GUI 45-53
        for (int i = 0; i < 9; i++) {
            if (targetContents[i] != null) {
                gui.setItem(i + 45, targetContents[i]);
            }
        }

        // 3. Map Armor -> GUI 3, 4, 5, 6
        int[] armorSlots = {3, 4, 5, 6};
        for (int i = 0; i < 4; i++) {
            if (targetArmor[i] != null) {
                gui.setItem(armorSlots[i], targetArmor[i]);
            }
        }

        // 4. Map Offhand -> GUI 2
        if (targetOffhand.getType() != Material.AIR) {
            gui.setItem(2, targetOffhand);
        }

        // 5. Fill Spacers
        ItemStack spacer = getSpacerPane();
        gui.setItem(0, spacer);
        gui.setItem(1, spacer);
        gui.setItem(7, spacer);
        gui.setItem(8, spacer);

        for (int i = 9; i < 18; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, spacer);
            }
        }

        admin.openInventory(gui);
    }

    private void openEndersee(Player admin, Player target) {
        Inventory gui = Bukkit.createInventory(null, 27, LegacyComponentSerializer.legacySection().deserialize("§b" + target.getName() + "'s Ender Chest §b§l(ESee)"));
        ItemStack[] targetEnder = target.getEnderChest().getContents();
        for (int i = 0; i < 27; i++) {
            if (targetEnder[i] != null) {
                gui.setItem(i, targetEnder[i]);
            }
        }
        admin.openInventory(gui);
    }

    private ItemStack getSpacerPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setHideTooltip(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("§b§l(InvSee)")) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta != null && meta.isHideTooltip()) {
                    event.setCancelled(true);
                    return;
                }
            }

            int slot = event.getRawSlot();
            if (slot >= 0 && slot < 54) {
                if (slot == 0 || slot == 1 || slot == 7 || slot == 8 || (slot >= 9 && slot <= 17)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryCloseInvsee(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("§b§l(InvSee)")) {
            return;
        }

        String targetName = ChatColor.stripColor(title)
                .replace("'s Inventory (InvSee)", "")
                .trim();

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        ItemStack[] newContents = new ItemStack[36];

        // 1. Sync Main Inventory (GUI 18-44) -> target 9-35
        for (int i = 9; i < 36; i++) {
            newContents[i] = top.getItem(i + 9);
        }

        // 2. Sync Hotbar (GUI 45-53) -> target 0-8
        for (int i = 0; i < 9; i++) {
            newContents[i] = top.getItem(i + 45);
        }

        // 3. Sync Armor (GUI 3, 4, 5, 6) -> target 0-3
        ItemStack[] newArmor = new ItemStack[4];
        int[] armorSlots = {3, 4, 5, 6};
        for (int i = 0; i < 4; i++) {
            newArmor[i] = top.getItem(armorSlots[i]);
        }

        // 4. Sync Offhand (GUI 2) -> target
        ItemStack newOffhand = top.getItem(2);

        target.getInventory().setContents(newContents);
        target.getInventory().setArmorContents(newArmor);
        target.getInventory().setItemInOffHand(newOffhand);
        target.updateInventory();
    }

    @EventHandler
    public void onInventoryCloseEndersee(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("§b§l(ESee)")) {
            return;
        }

        String targetName = ChatColor.stripColor(title)
                .replace("'s Ender Chest (ESee)", "")
                .trim();

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        ItemStack[] newEnder = new ItemStack[27];
        for (int i = 0; i < 27; i++) {
            newEnder[i] = top.getItem(i);
        }

        target.getEnderChest().setContents(newEnder);
        target.updateInventory();
    }
}
