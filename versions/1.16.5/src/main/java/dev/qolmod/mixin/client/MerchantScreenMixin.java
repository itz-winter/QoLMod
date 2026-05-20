package dev.qolmod.mixin.client;

import dev.qolmod.client.QoLModClient;
import dev.qolmod.client.features.traderefresh.TradeRefreshHandler;
import dev.qolmod.config.QoLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
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

    @Shadow protected int x;
    @Shadow protected int y;

    @Shadow protected java.util.List buttons;

    @SuppressWarnings("unchecked")
    private void qolmod$addButton(net.minecraft.client.gui.widget.ButtonWidget btn) {
        buttons.add(btn);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void qolmod$onInit(CallbackInfo ci) {
        TradeRefreshHandler handler = QoLModClient.getTradeRefreshHandler();
        if (handler == null) return;
        if (!QoLConfig.getInstance().getBool("tradeRefresh.enabled")) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() == null) return; // Singleplayer/LAN only

        int btnX = this.x + 176;
        int btnY = this.y + 8;

        qolmod$addButton(new ButtonWidget(btnX, btnY, 96, 20, new LiteralText("Refresh Trades"), btn -> handler.refreshTrades(client)));
    }

    @Inject(method = "onClosed", at = @At("HEAD"), require = 0)
    private void qolmod$onClose(CallbackInfo ci) {
        TradeRefreshHandler handler = QoLModClient.getTradeRefreshHandler();
        if (handler != null) handler.stopAutoRefresh();
    }
}
