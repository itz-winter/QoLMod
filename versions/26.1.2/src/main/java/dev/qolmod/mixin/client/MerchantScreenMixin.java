package dev.qolmod.mixin.client;

import dev.qolmod.client.QoLModClient;
import dev.qolmod.client.features.traderefresh.TradeRefreshHandler;
import dev.qolmod.config.QoLConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.trading.MerchantScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a "Refresh Trades" button to the merchant screen (singleplayer only).
 */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addDrawableChild(T drawableElement);

    @Inject(method = "init", at = @At("TAIL"))
    private void qolmod$onInit(CallbackInfo ci) {
        TradeRefreshHandler handler = QoLModClient.getTradeRefreshHandler();
        if (handler == null) return;
        if (!QoLConfig.getInstance().getBool("tradeRefresh.enabled")) return;

        Minecraft client = Minecraft.getInstance();
        if (client.getServer() == null) return; // Singleplayer/LAN only

        int btnX = this.leftPos + 176;
        int btnY = this.topPos + 8;

        addDrawableChild(Button.builder(
                Component.literal("Refresh Trades"),
                btn -> handler.refreshTrades(client)
        ).dimensions(btnX, btnY, 96, 20).build());
    }

    @Inject(method = "onClose", at = @At("HEAD"), require = 0)
    private void qolmod$onClose(CallbackInfo ci) {
        TradeRefreshHandler handler = QoLModClient.getTradeRefreshHandler();
        if (handler != null) handler.stopAutoRefresh();
    }
}
