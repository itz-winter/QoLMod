package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

/**
 * Registers gamemode alias commands:
 *   /gm <mode>  — mode can be 0/1/2/3 or survival/creative/adventure/spectator (or s/c/a/sp)
 *   /gms        — survival
 *   /gmc        — creative
 *   /gma        — adventure
 *   /gmsp       — spectator
 * All require permission level 2 (operator).
 */
public class GamemodeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /gm <mode>
        dispatcher.register(CommandManager.literal("gm")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .then(CommandManager.argument("mode", StringArgumentType.word())
                        .executes(ctx -> executeGm(ctx, StringArgumentType.getString(ctx, "mode")))));

        // /gms
        dispatcher.register(CommandManager.literal("gms")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(ctx -> setGamemode(ctx.getSource(), GameMode.SURVIVAL)));

        // /gmc
        dispatcher.register(CommandManager.literal("gmc")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(ctx -> setGamemode(ctx.getSource(), GameMode.CREATIVE)));

        // /gma
        dispatcher.register(CommandManager.literal("gma")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(ctx -> setGamemode(ctx.getSource(), GameMode.ADVENTURE)));

        // /gmsp
        dispatcher.register(CommandManager.literal("gmsp")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(ctx -> setGamemode(ctx.getSource(), GameMode.SPECTATOR)));
    }

    private static int executeGm(CommandContext<ServerCommandSource> ctx, String modeStr) {
        GameMode mode = parseGameMode(modeStr);
        if (mode == null) {
            ctx.getSource().sendError(Text.literal("§cUnknown gamemode: §e" + modeStr
                    + "§c. Use survival/creative/adventure/spectator or 0/1/2/3."));
            return 0;
        }
        return setGamemode(ctx.getSource(), mode);
    }

    private static int setGamemode(ServerCommandSource source, GameMode mode) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§cThis command can only be used by a player."));
            return 0;
        }
        player.changeGameMode(mode);
        String name = switch (mode) {
            case SURVIVAL -> "§aSurvival";
            case CREATIVE -> "§bCreative";
            case ADVENTURE -> "§eAdventure";
            case SPECTATOR -> "§7Spectator";
        };
        player.sendMessage(Text.literal("§7Gamemode set to " + name + "§7."));
        return 1;
    }

    private static GameMode parseGameMode(String s) {
        return switch (s.toLowerCase()) {
            case "0", "s", "survival" -> GameMode.SURVIVAL;
            case "1", "c", "creative" -> GameMode.CREATIVE;
            case "2", "a", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }
}
