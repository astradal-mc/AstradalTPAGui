package net.astradal.astradalTPAGui.commands.subcommands;

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
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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

        ScrollRequest request = plugin.getScrollManager().getRequestByTarget(target);

        if (request == null) {
            target.sendMessage(Component.text("You do not have any pending TPA requests.", NamedTextColor.RED));
            return 0;
        }

        Player requester = Bukkit.getPlayer(request.requester());
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(Component.text("The player who sent the request is no longer online.", NamedTextColor.RED));
            return 0;
        }

        // Determine who is traveling based on TPA vs TPAHERE
        Player traveler = request.isHere() ? target : requester;
        Player destination = request.isHere() ? requester : target;

        target.sendMessage(Component.text("Teleport request accepted.", NamedTextColor.GREEN));
        requester.sendMessage(Component.text(target.getName() + " accepted your scroll request!", NamedTextColor.GREEN));

        // Pull warmup settings from config (default to 3 seconds if missing)
        int warmupSeconds = plugin.getConfig().getInt("teleport-mechanics.warmup-seconds", 3);
        boolean cancelOnMove = plugin.getConfig().getBoolean("teleport-mechanics.cancel-on-move", true);

        // Start the Warmup Sequence
        new BukkitRunnable() {
            int ticks = warmupSeconds * 20;
            final Location startLoc = traveler.getLocation();

            @Override
            public void run() {
                // 1. Safety Check: Are they both still online?
                if (!traveler.isOnline() || !destination.isOnline()) {
                    plugin.getScrollManager().removePendingRequest(requester);
                    this.cancel();
                    return;
                }

                // 2. Movement Check
                if (cancelOnMove && (!startLoc.getWorld().equals(traveler.getWorld()) || startLoc.distanceSquared(traveler.getLocation()) > 0.5)) {
                    traveler.sendMessage(Component.text("Teleport cancelled because you moved!", NamedTextColor.RED));
                    plugin.getScrollManager().removePendingRequest(requester);
                    this.cancel();
                    return;
                }

                // 3. Execution
                if (ticks <= 0) {
                    this.cancel();
                    traveler.sendMessage(Component.text("Teleporting...", NamedTextColor.GOLD));

                    // Paper's Native Async Teleport
                    traveler.teleportAsync(destination.getLocation()).thenAccept(success -> {
                        if (success) {
                            // If they arrive safely, the requester pays the scroll fee!
                            plugin.getScrollManager().consumeScroll(requester);
                            plugin.getScrollManager().removePendingRequest(requester);
                        }
                    });
                    return;
                }

                // 4. Effects & Countdown
                // Spawn cool portal particles around the traveler every 5 ticks
                traveler.getWorld().spawnParticle(Particle.PORTAL, traveler.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

                // Send a chat message every full second
                if (ticks % 20 == 0) {
                    traveler.sendMessage(Component.text("Teleporting in " + (ticks / 20) + " seconds... Don't move.", NamedTextColor.YELLOW));
                }

                ticks -= 5; // We run the task every 5 ticks (1/4 second) for smooth particles
            }
        }.runTaskTimer(plugin, 0L, 5L);

        return Command.SINGLE_SUCCESS;
    }
}