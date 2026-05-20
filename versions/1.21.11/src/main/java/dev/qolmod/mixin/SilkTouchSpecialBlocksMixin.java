package dev.qolmod.mixin;

import dev.qolmod.QoLMod;
import dev.qolmod.config.QoLConfig;
import dev.qolmod.gamerule.QoLGameRules;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Implements Silk Touch pickup for blocks that are normally unobtainable:
 * Budding Amethyst, Reinforced Deepslate, Suspicious Sand, Suspicious Gravel.
 *
 * Each block has a corresponding gamerule and config switch. All default to
 * enabled except Reinforced Deepslate (default off, as it has little survival
 * purpose).
 *
 * Suspicious Sand/Gravel drop as empty blocks — any loot that was inside is
 * lost (just like how you can't silk-touch a chest with items in it).
 */
@Mixin(Block.class)
public class SilkTouchSpecialBlocksMixin {

    @Inject(
            method = "afterBreak",
            at = @At("HEAD")
    )
    private void qolmod$silkTouchSpecialBlocks(
            World world,
            PlayerEntity player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            ItemStack tool,
            CallbackInfo ci
    ) {
        if (world.isClient()) return;

        QoLConfig config = QoLMod.getConfig();
        if (config == null) return;

        ServerWorld serverWorld = (ServerWorld) world;
        GameRules gamerules = serverWorld.getGameRules();

        // Resolve the Silk Touch enchantment entry once
        var enchantRegistry = serverWorld.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var silkTouchEntry = enchantRegistry.getOrThrow(Enchantments.SILK_TOUCH);
        if (EnchantmentHelper.getLevel(silkTouchEntry, tool) < 1) return;

        if (state.isOf(Blocks.BUDDING_AMETHYST)) {
            if (config.silkTouchBuddingAmethystEnabled
                    && gamerules.getValue(QoLGameRules.SILK_TOUCH_BUDDING_AMETHYST)) {
                Block.dropStack(world, pos, new ItemStack(Items.BUDDING_AMETHYST));
            }
        } else if (state.isOf(Blocks.REINFORCED_DEEPSLATE)) {
            if (config.silkTouchReinforcedDeepslateEnabled
                    && gamerules.getValue(QoLGameRules.SILK_TOUCH_REINFORCED_DEEPSLATE)) {
                Block.dropStack(world, pos, new ItemStack(Items.REINFORCED_DEEPSLATE));
            }
        } else if (state.isOf(Blocks.SUSPICIOUS_SAND)) {
            if (config.silkTouchSuspiciousSandEnabled
                    && gamerules.getValue(QoLGameRules.SILK_TOUCH_SUSPICIOUS_SAND)) {
                Block.dropStack(world, pos, new ItemStack(Items.SUSPICIOUS_SAND));
            }
        } else if (state.isOf(Blocks.SUSPICIOUS_GRAVEL)) {
            if (config.silkTouchSuspiciousGravelEnabled
                    && gamerules.getValue(QoLGameRules.SILK_TOUCH_SUSPICIOUS_GRAVEL)) {
                Block.dropStack(world, pos, new ItemStack(Items.SUSPICIOUS_GRAVEL));
            }
        }
    }
}
