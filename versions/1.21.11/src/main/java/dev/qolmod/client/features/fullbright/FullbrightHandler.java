package dev.qolmod.client.features.fullbright;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Fullbright toggle handler.
 * Overrides gamma to maximum (15.0) when enabled.
 * Based on Full-Brightness-Toggle by Serilum (maxGamma = 14.0 % 28.0 + 1.0 = 15.0).
 * Initial gamma is captured on the first tick so the user's real gamma is preserved.
 */
public class FullbrightHandler {

    private static final double MAX_GAMMA = 15.0;

    private boolean enabled;
    /** The user's real gamma setting, captured before fullbright is first applied. */
    private double initialGamma = 1.0;
    /** True once we have captured initialGamma from the loaded options. */
    private boolean gammaInitialized = false;
    private long toggleTimestamp = 0;

    public FullbrightHandler(boolean defaultState) {
        this.enabled = defaultState;
        // Do NOT apply gamma here — options are not loaded yet during mod init.
        // tick() handles first-time application.
    }

    /**
     * Must be called every client tick (from ClientTickEvents).
     * Handles deferred first-time application when options are ready.
     */
    public void tick(MinecraftClient client) {
        if (!gammaInitialized && client.options != null) {
            initialGamma = client.options.getGamma().getValue();
            gammaInitialized = true;
            if (enabled) {
                client.options.getGamma().setValue(MAX_GAMMA);
            }
        }
    }

    public void toggle() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options == null) return;

        // Capture initialGamma now if not done yet
        if (!gammaInitialized) {
            initialGamma = client.options.getGamma().getValue();
            gammaInitialized = true;
        }

        enabled = !enabled;
        toggleTimestamp = System.currentTimeMillis();

        if (enabled) {
            // Save the user's current (real) gamma before we override it
            double current = client.options.getGamma().getValue();
            if (current < MAX_GAMMA - 0.1) {
                initialGamma = current;
            }
            client.options.getGamma().setValue(MAX_GAMMA);
        } else {
            client.options.getGamma().setValue(initialGamma);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void renderHud(DrawContext context) {
        long elapsed = System.currentTimeMillis() - toggleTimestamp;
        if (elapsed < 2000) {
            MinecraftClient client = MinecraftClient.getInstance();
            String text = enabled ? "§aFullbright: ON" : "§cFullbright: OFF";
            int width = client.textRenderer.getWidth(text);
            int screenWidth = client.getWindow().getScaledWidth();
            int x = (screenWidth - width) / 2;
            int y = 4;
            context.drawTextWithShadow(client.textRenderer, Text.literal(text), x, y, 0xFFFFFF);
        }
    }
}
