package dev.qolmod;

import dev.qolmod.commands.*;
import dev.qolmod.config.QoLConfig;
import dev.qolmod.features.treechopper.TreeChopperManager;
import dev.qolmod.features.back.BackManager;
import dev.qolmod.features.homes.HomeManager;
import dev.qolmod.features.teleport.TpaManager;
import dev.qolmod.features.warps.WarpManager;
import dev.qolmod.features.channel.ChannelManager;
import dev.qolmod.util.QoLConstants;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Main server-side entrypoint for QoLMod.
 * Registers commands, managers, and event handlers.
 */
public class QoLMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(QoLConstants.MOD_NAME);

    private static QoLConfig config;
    private static TreeChopperManager treeChopperManager;
    private static BackManager backManager;
    private static HomeManager homeManager;
    private static TpaManager tpaManager;
    private static WarpManager warpManager;
    private static ChannelManager channelManager;
    private static MinecraftServer currentServer;

    @Override
    public void onInitialize() {
        LOGGER.info("[QoLMod] Initializing...");

        // Load config
        Path configDir = FabricLoader.getInstance().getConfigDir();
        config = QoLConfig.init(configDir);

        // Register custom items (villager buckets, etc.)
        QoLItems.init();

        // Register villager-bucket dispenser behaviors
        registerVillagerBucketDispenserBehaviors();

        // Initialize managers
        treeChopperManager = new TreeChopperManager();
        backManager = new BackManager();
        tpaManager = new TpaManager();
        warpManager = new WarpManager();
        channelManager = new ChannelManager();

        // Register commands on server start
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            if (config.commandTpaEnabled) TpaCommand.register(dispatcher);
            if (config.commandWarpEnabled) WarpCommand.register(dispatcher);
            if (config.commandBanEnabled) BanCommand.register(dispatcher);
            if (config.commandKickEnabled) KickCommand.register(dispatcher);
            if (config.commandChannelEnabled) ChannelCommand.register(dispatcher);
            if (config.commandHomeEnabled) HomeCommand.register(dispatcher);
            if (config.commandBackEnabled) BackCommand.register(dispatcher);
            if (config.commandRecipeEnabled) RecipeCommand.register(dispatcher);
            if (config.treeChopperEnabled) TreeChopperCommand.register(dispatcher);

            // Always register alias command
            if (config.commandAliasesEnabled) AliasCommand.register(dispatcher);
        });

        // Server lifecycle
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            currentServer = server;
            homeManager = new HomeManager(server);
            warpManager.load(server);
            LOGGER.info("[QoLMod] Server started, managers initialized.");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (homeManager != null) homeManager.saveAll();
            if (warpManager != null) warpManager.save(server);
            tpaManager.cleanup();
            currentServer = null;
        });

        // Tick events for TPA timeout cleanup
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tpaManager.tick();
        });

        // Player join/quit events for back manager
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server1) -> {
            backManager.clearPlayer(handler.getPlayer().getUuid());
        });

        LOGGER.info("[QoLMod] Initialization complete.");
    }

    public static QoLConfig getConfig() { return config; }
    public static TreeChopperManager getTreeChopperManager() { return treeChopperManager; }
    public static BackManager getBackManager() { return backManager; }
    public static HomeManager getHomeManager() { return homeManager; }
    public static TpaManager getTpaManager() { return tpaManager; }
    public static WarpManager getWarpManager() { return warpManager; }
    public static ChannelManager getChannelManager() { return channelManager; }
    public static MinecraftServer getServer() { return currentServer; }

    /** Registers dispenser behavior that releases a bucketed villager when dispensed. */
    private static void registerVillagerBucketDispenserBehaviors() {
        ItemDispenserBehavior releaseBehavior = new ItemDispenserBehavior() {
            @Override
            protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
                var item = stack.getItem();
                if (item instanceof net.minecraft.item.FluidModificationItem fluidItem) {
                    var pos = pointer.pos().offset(pointer.state()
                            .get(net.minecraft.block.DispenserBlock.FACING));
                    if (fluidItem.placeFluid(null, pointer.world(), pos, null)) {
                        fluidItem.onEmptied(null, pointer.world(), stack, pos);
                        return new ItemStack(Items.BUCKET);
                    }
                }
                return super.dispenseSilently(pointer, stack);
            }
        };

        DispenserBlock.registerBehavior(QoLItems.VILLAGER_IN_A_BUCKET, releaseBehavior);
        DispenserBlock.registerBehavior(QoLItems.WANDERING_TRADER_IN_A_BUCKET, releaseBehavior);
        DispenserBlock.registerBehavior(QoLItems.ZOMBIE_VILLAGER_IN_A_BUCKET, releaseBehavior);
    }
}
