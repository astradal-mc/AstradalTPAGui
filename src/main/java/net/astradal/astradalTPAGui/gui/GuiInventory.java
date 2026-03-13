package net.astradal.astradalTPAGui.gui;

import net.astradal.astradalTPAGui.AstradalTPAGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.IntStream;

public class GuiInventory implements InventoryHolder {

    private final Inventory inventory;
    public final AstradalTPAGui plugin;

    public GuiInventory(AstradalTPAGui plugin, Player viewer) {
        this.plugin = plugin;

        int playerCount = 1; //default case
        if(plugin.getServer().getOnlinePlayers().size() > 1) { //ensure there are more than one person
            playerCount = plugin.getServer().getOnlinePlayers().size() - 1; // -1 to remove yourself from the list because your head is not displayed
        }

        // Inventories are in multiples of nines. Add 8 to round up, and divide and multiply by 9 to get the nearest multiple of 9.
        int nextMultipleOf9 = (((playerCount+8)/9)*9);

        // Initialize inventory
        this.inventory = plugin.getServer().createInventory(this, nextMultipleOf9, Component.text("TPA Menu", NamedTextColor.BLACK));

        // Get all the online players as a list, filtering for yourself
        List<Player> players = plugin.getServer().getOnlinePlayers()
            .stream()
            .map(p -> (Player) p)
            .filter(p -> !p.getUniqueId().equals(viewer.getUniqueId()))
            .toList();

        // Setup Key for PDC
        NamespacedKey key = new NamespacedKey(plugin, "tpa_target");

        List<Component> lore = List.of(
            Component.text("Left-Click to teleport to them", NamedTextColor.GREEN),
            Component.text("Right-Click to pull them to you", NamedTextColor.LIGHT_PURPLE)
        );

        // Iterate over each player from the list and add their head to the GUI, saving there UUID as a string in the PDC
        IntStream.range(0, players.size()).forEach(i -> {
            Player player = players.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if(meta != null) {
                meta.setOwningPlayer(plugin.getServer().getOfflinePlayer(player.getName())); //set skull's owner
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, player.getUniqueId().toString()); //add owner UUID to data storage of head
                meta.displayName(Component.text(player.getName(), NamedTextColor.GOLD)); //set skull name
                meta.lore(lore); //set usage instruction text as lore
                head.setItemMeta(meta);

            }

            // Set the head slot from the int stream to the head we just built
            this.inventory.setItem(i, head);
        });
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
