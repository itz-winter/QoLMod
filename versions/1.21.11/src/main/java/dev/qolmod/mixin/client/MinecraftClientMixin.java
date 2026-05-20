package dev.qolmod.mixin.client;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Placeholder mixin for MinecraftClient in 1.21.11.
 *
 * The getFramerateLimit method was removed in 1.21.11. Fullbright is handled
 * via FullbrightHandler.onTick() through direct SimpleOption value manipulation
 * -- no game-loop intercept is needed here.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    // intentionally empty
}
