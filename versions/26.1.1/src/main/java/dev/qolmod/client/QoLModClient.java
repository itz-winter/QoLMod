package dev.qolmod.client;

import dev.qolmod.QoLMod;
import dev.qolmod.client.features.fullbright.FullbrightHandler;
import dev.qolmod.client.features.hunger.HungerDisplayRenderer;
import dev.qolmod.client.features.invmove.InvMoveHandler;
import dev.qolmod.client.features.placement.AccurateBlockPlacementHandler;
import dev.qolmod.client.features.traderefresh.TradeRefreshHandler;
import dev.qolmod.client.features.treechopper.TreeChopperClientHandler;
import dev.qolmod.config.QoLConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.KeyMapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side entrypoint for QoLMod.
 * Registers keybinds, HUD renderers, and client-only features.
 */
public class QoLModClient implements ClientModInitializer {

    // Keybindings
    public static KeyMapping fullbrightKey;
    public static KeyMapping treeChopperKey;
    public static KeyMapping tradeRefreshKey;
    public static KeyMapping invMoveKey;

    private FullbrightHandler fullbrightHandler;
    private TreeChopperClientHandler treeChopperClientHandler;
    private TradeRefreshHandler tradeRefreshHandler;
    private InvMoveHandler invMoveHandler;
    private AccurateBlockPlacementHandler placementHandler;

    @Override
    public void onInitializeClient() {
        QoLConfig config = QoLMod.getConfig();

        // === Register Keybindings ===

        if (config.fullbrightEnabled) {
            fullbrightKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.qolmod.fullbright",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_G,
                    "category.qolmod"
            ));
            fullbrightHandler = new FullbrightHandler(config.fullbrightDefault);
        }

        if (config.treeChopperEnabled) {
            treeChopperKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.qolmod.treechopper",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "category.qolmod"
            ));
            treeChopperClientHandler = new TreeChopperClientHandler(config.treeChopperDefault);
        }

        if (config.tradeRefreshEnabled) {
            tradeRefreshKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.qolmod.traderefresh",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_F,
                    "category.qolmod"
            ));
            tradeRefreshHandler = new TradeRefreshHandler();
        }

        if (config.invMoveEnabled) {
            invMoveKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.qolmod.invmove",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "category.qolmod"
            ));
            invMoveHandler = new InvMoveHandler(config.invMoveDefault);
        }

        placementHandler = new AccurateBlockPlacementHandler();

        // === Client Tick ===
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (fullbrightHandler != null && fullbrightKey.wasPressed()) {
                fullbrightHandler.toggle();
            }
            if (treeChopperClientHandler != null && treeChopperKey.wasPressed()) {
                treeChopperClientHandler.toggle();
            }
            if (tradeRefreshHandler != null && tradeRefreshKey.wasPressed()) {
                tradeRefreshHandler.onKeyPress(client);
            }
            if (invMoveHandler != null && invMoveKey.wasPressed()) {
                invMoveHandler.toggle();
            }
        });

        // === HUD Rendering ===
        HudRenderCallback.EVENT.register((GuiGraphics, DeltaTracker) -> {
            if (fullbrightHandler != null) {
                fullbrightHandler.renderHud(GuiGraphics);
            }
            if (treeChopperClientHandler != null) {
                treeChopperClientHandler.renderHud(GuiGraphics);
            }
            if (config.hungerDisplayEnabled) {
                HungerDisplayRenderer.render(GuiGraphics, DeltaTracker);
            }
        });
    }

    public static FullbrightHandler getFullbrightHandler() {
        QoLModClient instance = getInstance();
        return instance != null ? instance.fullbrightHandler : null;
    }

    public static InvMoveHandler getInvMoveHandler() {
        QoLModClient instance = getInstance();
        return instance != null ? instance.invMoveHandler : null;
    }

    public static TradeRefreshHandler getTradeRefreshHandler() {
        QoLModClient instance = getInstance();
        return instance != null ? instance.tradeRefreshHandler : null;
    }

    private static QoLModClient INSTANCE;

    public QoLModClient() {
        INSTANCE = this;
    }

    public static QoLModClient getInstance() {
        return INSTANCE;
    }
}
