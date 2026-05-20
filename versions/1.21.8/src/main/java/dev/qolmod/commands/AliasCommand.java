package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.qolmod.config.CommandAliasManager;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Map;

/**
 * Registers /alias add <name> <command>, /alias remove <name>, /alias list
 * Per-player command aliases.
 */
public class AliasCommand {

    private static final CommandAliasManager aliasManager = new CommandAliasManager(
            FabricLoader.getInstance().getConfigDir()
    );

    public static CommandAliasManager getAliasManager() {
        return aliasManager;
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("alias")
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                        .executes(AliasCommand::executeAdd))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .suggests(suggestAliases())
                                .executes(AliasCommand::executeRemove)))
                .then(CommandManager.literal("list")
                        .executes(AliasCommand::executeList)));
    }

    private static int executeAdd(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        String command = StringArgumentType.getString(ctx, "command");

        aliasManager.load(player.getUuid().toString());
        aliasManager.addAlias(name, command);
        aliasManager.save(player.getUuid().toString());

        player.sendMessage(Text.literal("§aAlias added: §f/" + name + " §7→ §f/" + command));
        return 1;
    }

    private static int executeRemove(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(ctx, "name");

        aliasManager.load(player.getUuid().toString());
        boolean hadAlias = aliasManager.getAll().containsKey(name.toLowerCase(java.util.Locale.ROOT));
        aliasManager.removeAlias(name);
        aliasManager.save(player.getUuid().toString());

        if (hadAlias) {
            player.sendMessage(Text.literal("§aAlias removed: §f/" + name));
            return 1;
        } else {
            player.sendMessage(Text.literal("§cAlias not found: " + name));
            return 0;
        }
    }

    private static int executeList(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        aliasManager.load(player.getUuid().toString());
        Map<String, String> aliases = aliasManager.getAll();

        if (aliases.isEmpty()) {
            player.sendMessage(Text.literal("§7You have no aliases. Use §e/alias add <name> <command>§7."));
            return 0;
        }

        player.sendMessage(Text.literal("§6=== Your Aliases ==="));
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            player.sendMessage(Text.literal("§e/" + entry.getKey() + " §7→ §f/" + entry.getValue()));
        }
        return 1;
    }

    private static SuggestionProvider<ServerCommandSource> suggestAliases() {
        return (ctx, builder) -> {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player == null) return builder.buildFuture();
            aliasManager.load(player.getUuid().toString());
            return CommandSource.suggestMatching(aliasManager.getAll().keySet(), builder);
        };
    }
}
