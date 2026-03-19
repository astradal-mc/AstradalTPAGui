package net.astradal.astradalTPAGui.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.astradal.astradalTPAGui.AstradalTPAGui;
import net.astradal.astradalTPAGui.managers.ScrollManager.ScrollRequest;
import net.kyori.adventure.bossbar.BossBar;
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

        // Pull warmup settings from config (default to 10 seconds if missing)
        int warmupSeconds = plugin.getConfig().getInt("teleport-mechanics.warmup-seconds", 10);
        boolean cancelOnMove = plugin.getConfig().getBoolean("teleport-mechanics.cancel-on-move", true);

        // Create the BossBar
        final BossBar warmupBar = BossBar.bossBar(
            Component.text("Teleporting...", NamedTextColor.LIGHT_PURPLE),
            1.0f,
            BossBar.Color.PURPLE,
            BossBar.Overlay.PROGRESS
        );

        // Show it to the traveling player right away
        traveler.showBossBar(warmupBar);

        // Start the Warmup Sequence
        new BukkitRunnable() {
            final int totalTicks = warmupSeconds * 20;
            int ticks = totalTicks;
            final Location startLoc = traveler.getLocation();

            @Override
            public void run() {
                // 1. Safety Check: Are they both still online?
                if (!traveler.isOnline() || !destination.isOnline()) {
                    if (traveler.isOnline()) traveler.hideBossBar(warmupBar);
                    plugin.getScrollManager().removePendingRequest(requester);
                    this.cancel();
                    return;
                }

                // 2. Movement Check
                if (cancelOnMove && (!startLoc.getWorld().equals(traveler.getWorld()) || startLoc.distanceSquared(traveler.getLocation()) > 0.5)) {
                    traveler.hideBossBar(warmupBar);
                    traveler.sendMessage(Component.text("Teleport cancelled because you moved!", NamedTextColor.RED));
                    plugin.getScrollManager().removePendingRequest(requester);
                    this.cancel();
                    return;
                }

                // 3. Execution
                if (ticks <= 0) {
                    this.cancel();

                    // Verify the requester STILL has the scroll right before we teleport
                    boolean hasScroll = false;
                    for (org.bukkit.inventory.ItemStack item : requester.getInventory().getContents()) {
                        if (plugin.getScrollManager().isTPAScroll(item)) {
                            hasScroll = true;
                            break;
                        }
                    }

                    // If they dropped it, cancel the teleport entirely
                    if (!hasScroll) {
                        traveler.hideBossBar(warmupBar);
                        traveler.sendMessage(Component.text("Teleport cancelled! The scroll is no longer in the requester's inventory.", NamedTextColor.RED));

                        if (!traveler.equals(requester)) { // Notify the other person too
                            requester.sendMessage(Component.text("Teleport cancelled! You lost the scroll.", NamedTextColor.RED));
                        }

                        plugin.getScrollManager().removePendingRequest(requester);
                        return;
                    }

                    traveler.hideBossBar(warmupBar);
                    traveler.sendMessage(Component.text("Teleporting...", NamedTextColor.GOLD));

                    // Paper's Native Async Teleport
                    traveler.teleportAsync(destination.getLocation()).thenAccept(success -> {
                        if (success) {
                            plugin.getScrollManager().consumeScroll(requester);
                            plugin.getScrollManager().removePendingRequest(requester);
                        }
                    });
                    return;
                }

                // 4. Effects & BossBar Animation
                // Calculate progress (0.0 to 1.0) and update the bar
                float progress = totalTicks > 0 ? Math.max(0.0f, Math.min(1.0f, (float) ticks / totalTicks)) : 0.0f;
                warmupBar.progress(progress);

                // Update the text on the bar to show the exact seconds remaining
                int secondsLeft = (int) Math.ceil(ticks / 20.0);
                warmupBar.name(Component.text("Warming up... " + secondsLeft + "s", NamedTextColor.LIGHT_PURPLE));

                // Spawn cool portal particles around the traveler every 5 ticks
                traveler.getWorld().spawnParticle(Particle.PORTAL, traveler.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

                ticks -= 5; // We run the task every 5 ticks (1/4 second) for smooth animations
            }
        }.runTaskTimer(plugin, 0L, 5L);

        return Command.SINGLE_SUCCESS;
    }
}