package dev.qolmod.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for accurate block placement — adjusts where blocks are placed
 * based on the actual crosshair position rather than face center.
 */
@Mixin(LocalPlayer.class)
public class ClientPlayerMixin {

    @Inject(
            method = "tick",
            at = @At("TAIL")
    )
    private void qolmod$onTick(CallbackInfo ci) {
        // Placeholder for accurate block placement logic
        // AccurateBlockPlacement hooks happen in the placement interaction, not tick
    }
}
