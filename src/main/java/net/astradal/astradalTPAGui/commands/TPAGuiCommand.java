package net.astradal.astradalTPAGui.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.astradal.astradalTPAGui.AstradalTPAGui;
import net.astradal.astradalTPAGui.commands.subcommands.*;

public final class TPAGuiCommand {

    /**
     * Creates and builds the root '/tpagui' command node with all its subcommands.
     * This static method acts as a factory for the entire command structure.
     *
     * @param plugin     The main plugin instance, which is passed to subcommands
     * so they can access plugin services and managers.
     * @param dispatcher The command dispatcher, used here to build the default help command's
     * usage message.
     * @return The fully constructed command node, ready to be registered with Paper's
     * command system.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> create(
        AstradalTPAGui plugin,
        CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        var rootNode = Commands.literal("tpagui")
            .executes(ctx -> HelpCommand.execute(ctx, dispatcher));

        // Register all subcommands
        rootNode.then(HelpCommand.build(dispatcher));
        rootNode.then(GetScrollCommand.build(plugin));
        rootNode.then(ReloadCommand.build(plugin));
        rootNode.then(VersionCommand.build(plugin));
        rootNode.then(TpaCommand.build(plugin));
        rootNode.then(TpacceptCommand.build(plugin));
        rootNode.then(TpahereCommand.build(plugin));
        rootNode.then(TpacancelCommand.build(plugin));

        return rootNode;
    }
}
