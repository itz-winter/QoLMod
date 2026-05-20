package dev.qolmod.mixin;

import dev.qolmod.QoLMod;
import dev.qolmod.config.QoLConfig;
import dev.qolmod.gamerule.QoLGameRules;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.TypedEntityData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts block breaking for spawners to implement Silk Touch spawner pickup.
 *
 * When the {@code qolmod:silk_touch_spawners} gamerule is enabled and the breaking
 * tool has Silk Touch, the spawner drops as an item with its NBT preserved (so it
 * retains the mob type when placed again).
 *
 * This injects into {@link Block#afterBreak} and only activates when {@code this}
 * is a {@link SpawnerBlock}. The vanilla spawner loot table drops nothing, so we
 * simply add our custom drop without cancelling the call.
 */
@Mixin(Block.class)
public class SpawnerSilkTouchMixin {

    @Inject(
            method = "afterBreak",
            at = @At("HEAD")
    )
    private void qolmod$spawnerSilkTouch(
            World world,
            PlayerEntity player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            ItemStack tool,
            CallbackInfo ci
    ) {
        // Only on server, only for spawner blocks
        if (world.isClient()) return;
        if (!((Object) this instanceof SpawnerBlock)) return;
        if (blockEntity == null) return;

        // Check master config switch
        QoLConfig config = QoLMod.getConfig();
        if (config == null || !config.silkTouchSpawnersEnabled) return;

        // Check gamerule (world-specific, persistent) — requires ServerWorld
        ServerWorld serverWorld = (ServerWorld) world;
        if (!serverWorld.getGameRules().getValue(QoLGameRules.SILK_TOUCH_SPAWNERS)) return;

        // Check if tool has Silk Touch (level >= 1)
        var enchantRegistry = serverWorld.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var silkTouchEntry = enchantRegistry.getOrThrow(Enchantments.SILK_TOUCH);
        if (EnchantmentHelper.getLevel(silkTouchEntry, tool) < 1) return;

        // Build an ItemStack for the spawner, preserving the block entity's NBT
        ItemStack spawnerStack = new ItemStack(Items.SPAWNER);

        // createNbt includes the "id" field — TypedEntityData.create internally strips it
        NbtCompound nbt = blockEntity.createNbt(serverWorld.getRegistryManager());

        TypedEntityData<BlockEntityType<?>> blockEntityData =
                TypedEntityData.create(BlockEntityType.MOB_SPAWNER, nbt);
        spawnerStack.set(DataComponentTypes.BLOCK_ENTITY_DATA, blockEntityData);

        // Drop the spawner at the broken position
        Block.dropStack(world, pos, spawnerStack);
    }
}
