package dev.qolmod.client.features.treechopper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Client-side tree chopper toggle handler.
 * The actual chopping logic is server-side (TreeChopperManager);
 * this manages the toggle state and HUD notification.
 */
public class TreeChopperClientHandler {

    private boolean enabled;
    private long toggleTimestamp = 0;

    public TreeChopperClientHandler(boolean defaultState) {
        this.enabled = defaultState;
    }

    public void toggle() {
        enabled = !enabled;
        toggleTimestamp = System.currentTimeMillis();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        toggleTimestamp = System.currentTimeMillis();
    }

    public void renderHud(GuiGraphics context) {
        long elapsed = System.currentTimeMillis() - toggleTimestamp;
        if (elapsed < 2000) {
            Minecraft client = Minecraft.getInstance();
            String text = enabled ? "§aTree Chopper: ON" : "§cTree Chopper: OFF";
            int width = client.textRenderer.getWidth(text);
            int screenWidth = client.getWindow().getScaledWidth();
            int x = (screenWidth - width) / 2;
            int y = 16;
            context.drawTextWithShadow(client.textRenderer, Component.literal(text), x, y, 0xFFFFFF);
        }
    }
}
