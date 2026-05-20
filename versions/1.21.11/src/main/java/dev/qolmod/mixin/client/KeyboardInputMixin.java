package dev.qolmod.mixin.client;

import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * InvMove placeholder for MC 1.21.11.
 *
 * In 1.21.11, the movement fields (movementForward, movementSideways, jumping,
 * sneaking) were removed entirely from KeyboardInput as part of an input system
 * rearchitecture. Shadowing them crashes the game at mod-load time because the
 * remapper cannot resolve them.
 *
 * This stub keeps the class in the mixin list so it can be properly
 * reimplemented once the new input API is confirmed.
 */
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void qolmod$onTick(CallbackInfo ci) {
        // InvMove disabled for 1.21.11 pending input-API reimplementation.
    }
}
