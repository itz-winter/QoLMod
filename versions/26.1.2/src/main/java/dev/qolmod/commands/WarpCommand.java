package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.qolmod.QoLMod;
import dev.qolmod.features.warps.WarpManager;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.packet.s2c.play.PositionFlag;

import java.util.Set;

/**
 * Registers /warp, /setwarp, /delwarp, /warps
 * Ops only can create/delete. Anyone can use.
 */
public class WarpCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /warp <name>
        dispatcher.register(CommandManager.literal("warp")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(suggestWarps())
                        .executes(WarpCommand::executeWarp)));

        // /setwarp <name> - requires OP level 2
        dispatcher.register(CommandManager.literal("setwarp")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(WarpCommand::executeSetWarp)));

        // /delwarp <name> - requires OP level 2
        dispatcher.register(CommandManager.literal("delwarp")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(suggestWarps())
                        .executes(WarpCommand::executeDelWarp)));

        // /warps - list all warps
        dispatcher.register(CommandManager.literal("warps")
                .executes(WarpCommand::executeListWarps));
    }

    private static int executeWarp(CommandContext<ServerCommandSource> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        WarpManager warpManager = QoLMod.getWarpManager();
        WarpManager.WarpData warp = warpManager.getWarp(name);

        if (warp == null) {
            player.sendMessage(Component.literal("§cWarp not found: " + name));
            return 0;
        }

        // Record back location
        if (QoLMod.getBackManager() != null) {
            QoLMod.getBackManager().recordTeleport(player);
        }

        ServerLevel targetWorld = ctx.getSource().getServer().getWorld(warp.dimension);
        if (targetWorld == null) {
            player.sendMessage(Component.literal("§cThe dimension for this warp no longer exists."));
            return 0;
        }

        player.teleport(targetWorld,
                warp.pos.getX() + 0.5, warp.pos.getY(), warp.pos.getZ() + 0.5,
                Set.of(), warp.yaw, warp.pitch, true);
        player.sendMessage(Component.literal("§aTeleported to warp: §f" + warp.name));
        return 1;
    }

    private static int executeSetWarp(CommandContext<ServerCommandSource> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        WarpManager warpManager = QoLMod.getWarpManager();

        warpManager.setWarp(name, player.getServerLevel().getResourceKey(),
                player.getBlockPos(), player.getYaw(), player.getPitch());
        warpManager.save(ctx.getSource().getServer());

        player.sendMessage(Component.literal("§aWarp set: §f" + name));
        return 1;
    }

    private static int executeDelWarp(CommandContext<ServerCommandSource> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        WarpManager warpManager = QoLMod.getWarpManager();

        if (warpManager.deleteWarp(name)) {
            warpManager.save(ctx.getSource().getServer());
            player.sendMessage(Component.literal("§aWarp deleted: §f" + name));
            return 1;
        } else {
            player.sendMessage(Component.literal("§cWarp not found: " + name));
            return 0;
        }
    }

    private static int executeListWarps(CommandContext<ServerCommandSource> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        WarpManager warpManager = QoLMod.getWarpManager();
        var warps = warpManager.getAll();

        if (warps.isEmpty()) {
            player.sendMessage(Component.literal("§7No warps have been set."));
            return 0;
        }

        player.sendMessage(Component.literal("§6=== Warps ==="));
        for (WarpManager.WarpData warp : warps) {
            player.sendMessage(Component.literal("§e" + warp.name + " §7- " +
                    warp.pos.getX() + ", " + warp.pos.getY() + ", " + warp.pos.getZ()));
        }
        return 1;
    }

    private static SuggestionProvider<ServerCommandSource> suggestWarps() {
        return (ctx, builder) -> CommandSource.suggestMatching(
                QoLMod.getWarpManager().getNames(), builder
        );
    }
}
