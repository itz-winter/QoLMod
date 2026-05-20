package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.qolmod.QoLMod;
import dev.qolmod.features.teleport.TpaManager;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Registers /tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel
 */
public class TpaCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /tpa <player>
        dispatcher.register(CommandManager.literal("tpa")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(suggestOnlinePlayers())
                        .executes(ctx -> executeTpa(ctx, false))));

        // /tpahere <player>
        dispatcher.register(CommandManager.literal("tpahere")
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(suggestOnlinePlayers())
                        .executes(ctx -> executeTpa(ctx, true))));

        // /tpaccept [player]
        dispatcher.register(CommandManager.literal("tpaccept")
                .executes(ctx -> executeAccept(ctx, null))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(suggestOnlinePlayers())
                        .executes(ctx -> executeAccept(ctx, StringArgumentType.getString(ctx, "player")))));

        // /tpdeny [player]
        dispatcher.register(CommandManager.literal("tpdeny")
                .executes(ctx -> executeDeny(ctx, null))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests(suggestOnlinePlayers())
                        .executes(ctx -> executeDeny(ctx, StringArgumentType.getString(ctx, "player")))));

        // /tpcancel
        dispatcher.register(CommandManager.literal("tpcancel")
                .executes(TpaCommand::executeCancel));
    }

    private static int executeTpa(CommandContext<ServerCommandSource> ctx, boolean here) {
        ServerPlayerEntity sender = ctx.getSource().getPlayer();
        if (sender == null) return 0;

        String targetName = StringArgumentType.getString(ctx, "player");
        ServerPlayerEntity target = ctx.getSource().getServer().getPlayerManager().getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(Text.literal("§cPlayer not found: " + targetName));
            return 0;
        }

        if (target == sender) {
            sender.sendMessage(Text.literal("§cYou can't teleport to yourself."));
            return 0;
        }

        TpaManager tpaManager = QoLMod.getTpaManager();
        tpaManager.sendRequest(sender, target, here);

        String type = here ? "tpahere" : "tpa";
        sender.sendMessage(Text.literal("§aTeleport request sent to " + target.getName().getString() + "."));
        target.sendMessage(Text.literal("§e" + sender.getName().getString() + " wants to " +
                (here ? "teleport you to them" : "teleport to you") +
                ". Type §a/tpaccept§e or §c/tpdeny§e."));

        return 1;
    }

    private static int executeAccept(CommandContext<ServerCommandSource> ctx, String senderName) {
        ServerPlayerEntity target = ctx.getSource().getPlayer();
        if (target == null) return 0;

        TpaManager tpaManager = QoLMod.getTpaManager();
        java.util.UUID senderId = null;
        if (senderName != null) {
            ServerPlayerEntity senderPlayer = ctx.getSource().getServer().getPlayerManager().getPlayer(senderName);
            if (senderPlayer != null) senderId = senderPlayer.getUuid();
        }

        TpaManager.TpaRequest request = tpaManager.acceptRequest(target.getUuid(), senderId);
        if (request == null) {
            target.sendMessage(Text.literal("§cNo pending teleport requests."));
            return 0;
        }

        // Perform the teleport
        ServerPlayerEntity sender = ctx.getSource().getServer().getPlayerManager().getPlayer(request.senderId);
        if (sender == null) {
            target.sendMessage(Text.literal("§cThe requesting player is no longer online."));
            return 0;
        }

        // Record back location
        if (QoLMod.getBackManager() != null) {
            if (request.tpaHere) {
                QoLMod.getBackManager().recordTeleport(target);
            } else {
                QoLMod.getBackManager().recordTeleport(sender);
            }
        }

        if (request.tpaHere) {
            target.teleport(sender.getServerWorld(),
                    sender.getX(), sender.getY(), sender.getZ(),
                    sender.getYaw(), sender.getPitch());
            target.sendMessage(Text.literal("§aTeleported to " + sender.getName().getString() + "."));
            sender.sendMessage(Text.literal("§a" + target.getName().getString() + " accepted your request."));
        } else {
            sender.teleport(target.getServerWorld(),
                    target.getX(), target.getY(), target.getZ(),
                    target.getYaw(), target.getPitch());
            sender.sendMessage(Text.literal("§aTeleported to " + target.getName().getString() + "."));
            target.sendMessage(Text.literal("§a" + sender.getName().getString() + "'s teleport request accepted."));
        }

        return 1;
    }

    private static int executeDeny(CommandContext<ServerCommandSource> ctx, String senderName) {
        ServerPlayerEntity target = ctx.getSource().getPlayer();
        if (target == null) return 0;

        TpaManager tpaManager = QoLMod.getTpaManager();
        java.util.UUID senderId = null;
        if (senderName != null) {
            ServerPlayerEntity senderPlayer = ctx.getSource().getServer().getPlayerManager().getPlayer(senderName);
            if (senderPlayer != null) senderId = senderPlayer.getUuid();
        }

        TpaManager.TpaRequest request = tpaManager.denyRequest(target.getUuid(), senderId);
        if (request == null) {
            target.sendMessage(Text.literal("§cNo pending teleport requests."));
            return 0;
        }

        ServerPlayerEntity sender = ctx.getSource().getServer().getPlayerManager().getPlayer(request.senderId);
        target.sendMessage(Text.literal("§cTeleport request denied."));
        if (sender != null) {
            sender.sendMessage(Text.literal("§c" + target.getName().getString() + " denied your teleport request."));
        }

        return 1;
    }

    private static int executeCancel(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity sender = ctx.getSource().getPlayer();
        if (sender == null) return 0;

        TpaManager tpaManager = QoLMod.getTpaManager();
        if (!tpaManager.hasOutgoing(sender.getUuid())) {
            sender.sendMessage(Text.literal("§cYou have no outgoing teleport request."));
            return 0;
        }

        tpaManager.cancelOutgoing(sender.getUuid());
        sender.sendMessage(Text.literal("§aTeleport request cancelled."));
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
