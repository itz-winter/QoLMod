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
import net.minecraft.text.LiteralText;
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
        ServerPlayerEntity sender; try { sender = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (sender == null) return 0;

        String targetName = StringArgumentType.getString(ctx, "player");
        ServerPlayerEntity target = ctx.getSource().getServer().getPlayerManager().getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(new LiteralText("Â§cPlayer not found: " + targetName), false);
            return 0;
        }

        if (target == sender) {
            sender.sendMessage(new LiteralText("Â§cYou can't teleport to yourself."), false);
            return 0;
        }

        TpaManager tpaManager = QoLMod.getTpaManager();
        tpaManager.sendRequest(sender, target, here);

        String type = here ? "tpahere" : "tpa";
        sender.sendMessage(new LiteralText("Â§aTeleport request sent to " + target.getName().getString() + "."), false);
        target.sendMessage(new LiteralText("§e" + sender.getName().getString() + " wants to " +
                (here ? "teleport you to them" : "teleport to you") +
                ". Type §a/tpaccept§e or §c/tpdeny§e."), false);

        return 1;
    }

    private static int executeAccept(CommandContext<ServerCommandSource> ctx, String senderName) {
        ServerPlayerEntity target; try { target = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (target == null) return 0;

        TpaManager tpaManager = QoLMod.getTpaManager();
        java.util.UUID senderId = null;
        if (senderName != null) {
            ServerPlayerEntity senderPlayer = ctx.getSource().getServer().getPlayerManager().getPlayer(senderName);
            if (senderPlayer != null) senderId = senderPlayer.getUuid();
        }

        TpaManager.TpaRequest request = tpaManager.acceptRequest(target.getUuid(), senderId);
        if (request == null) {
            target.sendMessage(new LiteralText("Â§cNo pending teleport requests."), false);
            return 0;
        }

        // Perform the teleport
        ServerPlayerEntity sender = ctx.getSource().getServer().getPlayerManager().getPlayer(request.senderId);
        if (sender == null) {
            target.sendMessage(new LiteralText("Â§cThe requesting player is no longer online."), false);
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
            target.teleport(sender.getWorld(),
                    sender.getX(), sender.getY(), sender.getZ(),
                    sender.getYaw(), sender.getPitch());
            target.sendMessage(new LiteralText("Â§aTeleported to " + sender.getName().getString() + "."), false);
            sender.sendMessage(new LiteralText("Â§a" + target.getName().getString() + " accepted your request."), false);
        } else {
            sender.teleport(target.getWorld(),
                    target.getX(), target.getY(), target.getZ(),
                    target.getYaw(), target.getPitch());
            sender.sendMessage(new LiteralText("Â§aTeleported to " + target.getName().getString() + "."), false);
            target.sendMessage(new LiteralText("Â§a" + sender.getName().getString() + "'s teleport request accepted."), false);
        }

        return 1;
    }

    private static int executeDeny(CommandContext<ServerCommandSource> ctx, String senderName) {
        ServerPlayerEntity target; try { target = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (target == null) return 0;

        TpaManager tpaManager = QoLMod.getTpaManager();
        java.util.UUID senderId = null;
        if (senderName != null) {
            ServerPlayerEntity senderPlayer = ctx.getSource().getServer().getPlayerManager().getPlayer(senderName);
            if (senderPlayer != null) senderId = senderPlayer.getUuid();
        }

        TpaManager.TpaRequest request = tpaManager.denyRequest(target.getUuid(), senderId);
        if (request == null) {
            target.sendMessage(new LiteralText("Â§cNo pending teleport requests."), false);
            return 0;
        }

        ServerPlayerEntity sender = ctx.getSource().getServer().getPlayerManager().getPlayer(request.senderId);
        target.sendMessage(new LiteralText("Â§cTeleport request denied."), false);
        if (sender != null) {
            sender.sendMessage(new LiteralText("Â§c" + target.getName().getString() + " denied your teleport request."), false);
        }

        return 1;
    }

    private static int executeCancel(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity sender; try { sender = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (sender == null) return 0;

        TpaManager tpaManager = QoLMod.getTpaManager();
        if (!tpaManager.hasOutgoing(sender.getUuid())) {
            sender.sendMessage(new LiteralText("Â§cYou have no outgoing teleport request."), false);
            return 0;
        }

        tpaManager.cancelOutgoing(sender.getUuid());
        sender.sendMessage(new LiteralText("Â§aTeleport request cancelled."), false);
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
