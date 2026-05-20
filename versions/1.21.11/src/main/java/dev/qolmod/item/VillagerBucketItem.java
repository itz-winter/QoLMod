package dev.qolmod.item;

import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.function.Consumer;

/**
 * Item class for villager-type bucket items (villager, wandering trader, zombie villager).
 *
 * Unlike fish bucket items, these do NOT contain water — the villager is held dry.
 * Releasing places the villager entity at the target position with full data restored
 * (profession, level, trades, etc.).
 */
public class VillagerBucketItem extends EntityBucketItem {

    private final SoundEvent releaseSound;

    public VillagerBucketItem(net.minecraft.entity.EntityType<? extends MobEntity> type,
                              SoundEvent releaseSound,
                              Item.Settings settings) {
        super(type, Fluids.EMPTY, releaseSound, settings);
        this.releaseSound = releaseSound;
    }

    /**
     * Instead of placing water, just verify the target position is valid and play
     * the release sound. Returns true to signal that the "placement" succeeded so
     * {@link #onEmptied} (inherited from EntityBucketItem) runs next.
     */
    @Override
    public boolean placeFluid(LivingEntity user, World world, BlockPos pos, BlockHitResult hit) {
        BlockState state = world.getBlockState(pos);
        // Reject if the block cannot be replaced (solid blocks, occupied fluid, etc.)
        if (!state.isAir() && !state.isReplaceable()) {
            return false;
        }
        if (!world.isClient()) {
            world.playSound(null, pos, releaseSound, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        }
        return true;
    }

    /**
     * Sound is already played in {@link #placeFluid}; suppress the default call.
     */
    @Override
    protected void playEmptyingSound(LivingEntity user, WorldAccess world, BlockPos pos) {
        // intentionally empty — sound handled in placeFluid
    }

    @Override
    public void appendTooltip(ItemStack stack,
                              Item.TooltipContext context,
                              TooltipDisplayComponent display,
                              Consumer<Text> tooltip,
                              TooltipType type) {
        NbtComponent data = stack.getOrDefault(DataComponentTypes.BUCKET_ENTITY_DATA, NbtComponent.DEFAULT);
        if (data.isEmpty()) {
            data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
            if (data.isEmpty()) return;
        }

        NbtCompound nbt = data.copyNbt();

        // VillagerData is stored under "VillagerData" key by VillagerEntity.writeCustomData
        nbt.getCompound("VillagerData").ifPresent(vd -> {
            int level = vd.getInt("level", 1);
            tooltip.accept(Text.literal("Level: " + level).formatted(Formatting.ITALIC, Formatting.GRAY));

            vd.getString("profession").ifPresent(prof -> {
                String name = prof.contains(":") ? prof.split(":")[1] : prof;
                // Capitalise first letter for display
                if (!name.isEmpty()) {
                    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                }
                tooltip.accept(Text.literal("Profession: " + name).formatted(Formatting.ITALIC, Formatting.GRAY));
            });

            vd.getString("type").ifPresent(vtype -> {
                String name = vtype.contains(":") ? vtype.split(":")[1] : vtype;
                if (!name.isEmpty()) {
                    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                }
                tooltip.accept(Text.literal("Type: " + name).formatted(Formatting.ITALIC, Formatting.GRAY));
            });
        });

        // Baby status stored as Age < 0
        nbt.getInt("Age").ifPresent(age -> {
            if (age < 0) {
                tooltip.accept(Text.literal("Baby").formatted(Formatting.ITALIC, Formatting.GRAY));
            }
        });
    }
}
