package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Registers /kick <player> [reason].
 */
public class KickCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("kick")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(suggestOnlinePlayers())
                        .executes(ctx -> executeKick(ctx, null))
                        .then(CommandManager.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> executeKick(ctx, StringArgumentType.getString(ctx, "reason"))))));
    }

    private static int executeKick(CommandContext<ServerCommandSource> ctx, String reason) {
        String targetName = StringArgumentType.getString(ctx, "player");
        ServerPlayerEntity target = ctx.getSource().getServer().getPlayerManager().getPlayer(targetName);

        if (target == null) {
            ctx.getSource().sendMessage(Text.literal("§cPlayer not found: " + targetName));
            return 0;
        }

        String reasonStr = reason != null ? reason : "Kicked by operator";
        target.networkHandler.disconnect(Text.literal("§cKicked: " + reasonStr));
        ctx.getSource().sendMessage(Text.literal("§aKicked " + targetName + ". Reason: " + reasonStr));

        return 1;
    }

    private static SuggestionProvider<ServerCommandSource> suggestOnlinePlayers() {
        return (ctx, builder) -> CommandSource.suggestMatching(
                ctx.getSource().getServer().getPlayerManager().getPlayerList().stream()
                        .map(p -> p.getName().getString()),
                builder
        );
    }
}
