package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.qolmod.QoLMod;
import dev.qolmod.features.back.BackManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

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
        ServerPlayerEntity player; try { player = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (player == null) return 0;

        BackManager backManager = QoLMod.getBackManager();
        if (backManager == null) {
            player.sendMessage(new LiteralText("Â§cBack system not available."), false);
            return 0;
        }

        BackManager.LocationData loc = backManager.getPreviousLocation(player.getUuid());
        if (loc == null) {
            player.sendMessage(new LiteralText("Â§cNo previous location to return to."), false);
            return 0;
        }

        // Record current location as new back
        backManager.recordTeleport(player);

        ServerWorld targetWorld = ctx.getSource().getServer().getWorld(loc.dimension);
        if (targetWorld == null) {
            player.sendMessage(new LiteralText("Â§cThe dimension no longer exists."), false);
            return 0;
        }

        player.teleport(targetWorld,
                loc.pos.getX() + 0.5, loc.pos.getY(), loc.pos.getZ() + 0.5,
                loc.yaw, loc.pitch);
        player.sendMessage(new LiteralText("Â§aTeleported to your previous location."), false);
        return 1;
    }

    private static int executeDback(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player; try { player = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (player == null) return 0;

        BackManager backManager = QoLMod.getBackManager();
        if (backManager == null) {
            player.sendMessage(new LiteralText("Â§cBack system not available."), false);
            return 0;
        }

        BackManager.LocationData loc = backManager.getDeathLocation(player.getUuid());
        if (loc == null) {
            player.sendMessage(new LiteralText("Â§cNo death location recorded."), false);
            return 0;
        }

        // Record current as back
        backManager.recordTeleport(player);

        ServerWorld targetWorld = ctx.getSource().getServer().getWorld(loc.dimension);
        if (targetWorld == null) {
            player.sendMessage(new LiteralText("Â§cThe dimension no longer exists."), false);
            return 0;
        }

        player.teleport(targetWorld,
                loc.pos.getX() + 0.5, loc.pos.getY(), loc.pos.getZ() + 0.5,
                loc.yaw, loc.pitch);
        player.sendMessage(new LiteralText("Â§aTeleported to your death location."), false);
        return 1;
    }
}
