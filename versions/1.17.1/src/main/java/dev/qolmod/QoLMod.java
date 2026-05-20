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
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
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

        // Initialize managers
        treeChopperManager = new TreeChopperManager();
        backManager = new BackManager();
        tpaManager = new TpaManager();
        warpManager = new WarpManager();
        channelManager = new ChannelManager();

        // Register commands on server start
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
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
}
