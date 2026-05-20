package dev.qolmod.mixin;

import dev.qolmod.QoLMod;
import dev.qolmod.config.QoLConfig;
import dev.qolmod.gamerule.QoLGameRules;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Restores spawn/respawn immunity that was removed in Minecraft 1.21.4.
 *
 * <p>When a player spawns or respawns, they receive
 * {@code qolmod:spawn_immunity_ticks} ticks of full damage immunity
 * (default 60 = 3 seconds). Any damage is silently cancelled during
 * this window. The counter ticks down each server tick.
 *
 * <p>Setting the gamerule to 0 or disabling the config flag fully
 * deactivates the feature.
 */
@Mixin(ServerPlayerEntity.class)
public class SpawnImmunityMixin {

    /** Remaining immunity ticks for this player. 0 = no immunity. */
    @Unique
    private int qolmod$immunityTicks = 0;

    /**
     * Called when the player spawns (initial join) or respawns (after death).
     * Reads the per-world gamerule and sets the immunity timer.
     */
    @Inject(method = "onSpawn", at = @At("TAIL"))
    private void qolmod$onSpawn(CallbackInfo ci) {
        QoLConfig config = QoLMod.getConfig();
        if (config == null || !config.spawnImmunityEnabled) {
            qolmod$immunityTicks = 0;
            return;
        }

        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        ServerWorld world = self.getEntityWorld();
        qolmod$immunityTicks = world.getGameRules().getValue(QoLGameRules.SPAWN_IMMUNITY_TICKS);
    }

    /**
     * Cancels all incoming damage while spawn immunity is active.
     * This runs before vanilla's invulnerability/absorption checks.
     */
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void qolmod$damage(ServerWorld world, DamageSource source, float amount,
                               CallbackInfoReturnable<Boolean> cir) {
        if (qolmod$immunityTicks > 0) {
            // Cancel the damage — return false = no damage was dealt
            cir.setReturnValue(false);
        }
    }

    /**
     * Decrements the immunity counter every server tick.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void qolmod$tick(CallbackInfo ci) {
        if (qolmod$immunityTicks > 0) {
            qolmod$immunityTicks--;
        }
    }
}
