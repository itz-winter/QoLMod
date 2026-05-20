package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.qolmod.QoLMod;
import dev.qolmod.features.treechopper.TreeChopperManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Registers /treechopper (server-side toggle).
 */
public class TreeChopperCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("treechopper")
                .executes(TreeChopperCommand::executeToggle));

        // Alias
        dispatcher.register(CommandManager.literal("tc")
                .executes(TreeChopperCommand::executeToggle));
    }

    private static int executeToggle(CommandContext<ServerCommandSource> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        TreeChopperManager manager = QoLMod.getTreeChopperManager();
        manager.toggle(player.getUUID());
        boolean enabled = manager.isEnabled(player.getUUID());

        player.sendMessage(Component.literal(enabled
                ? "§aTree Chopper enabled."
                : "§cTree Chopper disabled."));
        return 1;
    }
}
