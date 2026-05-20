package dev.qolmod.client.features.treechopper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

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

    public void renderHud(DrawContext context) {
        long elapsed = System.currentTimeMillis() - toggleTimestamp;
        if (elapsed < 2000) {
            MinecraftClient client = MinecraftClient.getInstance();
            String text = enabled ? "§aTree Chopper: ON" : "§cTree Chopper: OFF";
            int width = client.textRenderer.getWidth(text);
            int screenWidth = client.getWindow().getScaledWidth();
            int x = (screenWidth - width) / 2;
            int y = 16;
            context.drawTextWithShadow(client.textRenderer, Text.literal(text), x, y, 0xFFFFFF);
        }
    }
}
