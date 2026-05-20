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
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

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
        LOGGER.info("[QoLMod] Initializing for MC 1.21.3...");

        Path configDir = FabricLoader.getInstance().getConfigDir();
        config = QoLConfig.init(configDir);

        treeChopperManager = new TreeChopperManager();
        backManager = new BackManager();
        tpaManager = new TpaManager();
        warpManager = new WarpManager();
        channelManager = new ChannelManager();

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
            if (config.commandAliasesEnabled) AliasCommand.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            currentServer = server;
            homeManager = new HomeManager(server);
            warpManager.load(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (homeManager != null) homeManager.saveAll();
            if (warpManager != null) warpManager.save(server);
            tpaManager.cleanup();
            currentServer = null;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> tpaManager.tick());

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server1) -> {
            backManager.clearPlayer(handler.getPlayer().getUuid());
        });
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
