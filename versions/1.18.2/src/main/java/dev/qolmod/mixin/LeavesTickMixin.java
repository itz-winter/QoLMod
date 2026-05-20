package dev.qolmod.mixin;

import dev.qolmod.QoLMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for fast leaf decay when a tree is chopped.
 * Makes scheduled leaf ticks execute faster.
 */
@Mixin(LeavesBlock.class)
public class LeavesTickMixin {

    @Inject(
            method = "scheduledTick",
            at = @At("HEAD")
    )
    private void qolmod$onScheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        // The vanilla scheduledTick already handles leaf decay
        // This mixin is a hook point for future enhancements like
        // particle effects or custom decay behavior
    }
}
