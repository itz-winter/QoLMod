package dev.qolmod.mixin;

import dev.qolmod.QoLMod;
import dev.qolmod.features.treechopper.TreeChopperManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that intercepts block breaking to trigger tree chopper logic.
 */
@Mixin(net.minecraft.world.level.block.Block.class)
public class TreeBlockBreakMixin {

    @Inject(
            method = "playerWillDestroy",
            at = @At("HEAD"),
            cancellable = true
    )
    private void qolmod$onBlockBreak(World world, BlockPos pos, BlockState state, Player player, CallbackInfo ci) {
        if (world.isClientSide()) return;
        if (!(player instanceof ServerPlayer)) return;

        TreeChopperManager manager = QoLMod.getTreeChopperManager();
        if (manager != null && manager.onBlockBreak(world, player, pos, state)) {
            // Tree was harvested — the manager already removed the blocks
            // We don't cancel here since the original block break should still proceed
            // for the first log the player actually hit
        }
    }
}
