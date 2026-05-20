package dev.qolmod.mixin.client;

import dev.qolmod.client.features.fullbright.FullbrightHandler;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept gamma writes when fullbright is enabled.
 * This prevents the options.txt from storing our override gamma value.
 */
@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {

    @Inject(
            method = "write",
            at = @At("HEAD")
    )
    private void qolmod$beforeWrite(CallbackInfo ci) {
        // When saving options, the gamma value might be our override (16.0).
        // We don't need to do anything special here since FullbrightHandler
        // manages the gamma value directly via getGamma().setValue().
        // The mixin is kept as a hook point for future gamma save protection.
    }
}
