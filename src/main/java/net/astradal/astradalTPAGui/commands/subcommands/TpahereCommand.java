package net.astradal.astradalTPAGui.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.astradal.astradalTPAGui.AstradalTPAGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class TpahereCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build(AstradalTPAGui plugin) {
        return Commands.literal("tpahere")
            .then(Commands.argument("target", ArgumentTypes.player())
                .executes(ctx -> execute(ctx, plugin)));
    }

    public static int execute(CommandContext<CommandSourceStack> context, AstradalTPAGui plugin) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only Players may use this command.", NamedTextColor.RED));
            return 0;
        }

        try {
            // Resolve the target player from the Brigadier arguments
            Player target = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();

            if (player.equals(target)) {
                player.sendMessage(Component.text("You cannot teleport to yourself.", NamedTextColor.RED));
                return 0;
            }

            // 1. Verify they actually have a scroll in their inventory
            boolean hasScroll = false;
            for (ItemStack item : player.getInventory().getContents()) {
                if (plugin.getScrollManager().isTPAScroll(item)) {
                    hasScroll = true;
                    break;
                }
            }

            if (!hasScroll) {
                player.sendMessage(Component.text("You need a TPA Scroll to send a teleport request.", NamedTextColor.RED));
                return 0;
            }

            // 2. Register request in the state manager
            plugin.getScrollManager().addPendingRequest(player, target, true);

            // 3. Send out the notifications and clickable accept button
            player.sendMessage(Component.text("Scroll request sent to ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(target.getName(), NamedTextColor.GOLD)));

            Component acceptButton = Component.text("[ACCEPT]")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Click to accept " + player.getName() + "'s scroll teleport")))
                .clickEvent(ClickEvent.runCommand("/tpaccept"));

            target.sendMessage(Component.text(player.getName(), NamedTextColor.GOLD)
                .append(Component.text(" wants you to teleport to them using a TPA Scroll.", NamedTextColor.LIGHT_PURPLE)));
            target.sendMessage(Component.text("Click here to accept: ", NamedTextColor.GRAY).append(acceptButton));

        } catch (Exception e) {
            player.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
        }

        return Command.SINGLE_SUCCESS;
    }
}