package net.astradal.astradalTPAGui.listeners;

import net.astradal.astradalTPAGui.AstradalTPAGui;
import net.astradal.astradalTPAGui.gui.GuiInventory;
import net.astradal.astradalTPAGui.managers.ScrollManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class OnScrollUseListener implements Listener {

    private final AstradalTPAGui plugin;
    private final ScrollManager scrollManager;

    public OnScrollUseListener(AstradalTPAGui plugin, ScrollManager scrollManager) {
        this.plugin = plugin;
        this.scrollManager = scrollManager;
    }

    @EventHandler
    public void onScrollUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;

        // Check if the item is a TPA scroll using the new manager
        if (scrollManager.isTPAScroll(item)) {
            event.setCancelled(true); // Prevent default item usage (like placing blocks if it was re-textured)

            Player player = event.getPlayer();

            // Instantiate and open the GUI directly
            GuiInventory gui = new GuiInventory(plugin, player);
            player.openInventory(gui.getInventory());

            plugin.getLogger().info("Player " + player.getName() + " opened the TPA scroll menu.");
        }
    }
}