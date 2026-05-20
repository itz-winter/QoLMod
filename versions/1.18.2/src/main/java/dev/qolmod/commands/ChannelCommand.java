package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.qolmod.QoLMod;
import dev.qolmod.features.channel.ChannelManager;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

/**
 * Registers /channel <name>, /channel list
 */
public class ChannelCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("channel")
                .then(CommandManager.literal("list")
                        .executes(ChannelCommand::executeList))
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(suggestChannels())
                        .executes(ChannelCommand::executeSwitch)));

        // Alias
        dispatcher.register(CommandManager.literal("ch")
                .then(CommandManager.literal("list")
                        .executes(ChannelCommand::executeList))
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(suggestChannels())
                        .executes(ChannelCommand::executeSwitch)));
    }

    private static int executeSwitch(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player; try { player = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (player == null) return 0;

        String channelName = StringArgumentType.getString(ctx, "name");
        ChannelManager channelManager = QoLMod.getChannelManager();

        if (!channelManager.channelExists(channelName)) {
            player.sendMessage(new LiteralText("§cChannel not found: " + channelName +
                    ". Use §e/channel list §cto see available channels."), false);
            return 0;
        }

        channelManager.setChannel(player.getUuid(), channelName);
        player.sendMessage(new LiteralText("Â§aSwitched to channel: Â§f" + channelName), false);
        return 1;
    }

    private static int executeList(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player; try { player = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (player == null) return 0;

        ChannelManager channelManager = QoLMod.getChannelManager();
        String current = channelManager.getChannel(player.getUuid());

        player.sendMessage(new LiteralText("Â§6=== Channels ==="), false);
        for (String ch : channelManager.getAvailableChannels()) {
            String prefix = ch.equals(current) ? "Â§aâ–¸ " : "Â§7  ";
            player.sendMessage(new LiteralText(prefix + ch), false);
        }
        return 1;
    }

    private static SuggestionProvider<ServerCommandSource> suggestChannels() {
        return (ctx, builder) -> CommandSource.suggestMatching(
                QoLMod.getChannelManager().getAvailableChannels(), builder
        );
    }
}
