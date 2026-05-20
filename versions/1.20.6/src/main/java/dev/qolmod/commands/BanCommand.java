package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.qolmod.util.DurationParser;
import net.minecraft.command.CommandSource;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.BannedPlayerList;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.mojang.authlib.GameProfile;

import java.util.Date;

/**
 * Registers /ban with duration support and /unban.
 * Format: /ban <player> [duration] [reason]
 * Duration: "1d2h30m" or "permanent"
 */
public class BanCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /ban <player> [duration] [reason...]
        dispatcher.register(CommandManager.literal("ban")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(suggestOnlinePlayers())
                        .executes(ctx -> executeBan(ctx, null, null))
                        .then(CommandManager.argument("duration", StringArgumentType.word())
                                .executes(ctx -> executeBan(ctx, StringArgumentType.getString(ctx, "duration"), null))
                                .then(CommandManager.argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> executeBan(ctx,
                                                StringArgumentType.getString(ctx, "duration"),
                                                StringArgumentType.getString(ctx, "reason")))))));

        // /unban <player>
        dispatcher.register(CommandManager.literal("unban")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(BanCommand::executeUnban)));

        // /pardon (alias for unban)
        dispatcher.register(CommandManager.literal("pardon")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(BanCommand::executeUnban)));
    }

    private static int executeBan(CommandContext<ServerCommandSource> ctx, String durationStr, String reason) {
        String targetName = StringArgumentType.getString(ctx, "player");
        ServerCommandSource source = ctx.getSource();
        var server = source.getServer();

        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
        if (target == null) {
            source.sendMessage(Text.literal("§cPlayer not found: " + targetName));
            return 0;
        }

        Date expiry = null;
        String durationDisplay = "permanently";

        if (durationStr != null && !durationStr.equalsIgnoreCase("permanent")) {
            long seconds = DurationParser.parseToSeconds(durationStr);
            if (seconds <= 0) {
                source.sendMessage(Text.literal("§cInvalid duration format. Use: 1d2h30m"));
                return 0;
            }
            expiry = new Date(System.currentTimeMillis() + seconds * 1000);
            durationDisplay = "for " + DurationParser.format(seconds);
        }

        String reasonStr = reason != null ? reason : "Banned by operator";
        String sourceName = source.getName();

        BannedPlayerList banList = server.getPlayerManager().getUserBanList();
        BannedPlayerEntry entry = new BannedPlayerEntry(target.getGameProfile(),
                new Date(), sourceName, expiry, reasonStr);
        banList.add(entry);

        target.networkHandler.disconnect(Text.literal("§cYou have been banned " + durationDisplay +
                ".\n§7Reason: " + reasonStr));

        source.sendMessage(Text.literal("§aBanned " + targetName + " " + durationDisplay + "."));
        if (reason != null) {
            source.sendMessage(Text.literal("§7Reason: " + reasonStr));
        }

        return 1;
    }

    private static int executeUnban(CommandContext<ServerCommandSource> ctx) {
        String targetName = StringArgumentType.getString(ctx, "player");
        ServerCommandSource source = ctx.getSource();
        var server = source.getServer();

        BannedPlayerList banList = server.getPlayerManager().getUserBanList();
        var profile = server.getUserCache().findByName(targetName);

        if (profile.isPresent()) {
            if (banList.contains(profile.get())) {
                banList.remove(profile.get());
                source.sendMessage(Text.literal("§aUnbanned " + targetName + "."));
                return 1;
            }
        }

        source.sendMessage(Text.literal("§c" + targetName + " is not banned."));
        return 0;
    }

    private static SuggestionProvider<ServerCommandSource> suggestOnlinePlayers() {
        return (ctx, builder) -> CommandSource.suggestMatching(
                ctx.getSource().getServer().getPlayerManager().getPlayerList().stream()
                        .map(p -> p.getName().getString()),
                builder
        );
    }
}
