package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.qolmod.QoLMod;
import dev.qolmod.config.QoLConfig;
import dev.qolmod.features.homes.HomeManager;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Map;

/**
 * Registers /home, /sethome, /delhome, /homes
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

        // /homes - list all homes
        dispatcher.register(CommandManager.literal("homes")
                .executes(HomeCommand::executeListHomes));
    }

    private static int executeHome(CommandContext<ServerCommandSource> ctx, String name) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        HomeManager homeManager = QoLMod.getHomeManager();
        if (homeManager == null) {
            player.sendMessage(Text.literal("§cHome system not available."));
            return 0;
        }

        HomeManager.HomeData home = homeManager.getHome(player.getUuid(), name);
        if (home == null) {
            player.sendMessage(Text.literal("§cHome not found: " + name));
            return 0;
        }

        // Record back location
        if (QoLMod.getBackManager() != null) {
            QoLMod.getBackManager().recordTeleport(player);
        }

        ServerWorld targetWorld = ctx.getSource().getServer().getWorld(home.dimension);
        if (targetWorld == null) {
            player.sendMessage(Text.literal("§cThe dimension for this home no longer exists."));
            return 0;
        }

        player.teleport(targetWorld,
                home.pos.getX() + 0.5, home.pos.getY(), home.pos.getZ() + 0.5,
                home.yaw, home.pitch);
        player.sendMessage(Text.literal("§aTeleported to home: §f" + home.name));
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

        QoLConfig config = QoLMod.getConfig();
        int maxHomes = config.homesMaxPerPlayer;
        int currentCount = homeManager.getHomeCount(player.getUuid());

        // Check if updating existing or creating new
        boolean isUpdate = homeManager.getHome(player.getUuid(), name) != null;
        if (!isUpdate && currentCount >= maxHomes && !player.hasPermissionLevel(2)) {
            player.sendMessage(Text.literal("§cYou have reached the maximum number of homes (" + maxHomes + ")."));
            return 0;
        }

        homeManager.setHome(player.getUuid(), name,
                player.getWorld().getRegistryKey(),
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

        Map<String, HomeManager.HomeData> homes = homeManager.getHomes(player.getUuid());
        if (homes.isEmpty()) {
            player.sendMessage(Text.literal("§7You have no homes set. Use §e/sethome [name]§7 to set one."));
            return 0;
        }

        QoLConfig config = QoLMod.getConfig();
        player.sendMessage(Text.literal("§6=== Homes (" + homes.size() + "/" + config.homesMaxPerPlayer + ") ==="));
        for (HomeManager.HomeData home : homes.values()) {
            player.sendMessage(Text.literal("§e" + home.name + " §7- " +
                    home.pos.getX() + ", " + home.pos.getY() + ", " + home.pos.getZ()));
        }
        return 1;
    }

    private static SuggestionProvider<ServerCommandSource> suggestHomes() {
        return (ctx, builder) -> {
            ServerPlayerEntity player = ctx.getSource().getPlayer();
            if (player == null) return builder.buildFuture();
            HomeManager homeManager = QoLMod.getHomeManager();
            if (homeManager == null) return builder.buildFuture();
            return CommandSource.suggestMatching(
                    homeManager.getHomes(player.getUuid()).keySet(), builder
            );
        };
    }
}
