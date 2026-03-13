package net.astradal.astradalTPAGui.listeners;

import net.astradal.astradalTPAGui.managers.ScrollManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public class TeleportListener implements Listener {

    private final ScrollManager scrollManager;

    public TeleportListener(ScrollManager scrollManager) {
        this.scrollManager = scrollManager;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        // 1. Check if they used a scroll recently
        UUID targetId = scrollManager.getPendingTarget(player);
        if (targetId == null) return;

        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) return;

        Location destination = event.getTo();
        Location targetLoc = target.getLocation();

        // 2. Check if the destination is near the requested target
        // We use distanceSquared for performance, checking within ~3 blocks
        if (destination.getWorld().equals(targetLoc.getWorld()) && destination.distanceSquared(targetLoc) <= 9.0) {

            // It's a match! The TPA was successful.
            scrollManager.consumeScroll(player);
            scrollManager.removePendingRequest(player);
        }
    }
}