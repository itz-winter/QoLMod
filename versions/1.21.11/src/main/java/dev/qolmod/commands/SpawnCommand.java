package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.qolmod.QoLMod;
import dev.qolmod.gamerule.QoLGameRules;
import dev.qolmod.teleport.TeleportManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Registers /spawn — teleports the player (or a target) to the world's spawn point.
 * Records a back location before teleporting.
 */
public class SpawnCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /spawn — teleport self to spawn
        dispatcher.register(CommandManager.literal("spawn")
                .executes(SpawnCommand::executeSpawnSelf)
                // /spawn <player> — op-only, send another player to spawn
                .then(CommandManager.argument("target", StringArgumentType.word())
                        .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                        .executes(SpawnCommand::executeSpawnOther)));
    }

    private static int executeSpawnSelf(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        if (TeleportManager.hasPending(player.getUuid())) {
            player.sendMessage(Text.literal("§cYou already have a teleport in progress."));
            return 0;
        }

        var server = ctx.getSource().getServer();
        return doSpawnTeleport(player, server.getOverworld(), server);
    }

    private static int executeSpawnOther(CommandContext<ServerCommandSource> ctx) {
        String targetName = StringArgumentType.getString(ctx, "target");
        ServerPlayerEntity target = ctx.getSource().getServer().getPlayerManager().getPlayer(targetName);
        if (target == null) {
            ctx.getSource().sendError(Text.literal("§cPlayer not found: " + targetName));
            return 0;
        }

        var server = ctx.getSource().getServer();
        int result = doSpawnTeleport(target, server.getOverworld(), server);
        if (result == 1) {
            ctx.getSource().sendMessage(Text.literal("§aSent §e" + target.getName().getString() + " §ato spawn."));
        }
        return result;
    }

    private static int doSpawnTeleport(ServerPlayerEntity player, ServerWorld overworld,
                                       net.minecraft.server.MinecraftServer server) {
        // Spawn coordinates from overworld spawn point
        BlockPos spawnPos = overworld.getSpawnPoint().getPos();

        // Record back location
        if (QoLMod.getBackManager() != null) {
            QoLMod.getBackManager().recordTeleport(player);
        }

        int countdown = overworld.getGameRules().getValue(QoLGameRules.TP_COUNTDOWN_SECONDS);
        TeleportManager.requestTeleport(
                player, overworld,
                spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                0, 0,
                countdown,
                "§aTeleported to spawn."
        );
        return 1;
    }
}
