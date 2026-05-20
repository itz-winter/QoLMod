package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.qolmod.QoLMod;
import dev.qolmod.config.QoLConfig;
import dev.qolmod.gamerule.QoLGameRules;
import dev.qolmod.teleport.TeleportManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.*;

/**
 * Registers /rtp — random teleport to a safe overworld location.
 * Configurable via qolmod.json (rtp.radius, rtp.minRadius, rtp.cooldownSeconds).
 */
public class RtpCommand {

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final int MAX_ATTEMPTS = 40;

    // Blocks that make a location unsafe
    private static final Set<net.minecraft.block.Block> UNSAFE_FLOOR = Set.of(
            Blocks.LAVA, Blocks.MAGMA_BLOCK, Blocks.FIRE, Blocks.SOUL_FIRE,
            Blocks.CACTUS, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.WITHER_ROSE,
            Blocks.SWEET_BERRY_BUSH, Blocks.POWDER_SNOW
    );

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("rtp")
                .executes(RtpCommand::executeRtp));
        // Alias
        dispatcher.register(CommandManager.literal("randomteleport")
                .executes(RtpCommand::executeRtp));
    }

    private static int executeRtp(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        if (TeleportManager.hasPending(player.getUuid())) {
            player.sendMessage(Text.literal("§cYou already have a teleport in progress."));
            return 0;
        }

        QoLConfig config = QoLMod.getConfig();
        UUID id = player.getUuid();

        // Cooldown check
        Long lastUse = cooldowns.get(id);
        if (lastUse != null) {
            long elapsed = System.currentTimeMillis() - lastUse;
            long remaining = config.rtpCooldownSeconds * 1000L - elapsed;
            if (remaining > 0) {
                int secs = (int) Math.ceil(remaining / 1000.0);
                player.sendMessage(Text.literal("§cRTP is on cooldown. Wait §e" + secs + "§c second(s)."));
                return 0;
            }
        }

        ServerWorld world = (ServerWorld) player.getEntityWorld();
        // RTP only in overworld unless specifically in overworld already
        if (!world.equals(ctx.getSource().getServer().getOverworld())) {
            world = ctx.getSource().getServer().getOverworld();
        }

        player.sendMessage(Text.literal("§7Finding a safe location..."));

        int radius = config.rtpRadius;
        int minRadius = config.rtpMinRadius;
        Random rand = new Random();

        BlockPos destination = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double dist = minRadius + rand.nextDouble() * (radius - minRadius);
            int x = (int) Math.round(Math.cos(angle) * dist);
            int z = (int) Math.round(Math.sin(angle) * dist);

            // Find surface Y
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= world.getBottomY()) continue;

            BlockPos feetPos = new BlockPos(x, y, z);
            BlockPos floorPos = feetPos.down();
            BlockPos headPos = feetPos.up();

            BlockState floorState = world.getBlockState(floorPos);
            BlockState feetState = world.getBlockState(feetPos);
            BlockState headState = world.getBlockState(headPos);

            // Floor must be solid, not unsafe
            if (!floorState.isSolidBlock(world, floorPos)) continue;
            if (UNSAFE_FLOOR.contains(floorState.getBlock())) continue;
            if (floorState.getBlock() == Blocks.VOID_AIR) continue;

            // Feet and head must be passable (air/non-solid)
            if (!feetState.isAir() && feetState.isSolidBlock(world, feetPos)) continue;
            if (!headState.isAir() && headState.isSolidBlock(world, headPos)) continue;

            // Also check the feet block itself isn't dangerous
            if (UNSAFE_FLOOR.contains(feetState.getBlock())) continue;

            destination = feetPos;
            break;
        }

        if (destination == null) {
            player.sendMessage(Text.literal("§cCould not find a safe location. Try again!"));
            return 0;
        }

        // Record back and set cooldown
        if (QoLMod.getBackManager() != null) {
            QoLMod.getBackManager().recordTeleport(player);
        }
        cooldowns.put(id, System.currentTimeMillis());

        int countdown = world.getGameRules().getValue(QoLGameRules.TP_COUNTDOWN_SECONDS);
        TeleportManager.requestTeleport(
                player, world,
                destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5,
                0, 0,
                countdown,
                "§aRandom teleport! §7(" + destination.getX() + ", " + destination.getY() + ", " + destination.getZ() + ")"
        );
        return 1;
    }
}
