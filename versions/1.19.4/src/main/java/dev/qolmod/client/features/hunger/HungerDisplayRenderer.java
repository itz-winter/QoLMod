package dev.qolmod.client.features.hunger;

import dev.qolmod.QoLMod;
import dev.qolmod.config.QoLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.FoodComponent;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;

/**
 * AppleSkin-like hunger/saturation display overlay.
 * Renders saturation and exhaustion info on the HUD.
 * Based on AppleSkin by squeek502.
 * Adapted for 1.19.4 (MatrixStack, pre-component food API).
 */
public class HungerDisplayRenderer {

    public static void render(MatrixStack matrices, float tickDelta) {
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
        float exhaustion = hunger.getExhaustion();

        if (config.hungerDisplayShowSaturation && saturation > 0) {
            renderSaturationOverlay(matrices, screenWidth, screenHeight, foodLevel, saturation, config);
        }

        if (config.hungerDisplayShowExhaustion && exhaustion > 0) {
            renderExhaustionOverlay(matrices, screenWidth, screenHeight, exhaustion, config);
        }

        if (config.hungerDisplayShowFoodValues) {
            renderFoodValues(matrices, player, screenWidth, screenHeight, config);
        }
    }

    private static void renderSaturationOverlay(MatrixStack matrices, int screenWidth, int screenHeight,
                                                  int foodLevel, float saturation, QoLConfig config) {
        int right = screenWidth / 2 + 91;
        int top = screenHeight - 39;
        int color = parseColor(config.hungerDisplaySaturationColor);

        int saturationInt = (int) Math.ceil(saturation);
        for (int i = 0; i < Math.min(saturationInt, 20); i += 2) {
            int idx = i / 2;
            int x = right - idx * 8 - 9;
            int pipsLeft = saturationInt - i;

            if (pipsLeft >= 2) {
                DrawableHelper.fill(matrices, x + 3, top + 3, x + 7, top + 7, (0x80 << 24) | (color & 0xFFFFFF));
            } else if (pipsLeft == 1) {
                DrawableHelper.fill(matrices, x + 3, top + 3, x + 5, top + 7, (0x80 << 24) | (color & 0xFFFFFF));
            }
        }
    }

    private static void renderExhaustionOverlay(MatrixStack matrices, int screenWidth, int screenHeight,
                                                  float exhaustion, QoLConfig config) {
        int right = screenWidth / 2 + 91;
        int top = screenHeight - 39;
        int color = parseColor(config.hungerDisplayExhaustionColor);

        float ratio = Math.min(exhaustion / 4.0f, 1.0f);
        int barWidth = (int) (80 * ratio);
        if (barWidth > 0) {
            DrawableHelper.fill(matrices, right - 80, top + 9, right - 80 + barWidth, top + 10, (0x60 << 24) | (color & 0xFFFFFF));
        }
    }

    private static void renderFoodValues(MatrixStack matrices, PlayerEntity player,
                                           int screenWidth, int screenHeight, QoLConfig config) {
        if (player.getMainHandStack().isEmpty()) return;

        var stack = player.getMainHandStack();
        FoodComponent foodComponent = stack.getItem().getFoodComponent();
        if (foodComponent == null) return;

        int nutrition = foodComponent.getHunger();
        float satMod = foodComponent.getSaturationModifier();

        if (nutrition <= 0) return;

        int right = screenWidth / 2 + 91;
        int top = screenHeight - 39;
        int color = parseColor(config.hungerDisplayFoodValueColor);

        int currentFood = player.getHungerManager().getFoodLevel();
        int newFood = Math.min(currentFood + nutrition, 20);

        for (int i = currentFood; i < newFood; i += 2) {
            int idx = i / 2;
            int x = right - idx * 8 - 9;
            int borderColor = (0xA0 << 24) | (color & 0xFFFFFF);
            // Draw border manually (top, bottom, left, right lines)
            DrawableHelper.fill(matrices, x + 2, top + 2, x + 8, top + 3, borderColor);
            DrawableHelper.fill(matrices, x + 2, top + 7, x + 8, top + 8, borderColor);
            DrawableHelper.fill(matrices, x + 2, top + 3, x + 3, top + 7, borderColor);
            DrawableHelper.fill(matrices, x + 7, top + 3, x + 8, top + 7, borderColor);
        }
    }

    private static int parseColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFFAA00;
        }
    }
}
