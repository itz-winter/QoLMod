package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /god  — enables god mode (invulnerability).
 * /ungod — disables god mode.
 * Only available when cheats are enabled (permission level 2 / op).
 */
public class GodCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("god")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(ctx -> setGod(ctx, true)));

        dispatcher.register(CommandManager.literal("ungod")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(ctx -> setGod(ctx, false)));
    }

    private static int setGod(CommandContext<ServerCommandSource> ctx, boolean enable) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cThis command can only be used by a player."));
            return 0;
        }

        PlayerAbilities abilities = player.getAbilities();
        abilities.invulnerable = enable;
        player.sendAbilitiesUpdate();

        if (enable) {
            player.sendMessage(Text.literal("§aGod mode enabled."));
        } else {
            player.sendMessage(Text.literal("§cGod mode disabled."));
        }
        return 1;
    }
}
