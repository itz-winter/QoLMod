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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

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
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String channelName = StringArgumentType.getString(ctx, "name");
        ChannelManager channelManager = QoLMod.getChannelManager();

        if (!channelManager.channelExists(channelName)) {
            player.sendMessage(Component.literal("§cChannel not found: " + channelName +
                    ". Use §e/channel list §cto see available channels."));
            return 0;
        }

        channelManager.setChannel(player.getUUID(), channelName);
        player.sendMessage(Component.literal("§aSwitched to channel: §f" + channelName));
        return 1;
    }

    private static int executeList(CommandContext<ServerCommandSource> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        ChannelManager channelManager = QoLMod.getChannelManager();
        String current = channelManager.getChannel(player.getUUID());

        player.sendMessage(Component.literal("§6=== Channels ==="));
        for (String ch : channelManager.getAvailableChannels()) {
            String prefix = ch.equals(current) ? "§a▸ " : "§7  ";
            player.sendMessage(Component.literal(prefix + ch));
        }
        return 1;
    }

    private static SuggestionProvider<ServerCommandSource> suggestChannels() {
        return (ctx, builder) -> CommandSource.suggestMatching(
                QoLMod.getChannelManager().getAvailableChannels(), builder
        );
    }
}
