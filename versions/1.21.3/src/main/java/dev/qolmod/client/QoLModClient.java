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
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class QoLModClient implements ClientModInitializer {

    public static KeyBinding fullbrightKey;
    public static KeyBinding treeChopperKey;
    public static KeyBinding tradeRefreshKey;
    public static KeyBinding invMoveKey;

    private FullbrightHandler fullbrightHandler;
    private TreeChopperClientHandler treeChopperClientHandler;
    private TradeRefreshHandler tradeRefreshHandler;
    private InvMoveHandler invMoveHandler;
    private AccurateBlockPlacementHandler placementHandler;

    private static QoLModClient INSTANCE;

    public QoLModClient() { INSTANCE = this; }
    public static QoLModClient getInstance() { return INSTANCE; }

    @Override
    public void onInitializeClient() {
        QoLConfig config = QoLMod.getConfig();

        if (config.fullbrightEnabled) {
            fullbrightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.qolmod.fullbright", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.qolmod"));
            fullbrightHandler = new FullbrightHandler(config.fullbrightDefault);
        }
        if (config.treeChopperEnabled) {
            treeChopperKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.qolmod.treechopper", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.qolmod"));
            treeChopperClientHandler = new TreeChopperClientHandler(config.treeChopperDefault);
        }
        if (config.tradeRefreshEnabled) {
            tradeRefreshKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.qolmod.traderefresh", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F, "category.qolmod"));
            tradeRefreshHandler = new TradeRefreshHandler();
        }
        if (config.invMoveEnabled) {
            invMoveKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.qolmod.invmove", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.qolmod"));
            invMoveHandler = new InvMoveHandler(config.invMoveDefault);
        }
        placementHandler = new AccurateBlockPlacementHandler();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (fullbrightHandler != null && fullbrightKey.wasPressed()) fullbrightHandler.toggle();
            if (treeChopperClientHandler != null && treeChopperKey.wasPressed()) treeChopperClientHandler.toggle();
            if (tradeRefreshHandler != null && tradeRefreshKey.wasPressed()) tradeRefreshHandler.onKeyPress(client);
            if (invMoveHandler != null && invMoveKey.wasPressed()) invMoveHandler.toggle();
        });

        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            if (fullbrightHandler != null) fullbrightHandler.renderHud(drawContext);
            if (treeChopperClientHandler != null) treeChopperClientHandler.renderHud(drawContext);
            if (config.hungerDisplayEnabled) HungerDisplayRenderer.render(drawContext, renderTickCounter);
        });
    }

    public static FullbrightHandler getFullbrightHandler() {
        return INSTANCE != null ? INSTANCE.fullbrightHandler : null;
    }
    public static InvMoveHandler getInvMoveHandler() {
        return INSTANCE != null ? INSTANCE.invMoveHandler : null;
    }
    public static TradeRefreshHandler getTradeRefreshHandler() {
        return INSTANCE != null ? INSTANCE.tradeRefreshHandler : null;
    }
}
