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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

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
        ServerPlayerEntity player; try { player = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (player == null) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        WarpManager warpManager = QoLMod.getWarpManager();
        WarpManager.WarpData warp = warpManager.getWarp(name);

        if (warp == null) {
            player.sendMessage(new LiteralText("Â§cWarp not found: " + name), false);
            return 0;
        }

        // Record back location
        if (QoLMod.getBackManager() != null) {
            QoLMod.getBackManager().recordTeleport(player);
        }

        ServerWorld targetWorld = ctx.getSource().getMinecraftServer().getWorld(warp.dimension);
        if (targetWorld == null) {
            player.sendMessage(new LiteralText("Â§cThe dimension for this warp no longer exists."), false);
            return 0;
        }

        player.teleport(targetWorld,
                warp.pos.getX() + 0.5, warp.pos.getY(), warp.pos.getZ() + 0.5,
                warp.yaw, warp.pitch);
        player.sendMessage(new LiteralText("Â§aTeleported to warp: Â§f" + warp.name), false);
        return 1;
    }

    private static int executeSetWarp(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player; try { player = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (player == null) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        WarpManager warpManager = QoLMod.getWarpManager();

        warpManager.setWarp(name, player.world.getRegistryKey(),
                player.getBlockPos(), player.yaw, player.pitch);
        warpManager.save(ctx.getSource().getMinecraftServer());

        player.sendMessage(new LiteralText("Â§aWarp set: Â§f" + name), false);
        return 1;
    }

    private static int executeDelWarp(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player; try { player = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (player == null) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        WarpManager warpManager = QoLMod.getWarpManager();

        if (warpManager.deleteWarp(name)) {
            warpManager.save(ctx.getSource().getMinecraftServer());
            player.sendMessage(new LiteralText("Â§aWarp deleted: Â§f" + name), false);
            return 1;
        } else {
            player.sendMessage(new LiteralText("Â§cWarp not found: " + name), false);
            return 0;
        }
    }

    private static int executeListWarps(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player; try { player = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (player == null) return 0;

        WarpManager warpManager = QoLMod.getWarpManager();
        var warps = warpManager.getAll();

        if (warps.isEmpty()) {
            player.sendMessage(new LiteralText("Â§7No warps have been set."), false);
            return 0;
        }

        player.sendMessage(new LiteralText("Â§6=== Warps ==="), false);
        for (WarpManager.WarpData warp : warps) {
            player.sendMessage(new LiteralText("§e" + warp.name + " §7- " +
                    warp.pos.getX() + ", " + warp.pos.getY() + ", " + warp.pos.getZ()), false);
        }
        return 1;
    }

    private static SuggestionProvider<ServerCommandSource> suggestWarps() {
        return (ctx, builder) -> CommandSource.suggestMatching(
                QoLMod.getWarpManager().getNames(), builder
        );
    }
}
