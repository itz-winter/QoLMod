package dev.qolmod.client.features.fullbright;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Fullbright toggle handler.
 * Overrides gamma to max when enabled.
 * Based on Full-Brightness-Toggle by Serilum.
 */
public class FullbrightHandler {

    private boolean enabled;
    private double previousGamma = 1.0;
    private long toggleTimestamp = 0;

    public FullbrightHandler(boolean defaultState) {
        this.enabled = defaultState;
        if (enabled) {
            applyFullbright();
        }
    }

    public void toggle() {
        enabled = !enabled;
        toggleTimestamp = System.currentTimeMillis();

        if (enabled) {
            applyFullbright();
        } else {
            restoreGamma();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void applyFullbright() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            previousGamma = client.options.getGamma().getValue();
            client.options.getGamma().setValue(16.0);
        }
    }

    private void restoreGamma() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options != null) {
            client.options.getGamma().setValue(previousGamma);
        }
    }

    public void renderHud(MatrixStack matrices) {
        long elapsed = System.currentTimeMillis() - toggleTimestamp;
        if (elapsed < 2000) { // Show for 2 seconds after toggle
            MinecraftClient client = MinecraftClient.getInstance();
            String text = enabled ? "§aFullbright: ON" : "§cFullbright: OFF";
            int width = client.textRenderer.getWidth(text);
            int screenWidth = client.getWindow().getScaledWidth();
            int x = (screenWidth - width) / 2;
            int y = 4;
            client.textRenderer.drawWithShadow(matrices, text, x, y, 0xFFFFFF);
        }
    }
}
