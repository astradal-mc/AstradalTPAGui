package net.astradal.astradalTPAGui.listeners;

import net.astradal.astradalTPAGui.managers.ScrollManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener implements Listener {

    private final ScrollManager scrollManager;

    public TeleportListener(ScrollManager scrollManager) {
        this.scrollManager = scrollManager;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player teleporter = event.getPlayer();
        Location destination = event.getTo();

        // Scenario A: It is a normal TPA. The Teleporter IS the Requester.
        ScrollManager.ScrollRequest tpaReq = scrollManager.getRequestByRequester(teleporter);
        if (tpaReq != null && !tpaReq.isHere()) {
            Player target = Bukkit.getPlayer(tpaReq.target());
            if (target != null && destination.getWorld().equals(target.getWorld()) && destination.distanceSquared(target.getLocation()) <= 9.0) {
                scrollManager.consumeScroll(teleporter); // Requester pays
                scrollManager.removePendingRequest(teleporter);
            }
            return;
        }

        // Scenario B: It is a TPAHERE. The Teleporter IS the Target.
        ScrollManager.ScrollRequest hereReq = scrollManager.getRequestByTarget(teleporter);
        if (hereReq != null && hereReq.isHere()) {
            Player requester = Bukkit.getPlayer(hereReq.requester());
            if (requester != null && destination.getWorld().equals(requester.getWorld()) && destination.distanceSquared(requester.getLocation()) <= 9.0) {
                scrollManager.consumeScroll(requester); // Requester STILL pays
                scrollManager.removePendingRequest(requester);
            }
        }
    }
}