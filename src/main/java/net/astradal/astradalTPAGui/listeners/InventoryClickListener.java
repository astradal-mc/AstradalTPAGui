package net.astradal.astradalTPAGui.listeners;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
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
    private final Essentials essentials;

    public InventoryClickListener(AstradalTPAGui plugin, ScrollManager scrollManager) {
        this.plugin = plugin;
        this.scrollManager = scrollManager;
        // Hook directly into the Essentials plugin instance
        this.essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
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
            // 1. Register the attempt in our manager
            scrollManager.addPendingRequest(clicker, target);

            // 2. Convert Bukkit players to Essentials Users
            User essClicker = essentials.getUser(clicker);
            User essTarget = essentials.getUser(target);

            try {
                // 3. Use the official API to register the request silently
                essTarget.requestTeleport(essClicker, false);

                // Tell the sender it worked
                clicker.sendMessage(Component.text("Request sent to ", NamedTextColor.YELLOW)
                    .append(Component.text(target.getName(), NamedTextColor.GOLD)));

                // Tell the target they have a request!
                target.sendMessage(Component.text(clicker.getName(), NamedTextColor.GOLD)
                    .append(Component.text(" has requested to teleport to you.", NamedTextColor.YELLOW)));
                target.sendMessage(Component.text("Type ", NamedTextColor.YELLOW)
                    .append(Component.text("/tpaccept", NamedTextColor.GREEN))
                    .append(Component.text(" to accept.", NamedTextColor.YELLOW)));

            } catch (Exception e) {
                // Catch any Essentials errors (like target having tp toggle off)
                clicker.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
                scrollManager.removePendingRequest(clicker);
            }

            clicker.closeInventory();
        } else {
            clicker.sendMessage(Component.text("That player is no longer online.", NamedTextColor.RED));
            clicker.closeInventory();
        }
    }
}