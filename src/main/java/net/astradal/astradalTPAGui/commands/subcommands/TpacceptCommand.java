package net.astradal.astradalTPAGui.commands.subcommands;

import com.earth2me.essentials.AsyncTeleport;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.astradal.astradalTPAGui.AstradalTPAGui;
import net.astradal.astradalTPAGui.managers.ScrollManager.ScrollRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.concurrent.CompletableFuture;

public final class TpacceptCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build(AstradalTPAGui plugin) {
        return Commands.literal("tpaccept")
            .executes(ctx -> execute(ctx, plugin));
    }

    public static int execute(CommandContext<CommandSourceStack> context, AstradalTPAGui plugin) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player target)) {
            sender.sendMessage(Component.text("Only Players may use this command.", NamedTextColor.RED));
            return 0;
        }

        // 1. Get the complex request object mapped to this target
        ScrollRequest request = plugin.getScrollManager().getRequestByTarget(target);

        if (request == null) {
            target.sendMessage(Component.text("You do not have any pending TPA requests.", NamedTextColor.RED));
            return 0;
        }

        // 2. Verify the requester is still online
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(Component.text("The player who sent the request is no longer online.", NamedTextColor.RED));
            return 0;
        }

        target.sendMessage(Component.text("Teleport request accepted.", NamedTextColor.GREEN));
        requester.sendMessage(Component.text(target.getName() + " accepted your scroll request! Warming up...", NamedTextColor.GREEN));

        // 3. Hook into Essentials to borrow their AsyncTeleport logic
        Essentials ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        Trade charge = new Trade("tpa", ess);
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // 4. Branch the math based on the travel direction
            if (request.isHere()) {
                // TPAHERE: Target moves to Requester
                assert ess != null;
                User essTarget = ess.getUser(target);
                AsyncTeleport teleport = essTarget.getAsyncTeleport();
                teleport.setTpType(AsyncTeleport.TeleportType.TPA); // FIXED: Essentials uses TPA for both
                teleport.teleport(ess.getUser(requester).getBase(), charge, TeleportCause.COMMAND, future);
            } else {
                // TPA: Requester moves to Target
                assert ess != null;
                User essRequester = ess.getUser(requester);
                AsyncTeleport teleport = essRequester.getAsyncTeleport();
                teleport.setTpType(AsyncTeleport.TeleportType.TPA);
                teleport.teleport(ess.getUser(target).getBase(), charge, TeleportCause.COMMAND, future);
            }
        } catch (Exception e) {
            target.sendMessage(Component.text("An error occurred during teleportation.", NamedTextColor.RED));
            e.printStackTrace();
        }

        // NOTE: We do NOT remove the request from ScrollManager here!
        // We leave it there so the TeleportListener can verify the proximity and consume the scroll when they land.

        return Command.SINGLE_SUCCESS;
    }
}