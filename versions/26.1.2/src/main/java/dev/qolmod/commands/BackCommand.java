package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.qolmod.QoLMod;
import dev.qolmod.features.back.BackManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.packet.s2c.play.PositionFlag;

import java.util.Set;

/**
 * Registers /back (previous location) and /dback (death location).
 */
public class BackCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("back")
                .executes(BackCommand::executeBack));

        dispatcher.register(CommandManager.literal("dback")
                .executes(BackCommand::executeDback));
    }

    private static int executeBack(CommandContext<ServerCommandSource> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        BackManager backManager = QoLMod.getBackManager();
        if (backManager == null) {
            player.sendMessage(Component.literal("§cBack system not available."));
            return 0;
        }

        BackManager.LocationData loc = backManager.getPreviousLocation(player.getUUID());
        if (loc == null) {
            player.sendMessage(Component.literal("§cNo previous location to return to."));
            return 0;
        }

        // Record current location as new back
        backManager.recordTeleport(player);

        ServerLevel targetWorld = ctx.getSource().getServer().getWorld(loc.dimension);
        if (targetWorld == null) {
            player.sendMessage(Component.literal("§cThe dimension no longer exists."));
            return 0;
        }

        player.teleport(targetWorld,
                loc.pos.getX() + 0.5, loc.pos.getY(), loc.pos.getZ() + 0.5,
                Set.of(), loc.yaw, loc.pitch, true);
        player.sendMessage(Component.literal("§aTeleported to your previous location."));
        return 1;
    }

    private static int executeDback(CommandContext<ServerCommandSource> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        BackManager backManager = QoLMod.getBackManager();
        if (backManager == null) {
            player.sendMessage(Component.literal("§cBack system not available."));
            return 0;
        }

        BackManager.LocationData loc = backManager.getDeathLocation(player.getUUID());
        if (loc == null) {
            player.sendMessage(Component.literal("§cNo death location recorded."));
            return 0;
        }

        // Record current as back
        backManager.recordTeleport(player);

        ServerLevel targetWorld = ctx.getSource().getServer().getWorld(loc.dimension);
        if (targetWorld == null) {
            player.sendMessage(Component.literal("§cThe dimension no longer exists."));
            return 0;
        }

        player.teleport(targetWorld,
                loc.pos.getX() + 0.5, loc.pos.getY(), loc.pos.getZ() + 0.5,
                Set.of(), loc.yaw, loc.pitch, true);
        player.sendMessage(Component.literal("§aTeleported to your death location."));
        return 1;
    }
}
