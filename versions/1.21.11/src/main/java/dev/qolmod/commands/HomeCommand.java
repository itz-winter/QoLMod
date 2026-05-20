package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.qolmod.QoLMod;
import dev.qolmod.config.QoLConfig;
import dev.qolmod.features.homes.HomeGUI;
import dev.qolmod.features.homes.HomeManager;
import dev.qolmod.features.homes.HomeTeleportHelper;
import dev.qolmod.gamerule.QoLGameRules;
import dev.qolmod.teleport.TeleportManager;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Registers /home, /sethome, /delhome, /homes, /renhome
 */
public class HomeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /home [name] - default "home"
        dispatcher.register(CommandManager.literal("home")
                .executes(ctx -> executeHome(ctx, "home"))
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(suggestHomes())
                        .executes(ctx -> executeHome(ctx, StringArgumentType.getString(ctx, "name")))));

        // /sethome [name] - default "home"
        dispatcher.register(CommandManager.literal("sethome")
                .executes(ctx -> executeSetHome(ctx, "home"))
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> executeSetHome(ctx, StringArgumentType.getString(ctx, "name")))));

        // /delhome <name>
        dispatcher.register(CommandManager.literal("delhome")
                .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(suggestHomes())
                        .executes(HomeCommand::executeDelHome)));

        // /homes - open GUI
        dispatcher.register(CommandManager.literal("homes")
                .executes(HomeCommand::executeListHomes));

        // /renhome <old> <new>
        dispatcher.register(CommandManager.literal("renhome")
                .then(CommandManager.argument("old", StringArgumentType.word())
                        .suggests(suggestHomes())
                        .then(CommandManager.argument("new", StringArgumentType.word())
                                .executes(HomeCommand::executeRenHome))));
    }

    private static int executeHome(CommandContext<ServerCommandSource> ctx, String name) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        HomeManager homeManager = QoLMod.getHomeManager();
        if (homeManager == null) {
            player.sendMessage(Text.literal("§cHome system not available."));
            return 0;
        }

        if (TeleportManager.hasPending(player.getUuid())) {
            player.sendMessage(Text.literal("§cYou already have a teleport in progress."));
            return 0;
        }

        HomeManager.HomeData home = homeManager.getHome(player.getUuid(), name);
        if (home == null) {
            player.sendMessage(Text.literal("§cHome not found: " + name));
            return 0;
        }

        HomeTeleportHelper.teleport(player, homeManager, name);
        return 1;
    }

    private static int executeSetHome(CommandContext<ServerCommandSource> ctx, String name) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        HomeManager homeManager = QoLMod.getHomeManager();
        if (homeManager == null) {
            player.sendMessage(Text.literal("§cHome system not available."));
            return 0;
        }

        int maxHomes = ctx.getSource().getWorld().getGameRules()
                .getValue(QoLGameRules.HOMES_MAX_PER_PLAYER);
        int currentCount = homeManager.getHomeCount(player.getUuid());
        boolean isUpdate = homeManager.getHome(player.getUuid(), name) != null;

        if (!isUpdate && currentCount >= maxHomes) {
            player.sendMessage(Text.literal("§cYou have reached the maximum number of homes (" + maxHomes + ")."));
            return 0;
        }

        homeManager.setHome(player.getUuid(), name,
                player.getEntityWorld().getRegistryKey(),
                player.getBlockPos(),
                player.getYaw(),
                player.getPitch());

        player.sendMessage(Text.literal("§aHome " + (isUpdate ? "updated" : "set") + ": §f" + name));
        return 1;
    }

    private static int executeDelHome(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        HomeManager homeManager = QoLMod.getHomeManager();
        if (homeManager == null) {
            player.sendMessage(Text.literal("§cHome system not available."));
            return 0;
        }

        if (homeManager.deleteHome(player.getUuid(), name)) {
            player.sendMessage(Text.literal("§aHome deleted: §f" + name));
            return 1;
        } else {
            player.sendMessage(Text.literal("§cHome not found: " + name));
            return 0;
        }
    }

    private static int executeListHomes(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        HomeManager homeManager = QoLMod.getHomeManager();
        if (homeManager == null) {
            player.sendMessage(Text.literal("§cHome system not available."));
            return 0;
        }

        HomeGUI.open(player, homeManager);
        return 1;
    }

    private static int executeRenHome(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String oldName = StringArgumentType.getString(ctx, "old");
        String newName = StringArgumentType.getString(ctx, "new");

        HomeManager homeManager = QoLMod.getHomeManager();
        if (homeManager == null) {
            player.sendMessage(Text.literal("§cHome system not available."));
            return 0;
        }

        if (newName.length() > 32) {
            player.sendMessage(Text.literal("§cHome name too long (max 32 characters)."));
            return 0;
        }

        boolean renamed = homeManager.renameHome(player.getUuid(), oldName, newName);
        if (renamed) {
            player.sendMessage(Text.literal("§aRenamed home §e" + oldName + " §ato §e" + newName + "§a."));
            return 1;
        } else {
            if (homeManager.getHome(player.getUuid(), oldName) == null) {
                player.sendMessage(Text.literal("§cHome not found: " + oldName));
            } else {
                player.sendMessage(Text.literal("§cA home named §e" + newName + " §calready exists."));
            }
            return 0;
        }
    }

    private static SuggestionProvider<ServerCommandSource> suggestHomes() {
        return (ctx, builder) -> {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player == null) return builder.buildFuture();
            HomeManager homeManager = QoLMod.getHomeManager();
            if (homeManager == null) return builder.buildFuture();
            return CommandSource.suggestMatching(homeManager.getHomes(player.getUuid()).keySet(), builder);
        };
    }
}

