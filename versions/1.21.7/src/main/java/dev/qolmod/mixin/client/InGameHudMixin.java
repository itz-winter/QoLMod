package dev.qolmod.mixin.client;

import dev.qolmod.client.QoLModClient;
import dev.qolmod.client.features.fullbright.FullbrightHandler;
import dev.qolmod.client.features.hunger.HungerDisplayRenderer;
import dev.qolmod.client.features.invmove.InvMoveHandler;
import dev.qolmod.client.features.treechopper.TreeChopperClientHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
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
    private void qolmod$onHudRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // Delegate to our client handlers for HUD rendering
        // The actual rendering is handled by the callbacks registered in QoLModClient
        // This mixin provides an alternative injection point if needed
    }
}
