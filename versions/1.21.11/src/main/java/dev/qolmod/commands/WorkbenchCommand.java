package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.qolmod.QoLMod;
import net.minecraft.screen.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class WorkbenchCommand {

    private static ScreenHandlerContext virtualContext(ServerPlayerEntity player) {
        return new ScreenHandlerContext() {
            @Override
            public <T> Optional<T> get(BiFunction<World, BlockPos, T> function) {
                return Optional.empty();
            }
            @Override
            public void run(BiConsumer<World, BlockPos> consumer) {
                consumer.accept(player.getEntityWorld(), player.getBlockPos());
            }
        };
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        registerCraft(dispatcher);
        registerEnderChest(dispatcher);
        registerAnvil(dispatcher);
        registerGrindstone(dispatcher);
        registerStonecutter(dispatcher);
        registerSmithing(dispatcher);
        registerCartography(dispatcher);
        registerLoom(dispatcher);
    }

    private static void registerCraft(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("workbench")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchCraft)
                .executes(ctx -> openCrafting(ctx.getSource())));
        dispatcher.register(CommandManager.literal("wb")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchCraft)
                .executes(ctx -> openCrafting(ctx.getSource())));
        dispatcher.register(CommandManager.literal("craft")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchCraft)
                .executes(ctx -> openCrafting(ctx.getSource())));
    }

    private static void registerEnderChest(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("enderchest")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchEnderChest)
                .executes(ctx -> openEnderChest(ctx.getSource())));
        dispatcher.register(CommandManager.literal("ec")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchEnderChest)
                .executes(ctx -> openEnderChest(ctx.getSource())));
        dispatcher.register(CommandManager.literal("echest")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchEnderChest)
                .executes(ctx -> openEnderChest(ctx.getSource())));
    }

    private static void registerAnvil(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("anvil")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchAnvil)
                .executes(ctx -> openAnvil(ctx.getSource())));
    }

    private static void registerGrindstone(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("grindstone")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchGrindstone)
                .executes(ctx -> openGrindstone(ctx.getSource())));
        dispatcher.register(CommandManager.literal("gstone")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchGrindstone)
                .executes(ctx -> openGrindstone(ctx.getSource())));
    }

    private static void registerStonecutter(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("stonecutter")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchStonecutter)
                .executes(ctx -> openStonecutter(ctx.getSource())));
        dispatcher.register(CommandManager.literal("scutter")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchStonecutter)
                .executes(ctx -> openStonecutter(ctx.getSource())));
    }

    private static void registerSmithing(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("smithing")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchSmithing)
                .executes(ctx -> openSmithing(ctx.getSource())));
    }

    private static void registerCartography(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("cartography")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchCartography)
                .executes(ctx -> openCartography(ctx.getSource())));
    }

    private static void registerLoom(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("loom")
                .requires(src -> QoLMod.getConfig().commandWorkbenchEnabled && QoLMod.getConfig().commandWorkbenchLoom)
                .executes(ctx -> openLoom(ctx.getSource())));
    }

    private static int openCrafting(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ScreenHandlerContext ctx = virtualContext(player);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new CraftingScreenHandler(syncId, inv, ctx),
                Text.literal("Crafting")
        ));
        return 1;
    }

    private static int openEnderChest(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> GenericContainerScreenHandler.createGeneric9x3(syncId, inv, player.getEnderChestInventory()),
                Text.literal("Ender Chest")
        ));
        return 1;
    }

    private static int openAnvil(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ScreenHandlerContext ctx = virtualContext(player);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new AnvilScreenHandler(syncId, inv, ctx),
                Text.literal("Anvil")
        ));
        return 1;
    }

    private static int openGrindstone(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ScreenHandlerContext ctx = virtualContext(player);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new GrindstoneScreenHandler(syncId, inv, ctx),
                Text.literal("Grindstone")
        ));
        return 1;
    }

    private static int openStonecutter(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ScreenHandlerContext ctx = virtualContext(player);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new StonecutterScreenHandler(syncId, inv, ctx),
                Text.literal("Stonecutter")
        ));
        return 1;
    }

    private static int openSmithing(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ScreenHandlerContext ctx = virtualContext(player);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new SmithingScreenHandler(syncId, inv, ctx),
                Text.literal("Smithing Table")
        ));
        return 1;
    }

    private static int openCartography(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ScreenHandlerContext ctx = virtualContext(player);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new CartographyTableScreenHandler(syncId, inv, ctx),
                Text.literal("Cartography Table")
        ));
        return 1;
    }

    private static int openLoom(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        ScreenHandlerContext ctx = virtualContext(player);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new LoomScreenHandler(syncId, inv, ctx),
                Text.literal("Loom")
        ));
        return 1;
    }
}