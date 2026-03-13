package net.astradal.astradalTPAGui.commands.subcommands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.astradal.astradalTPAGui.AstradalTPAGui;
import net.astradal.astradalTPAGui.commands.TPAGuiPermissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GetScrollCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build(AstradalTPAGui plugin) {
        return Commands.literal("getScroll")
            .requires(TPAGuiPermissions.requires("getScroll"))
            .executes(ctx -> execute(ctx, plugin));
    }

    public static int execute(CommandContext<CommandSourceStack> context, AstradalTPAGui plugin) {
        CommandSender sender = context.getSource().getSender();
        if(!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only Players may use this command.", NamedTextColor.RED));
            return 0;
        }

        player.getInventory().addItem(plugin.getScrollManager().createTPAScrollItem());
        player.sendMessage(Component.text("You have been given a TPA Scroll.", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

}
