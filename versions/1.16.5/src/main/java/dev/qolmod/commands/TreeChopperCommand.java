package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.qolmod.QoLMod;
import dev.qolmod.features.treechopper.TreeChopperManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

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
        ServerPlayerEntity player; try { player = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (player == null) return 0;

        TreeChopperManager manager = QoLMod.getTreeChopperManager();
        manager.toggle(player.getUuid());
        boolean enabled = manager.isEnabled(player.getUuid());

        player.sendMessage(new LiteralText(enabled
                ? "§aTree Chopper enabled."
                : "§cTree Chopper disabled."), false);
        return 1;
    }
}
