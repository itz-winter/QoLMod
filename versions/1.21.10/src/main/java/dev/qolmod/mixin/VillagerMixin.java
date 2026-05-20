package dev.qolmod.mixin;

import dev.qolmod.QoLMod;
import dev.qolmod.config.QoLConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.qolmod.QoLItems.VILLAGER_IN_A_BUCKET;

/**
 * Makes {@link VillagerEntity} implement {@link Bucketable}, allowing players to capture
 * villagers in an empty bucket, preserving their full data (profession, trades, gossip, etc.).
 *
 * Controlled by {@link QoLConfig#villagerBucketEnabled}.
 */
@Mixin(VillagerEntity.class)
public abstract class VillagerMixin extends MerchantEntity implements Bucketable {

    /** Stored as a plain instance field — no DataTracker needed (villagers don't auto-despawn). */
    @Unique
    private boolean qolmod$fromBucket = false;

    protected VillagerMixin(EntityType<? extends MerchantEntity> type, World world) {
        super(type, world);
    }

    // ===== Interaction: right-click with empty bucket picks up the villager =====

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void qolmod$interactMob(PlayerEntity player, Hand hand,
                                    CallbackInfoReturnable<ActionResult> cir) {
        QoLConfig cfg = QoLMod.getConfig();
        if (cfg == null || !cfg.villagerBucketEnabled) return;

        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Items.BUCKET) || !isAlive()) return;

        // Replicate Bucketable.tryBucket() but check BUCKET instead of WATER_BUCKET
        // (tryBucket hardcodes a WATER_BUCKET check internally, so we can't use it)
        playSound(getBucketFillSound(), 1.0F, 1.0F);
        ItemStack bucketStack = getBucketItem();
        copyDataToStack(bucketStack);
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
            net.minecraft.advancement.criterion.Criteria.FILLED_BUCKET.trigger(sp, bucketStack);
        }
        setFromBucket(true);
        discard();
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        if (stack.isEmpty()) {
            player.setStackInHand(hand, bucketStack);
        } else {
            player.giveItemStack(bucketStack);
        }
        cir.setReturnValue(ActionResult.SUCCESS);
    }

    // ===== Persist fromBucket flag across world saves =====

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void qolmod$writeCustomData(WriteView output, CallbackInfo ci) {
        output.putBoolean("QoLFromBucket", qolmod$fromBucket);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void qolmod$readCustomData(ReadView input, CallbackInfo ci) {
        qolmod$fromBucket = input.getBoolean("QoLFromBucket", false);
    }

    // ===== Bucketable implementation =====

    @Override
    public boolean isFromBucket() {
        return qolmod$fromBucket;
    }

    @Override
    public void setFromBucket(boolean fromBucket) {
        qolmod$fromBucket = fromBucket;
    }

    /**
     * Saves the villager's full state (profession, level, trades, gossip, etc.) into
     * the bucket's {@code BUCKET_ENTITY_DATA} component by delegating to
     * {@link VillagerEntity#writeCustomData(WriteView)}.
     */
    @Override
    public void copyDataToStack(ItemStack stack) {
        Bucketable.copyDataToStack((MobEntity) (Object) this, stack);
        NbtComponent.set(DataComponentTypes.BUCKET_ENTITY_DATA, stack, nbt -> {
            NbtWriteView view = NbtWriteView.create(ErrorReporter.EMPTY);
            writeCustomData(view);
            nbt.copyFrom(view.getNbt());
        });
    }

    @Override
    public void copyDataFromNbt(NbtCompound nbt) {
        Bucketable.copyDataFromNbt((MobEntity) (Object) this, nbt);
        ReadView readView = NbtReadView.create(
                ErrorReporter.EMPTY, getEntityWorld().getRegistryManager(), nbt);
        readCustomData(readView);
    }

    @Override
    public ItemStack getBucketItem() {
        return new ItemStack(VILLAGER_IN_A_BUCKET);
    }

    @Override
    public SoundEvent getBucketFillSound() {
        return SoundEvents.ENTITY_VILLAGER_TRADE;
    }

    @Unique
    @SuppressWarnings("unchecked")
    private <T extends net.minecraft.entity.LivingEntity & Bucketable> T asBucketable() {
        return (T)(Object) this;
    }
}
