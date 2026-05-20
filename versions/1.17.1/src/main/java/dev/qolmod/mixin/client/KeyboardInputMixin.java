package dev.qolmod.mixin.client;

import dev.qolmod.client.QoLModClient;
import dev.qolmod.client.features.invmove.InvMoveHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects movement key processing while inventory screens are open (InvMove feature).
 */
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Shadow public float movementForward;
    @Shadow public float movementSideways;
    @Shadow public boolean jumping;
    @Shadow public boolean sneaking;

    @Inject(method = "tick", at = @At("TAIL"))
    private void qolmod$onTick(boolean slowDown, float f, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        InvMoveHandler handler = QoLModClient.getInvMoveHandler();

        if (handler == null || client.currentScreen == null) return;
        if (!handler.shouldAllowMovement(client.currentScreen)) return;

        GameOptions opts = client.options;

        boolean forward = opts.keyForward.isPressed();
        boolean back    = opts.keyBack.isPressed();
        boolean left    = opts.keyLeft.isPressed();
        boolean right   = opts.keyRight.isPressed();

        this.movementForward  = (forward ? 1.0f : 0.0f) - (back  ? 1.0f : 0.0f);
        this.movementSideways = (left   ? 1.0f : 0.0f) - (right ? 1.0f : 0.0f);

        if (slowDown) {
            this.movementForward  *= 0.3f;
            this.movementSideways *= 0.3f;
        }

        this.jumping = opts.keyJump.isPressed();
        this.sneaking = opts.keySneak.isPressed();
    }
}
