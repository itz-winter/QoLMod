package dev.qolmod.mixin.client;

import dev.qolmod.client.features.fullbright.FullbrightHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for fullbright — intercepts the gamma getter to return
 * our overridden value when fullbright is active.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(
            method = "getLimitFramerate",
            at = @At("HEAD")
    )
    private void qolmod$onGetFramerateLimit(CallbackInfoReturnable<Integer> cir) {
        // This is a non-intrusive hook into the game loop
        // The actual fullbright override is applied in FullbrightHandler.onTick()
        // via direct OptionInstance value manipulation
    }
}
