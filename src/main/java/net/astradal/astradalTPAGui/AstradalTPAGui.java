package net.astradal.astradalTPAGui;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.astradal.astradalTPAGui.commands.TPAGuiCommand;
import net.astradal.astradalTPAGui.listeners.InventoryClickListener;
import net.astradal.astradalTPAGui.listeners.OnScrollUseListener;
import net.astradal.astradalTPAGui.listeners.TeleportListener;
import net.astradal.astradalTPAGui.managers.ScrollManager;
import net.astradal.astradalTPAGui.utils.ConfigMigrationUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class AstradalTPAGui extends JavaPlugin {

    private ScrollManager scrollManager;

    @Override
    public void onEnable() {
        // --- 1. Configuration ---
        saveDefaultConfig();
        // This handles adding new keys
        ConfigMigrationUtil.migrateConfigDefaults(this);
        // This handles updating the version key
        ConfigMigrationUtil.updateVersionInConfig(this);

        this.scrollManager = new ScrollManager(this);

        registerCommands();

        getServer().getPluginManager().registerEvents(new InventoryClickListener(this, scrollManager), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(scrollManager), this);
        getServer().getPluginManager().registerEvents(new OnScrollUseListener(this, scrollManager), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerCommands() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final var registrar = event.registrar();
            final var dispatcher = registrar.getDispatcher();

            LiteralArgumentBuilder<CommandSourceStack> commandBuilder = TPAGuiCommand.create(this, dispatcher);

            LiteralCommandNode<CommandSourceStack> commandNode = commandBuilder.build();

            registrar.register(commandNode, "Manage the TPAGUI system");

        });
    }

    public ScrollManager getScrollManager() {
        return scrollManager;
    }
}
