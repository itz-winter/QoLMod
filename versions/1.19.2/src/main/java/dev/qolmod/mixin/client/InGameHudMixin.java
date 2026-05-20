package dev.qolmod.mixin.client;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to inject custom HUD rendering (hunger display, feature notifications).
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void qolmod$onHudRender(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        // Delegate to our client handlers for HUD rendering
        // The actual rendering is handled by the callbacks registered in QoLModClient
        // This mixin provides an alternative injection point if needed
    }
}
