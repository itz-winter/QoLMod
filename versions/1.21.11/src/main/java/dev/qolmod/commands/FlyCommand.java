package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /fly — toggles flight for the executing player.
 * Only available when cheats are enabled (permission level 2 / op).
 */
public class FlyCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("fly")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(FlyCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cThis command can only be used by a player."));
            return 0;
        }

        PlayerAbilities abilities = player.getAbilities();

        if (!abilities.allowFlying) {
            // Enable fly
            abilities.allowFlying = true;
            abilities.flying = true;
            player.sendAbilitiesUpdate();
            player.sendMessage(Text.literal("§aFlight enabled."));
        } else {
            // Disable fly — also stop flying
            abilities.allowFlying = false;
            abilities.flying = false;
            player.sendAbilitiesUpdate();
            player.sendMessage(Text.literal("§cFlight disabled."));
        }
        return 1;
    }
}
