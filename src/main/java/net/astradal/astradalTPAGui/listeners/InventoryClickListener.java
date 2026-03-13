package net.astradal.astradalTPAGui.listeners;

import net.astradal.astradalTPAGui.AstradalTPAGui;
import net.astradal.astradalTPAGui.managers.ScrollManager;
import net.astradal.astradalTPAGui.gui.GuiInventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class InventoryClickListener implements Listener {

    private final AstradalTPAGui plugin;
    private final ScrollManager scrollManager;

    public InventoryClickListener(AstradalTPAGui plugin, ScrollManager scrollManager) {
        this.plugin = plugin;
        this.scrollManager = scrollManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();
        if (inventory == null || !(inventory.getHolder(false) instanceof GuiInventory)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

        Player clicker = (Player) event.getWhoClicked();

        if (!(clicked.getItemMeta() instanceof SkullMeta skullMeta)) return;

        NamespacedKey key = new NamespacedKey(plugin, "tpa_target");
        String uuidStr = skullMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (uuidStr == null) {
            clicker.sendMessage(Component.text("Could not find target player", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(UUID.fromString(uuidStr));

        if (target != null && target.isOnline()) {
            // Register the attempt
            scrollManager.addPendingRequest(clicker, target);

            // Force EssentialsX to handle it natively
            clicker.performCommand("tpa " + target.getName());
            clicker.closeInventory();
        } else {
            clicker.sendMessage(Component.text("That player is no longer online.", NamedTextColor.RED));
            clicker.closeInventory();
        }
    }
}