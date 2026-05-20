package dev.qolmod.client.features.traderefresh;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

/**
 * Client-side villager trade refresh handler.
 * Only works in singleplayer / LAN where we can access the integrated server.
 */
public class TradeRefreshHandler {

    private boolean autoRefreshActive = false;
    private int targetTradeIndex = -1;

    public void onKeyPress(MinecraftClient client) {
        if (client.currentScreen instanceof MerchantScreen) {
            refreshTrades(client);
        }
    }

    /**
     * Resets all trade offer uses for the currently open merchant.
     * Requires singleplayer or LAN (integrated server must be running).
     */
    public void refreshTrades(MinecraftClient client) {
        if (!(client.currentScreen instanceof MerchantScreen)) return;
        MinecraftServer server = client.getServer();
        if (server == null) return; // Not singleplayer/LAN

        server.execute(() -> {
            if (client.player == null) return;
            ServerPlayerEntity serverPlayer = server.getPlayerManager()
                    .getPlayer(client.player.getUuid());
            if (serverPlayer == null) return;

            if (!(serverPlayer.currentScreenHandler instanceof MerchantScreenHandler merchantHandler)) return;

            TradeOfferList offers = merchantHandler.getRecipes();
            for (TradeOffer offer : offers) {
                offer.resetUses();
            }

            // Push updated offer list to client via network packet
            serverPlayer.networkHandler.sendPacket(new SetTradeOffersS2CPacket(
                    merchantHandler.syncId,
                    offers,
                    0,      // levelProgress
                    0,      // experience
                    false,  // leveled merchant
                    false   // can refresh
            ));
        });
    }

    public boolean isAutoRefreshActive() {
        return autoRefreshActive;
    }

    public void setAutoRefresh(boolean active, int tradeIndex) {
        this.autoRefreshActive = active;
        this.targetTradeIndex = tradeIndex;
    }

    public int getTargetTradeIndex() {
        return targetTradeIndex;
    }

    public void stopAutoRefresh() {
        autoRefreshActive = false;
        targetTradeIndex = -1;
    }
}
