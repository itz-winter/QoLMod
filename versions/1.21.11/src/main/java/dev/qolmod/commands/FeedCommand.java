package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /feed  — fills the player's hunger and saturation to maximum.
 * /starve — empties the player's hunger and saturation to zero.
 * Only available when cheats are enabled (permission level 2 / op).
 */
public class FeedCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("feed")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(ctx -> execute(ctx, true)));

        dispatcher.register(CommandManager.literal("starve")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(ctx -> execute(ctx, false)));
    }

    private static int execute(CommandContext<ServerCommandSource> ctx, boolean feed) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("§cThis command can only be used by a player."));
            return 0;
        }

        HungerManager hunger = player.getHungerManager();
        if (feed) {
            hunger.setFoodLevel(20);
            hunger.setSaturationLevel(20f);
            player.sendMessage(Text.literal("§aHunger and saturation restored."));
        } else {
            hunger.setFoodLevel(0);
            hunger.setSaturationLevel(0f);
            player.sendMessage(Text.literal("§cHunger and saturation emptied."));
        }
        return 1;
    }
}
