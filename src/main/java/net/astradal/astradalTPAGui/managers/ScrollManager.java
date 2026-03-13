package net.astradal.astradalTPAGui.managers;

import net.astradal.astradalTPAGui.AstradalTPAGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScrollManager {
    private final AstradalTPAGui plugin;
    private final NamespacedKey key;

    // Tracks Requester UUID -> Target UUID
    private final Map<UUID, ScrollRequest> pendingTeleports = new HashMap<>();
    public record ScrollRequest(UUID requester, UUID target, boolean isHere) {}

    public ScrollManager(AstradalTPAGui plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "tpa_scroll");
    }

    public boolean isTPAScroll(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public ItemStack createTPAScrollItem() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("TPA Scroll", NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(
            Component.text("Right-click to open the TPA menu", NamedTextColor.GRAY),
            Component.text("Consumed on teleport", NamedTextColor.DARK_GRAY)
        ));
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public void addPendingRequest(Player requester, Player target, boolean isHere) {
        pendingTeleports.put(requester.getUniqueId(), new ScrollRequest(requester.getUniqueId(), target.getUniqueId(), isHere));

        Bukkit.getScheduler().runTaskLater(plugin, () -> pendingTeleports.remove(requester.getUniqueId()), 20L * 130);
    }

    public void removePendingRequest(Player requester) {
        pendingTeleports.remove(requester.getUniqueId());
    }

    public void consumeScroll(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isTPAScroll(item)) {
                assert item != null;
                item.subtract(1); // Modern Paper method for item subtraction
                player.sendMessage(Component.text("Your TPA Scroll has been used.", NamedTextColor.GRAY));
                break; // Only consume one
            }
        }
    }

    public ScrollRequest getRequestByTarget(Player target) {
        for (ScrollRequest request : pendingTeleports.values()) {
            if (request.target().equals(target.getUniqueId())) {
                return request;
            }
        }
        return null;
    }

    public ScrollRequest getRequestByRequester(Player requester) {
        return pendingTeleports.get(requester.getUniqueId());
    }
}