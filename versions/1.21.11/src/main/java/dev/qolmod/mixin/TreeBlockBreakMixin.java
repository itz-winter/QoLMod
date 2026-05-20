package dev.qolmod.mixin;

import dev.qolmod.QoLMod;
import dev.qolmod.features.treechopper.TreeChopperManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin that intercepts block breaking to trigger tree chopper logic.
 * In 1.21.11, Block.onBreak returns BlockState instead of void, so we
 * must use CallbackInfoReturnable<BlockState>.
 */
@Mixin(net.minecraft.block.Block.class)
public class TreeBlockBreakMixin {

    @Inject(
            method = "onBreak",
            at = @At("HEAD"),
            cancellable = true
    )
    private void qolmod$onBlockBreak(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfoReturnable<BlockState> ci) {
        if (world.isClient()) return;
        if (!(player instanceof ServerPlayerEntity)) return;

        TreeChopperManager manager = QoLMod.getTreeChopperManager();
        if (manager != null && manager.onBlockBreak(world, player, pos, state)) {
            // Tree was harvested — the manager already removed the blocks
            // We don't cancel here since the original block break should still proceed
            // for the first log the player actually hit
        }
    }
}
