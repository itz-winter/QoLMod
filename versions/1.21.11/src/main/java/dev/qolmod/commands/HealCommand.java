package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /heal — restores the player to full health and clears all status effects.
 * Only available when cheats are enabled (permission level 2 / op).
 */
public class HealCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("heal")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(HealCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cThis command can only be used by a player."));
            return 0;
        }

        player.setHealth(player.getMaxHealth());
        player.clearStatusEffects();
        player.setAbsorptionAmount(0f);
        player.sendMessage(Text.literal("§aHealed to full health."));
        return 1;
    }
}
