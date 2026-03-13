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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TpacancelCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build(AstradalTPAGui plugin) {
        return Commands.literal("tpacancel")
            .executes(ctx -> execute(ctx, plugin));
    }

    public static int execute(CommandContext<CommandSourceStack> context, AstradalTPAGui plugin) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof Player player)) return 0;

        ScrollRequest req = plugin.getScrollManager().getRequestByRequester(player);
        if (req == null) {
            player.sendMessage(Component.text("You have no pending teleport requests to cancel.", NamedTextColor.RED));
            return 0;
        }

        plugin.getScrollManager().removePendingRequest(player);
        player.sendMessage(Component.text("Your teleport request has been cancelled.", NamedTextColor.YELLOW));

        return Command.SINGLE_SUCCESS;
    }
}