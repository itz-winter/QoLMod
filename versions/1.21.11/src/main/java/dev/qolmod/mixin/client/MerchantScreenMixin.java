package dev.qolmod.mixin.client;

import dev.qolmod.client.QoLModClient;
import dev.qolmod.client.features.traderefresh.TradeRefreshHandler;
import dev.qolmod.config.QoLConfig;
import dev.qolmod.mixin.client.accessor.ScreenInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a "Refresh Trades" button to the merchant screen (singleplayer only).
 *
 * In 1.21.11 the x/y fields (field_2776/field_2777) were removed from
 * MerchantScreen's class hierarchy. Position is now computed from the window
 * width instead of shadowing those fields.
 *
 * Uses ScreenInvoker to call the protected addDrawableChild without a shadow.
 */
@Mixin(MerchantScreen.class)
public class MerchantScreenMixin {

    private static final int MERCHANT_BG_WIDTH = 276; // MerchantScreen background width in pixels

    @Inject(method = "init", at = @At("TAIL"))
    private void qolmod$onInit(CallbackInfo ci) {
        TradeRefreshHandler handler = QoLModClient.getTradeRefreshHandler();
        if (handler == null) return;
        if (!QoLConfig.getInstance().getBool("tradeRefresh.enabled")) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() == null) return; // Singleplayer/LAN only

        // Compute the GUI top-left corner the same way HandledScreen does
        int scaledWidth = client.getWindow().getScaledWidth();
        int scaledHeight = client.getWindow().getScaledHeight();
        int guiX = (scaledWidth - MERCHANT_BG_WIDTH) / 2;
        int guiY = (scaledHeight - 166) / 2;

        int btnX = guiX + MERCHANT_BG_WIDTH;
        int btnY = guiY + 8;

        ((ScreenInvoker) (Object) this).invokeAddDrawableChild(ButtonWidget.builder(
                Text.literal("Refresh Trades"),
                btn -> handler.refreshTrades(client)
        ).dimensions(btnX, btnY, 96, 20).build());
    }

    @Inject(method = "close", at = @At("HEAD"), require = 0)
    private void qolmod$onClose(CallbackInfo ci) {
        TradeRefreshHandler handler = QoLModClient.getTradeRefreshHandler();
        if (handler != null) handler.stopAutoRefresh();
    }
}



