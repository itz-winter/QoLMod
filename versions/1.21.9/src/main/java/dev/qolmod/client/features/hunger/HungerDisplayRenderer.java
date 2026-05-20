package dev.qolmod.client.features.hunger;

import dev.qolmod.QoLMod;
import dev.qolmod.config.QoLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.render.RenderTickCounter;

/**
 * AppleSkin-like hunger/saturation display overlay.
 * Renders saturation and exhaustion info on the HUD.
 * Based on AppleSkin by squeek502.
 */
public class HungerDisplayRenderer {

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        QoLConfig config = QoLMod.getConfig();
        if (!config.hungerDisplayEnabled) return;

        PlayerEntity player = client.player;
        HungerManager hunger = player.getHungerManager();

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int foodLevel = hunger.getFoodLevel();
        float saturation = hunger.getSaturationLevel();

        // Render saturation overlay on food bar
        if (config.hungerDisplayShowSaturation && saturation > 0) {
            renderSaturationOverlay(context, screenWidth, screenHeight, foodLevel, saturation, config);
        }

        // Exhaustion display not available in 1.21.3+ (getExhaustion removed)

        // Render food values when holding food
        if (config.hungerDisplayShowFoodValues) {
            renderFoodValues(context, player, screenWidth, screenHeight, config);
        }
    }

    private static void renderSaturationOverlay(DrawContext context, int screenWidth, int screenHeight,
                                                  int foodLevel, float saturation, QoLConfig config) {
        int right = screenWidth / 2 + 91;
        int top = screenHeight - 39;
        int color = parseColor(config.hungerDisplaySaturationColor);

        // Draw saturation pips overlaid on the hunger bar
        int saturationInt = (int) Math.ceil(saturation);
        for (int i = 0; i < Math.min(saturationInt, 20); i += 2) {
            int idx = i / 2;
            int x = right - idx * 8 - 9;
            int pipsLeft = saturationInt - i;

            if (pipsLeft >= 2) {
                // Full saturation pip
                context.fill(x + 3, top + 3, x + 7, top + 7, (0x80 << 24) | (color & 0xFFFFFF));
            } else if (pipsLeft == 1) {
                // Half saturation pip
                context.fill(x + 3, top + 3, x + 5, top + 7, (0x80 << 24) | (color & 0xFFFFFF));
            }
        }
    }

    private static void renderExhaustionOverlay(DrawContext context, int screenWidth, int screenHeight,
                                                  float exhaustion, QoLConfig config) {
        int right = screenWidth / 2 + 91;
        int top = screenHeight - 39;
        int color = parseColor(config.hungerDisplayExhaustionColor);

        // Render exhaustion as a bar overlay
        float ratio = Math.min(exhaustion / 4.0f, 1.0f); // Exhaustion cap is 4.0
        int barWidth = (int) (80 * ratio);
        if (barWidth > 0) {
            context.fill(right - 80, top + 9, right - 80 + barWidth, top + 10, (0x60 << 24) | (color & 0xFFFFFF));
        }
    }

    private static void renderFoodValues(DrawContext context, PlayerEntity player,
                                           int screenWidth, int screenHeight, QoLConfig config) {
        // Show food value preview when player holds food
        if (player.getMainHandStack().isEmpty()) return;

        var stack = player.getMainHandStack();
        FoodComponent foodComponent = stack.get(DataComponentTypes.FOOD);
        if (foodComponent == null) return;

        int nutrition = foodComponent.nutrition();
        float satMod = foodComponent.saturation();

        if (nutrition <= 0) return;

        int right = screenWidth / 2 + 91;
        int top = screenHeight - 39;
        int color = parseColor(config.hungerDisplayFoodValueColor);

        // Render potential food value as outlined pips
        int currentFood = player.getHungerManager().getFoodLevel();
        int newFood = Math.min(currentFood + nutrition, 20);

        for (int i = currentFood; i < newFood; i += 2) {
            int idx = i / 2;
            int x = right - idx * 8 - 9;
            // Draw outline for predicted hunger restoration
            int borderColor = (0xA0 << 24) | (color & 0xFFFFFF);
            context.fill(x + 2, top + 2, x + 8, top + 3, borderColor); // top
            context.fill(x + 2, top + 7, x + 8, top + 8, borderColor); // bottom
            context.fill(x + 2, top + 2, x + 3, top + 8, borderColor); // left
            context.fill(x + 7, top + 2, x + 8, top + 8, borderColor); // right
        }
    }

    private static int parseColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFFAA00; // Default golden
        }
    }
}
