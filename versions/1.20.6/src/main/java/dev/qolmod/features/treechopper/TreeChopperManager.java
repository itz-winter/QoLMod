package dev.qolmod.features.treechopper;

import dev.qolmod.QoLMod;
import dev.qolmod.config.QoLConfig;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/**
 * Server-side tree chopper logic.
 * Based on Tree-Harvester by Serilum.
 * Uses LeavesBlock.PERSISTENT to detect player-made trees.
 */
public class TreeChopperManager {

    private final Set<UUID> enabledPlayers = new HashSet<>();

    public boolean isEnabled(UUID playerId) {
        return enabledPlayers.contains(playerId);
    }

    public void setEnabled(UUID playerId, boolean enabled) {
        if (enabled) {
            enabledPlayers.add(playerId);
        } else {
            enabledPlayers.remove(playerId);
        }
    }

    public void toggle(UUID playerId) {
        if (enabledPlayers.contains(playerId)) {
            enabledPlayers.remove(playerId);
        } else {
            enabledPlayers.add(playerId);
        }
    }

    /**
     * Called when a block is broken. If it's a log and tree chopper is enabled,
     * harvest the entire tree.
     *
     * @return true if the event should be cancelled (tree was harvested)
     */
    public boolean onBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state) {
        if (world.isClient()) return false;
        if (!isEnabled(player.getUuid())) return false;

        QoLConfig config = QoLMod.getConfig();

        if (!isLog(state.getBlock())) return false;

        // Check if player must hold axe
        if (config.treeChopperMustHoldAxe) {
            ItemStack hand = player.getMainHandStack();
            if (!(hand.getItem() instanceof AxeItem)) return false;
        }

        // Sneak behavior
        if (config.treeChopperSneakToChop) {
            if (!player.isSneaking()) return false;
        }

        // Find the bottom of the tree
        BlockPos bottomPos = findBottomLog(world, pos, state.getBlock());

        // Validate it's a tree (has leaves above)
        TreeValidation validation = validateTree(world, bottomPos, state.getBlock(), config);
        if (!validation.isValid) return false;

        // Harvest all logs
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        harvestTree(world, serverPlayer, validation.logs, config);

        // Fast leaf decay
        if (config.treeChopperFastLeafDecay) {
            scheduleLeafDecay((ServerWorld) world, validation.logs, bottomPos);
        }

        return true;
    }

    private BlockPos findBottomLog(World world, BlockPos pos, Block logType) {
        BlockPos current = pos;
        while (isMatchingLog(world, current.down(), logType)) {
            current = current.down();
        }
        return current;
    }

    private TreeValidation validateTree(World world, BlockPos bottomPos, Block logType, QoLConfig config) {
        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        collectLogs(world, bottomPos, logType, logs, visited, 0);

        if (logs.isEmpty()) return new TreeValidation(false, logs);

        // Check for leaves above the logs
        int leafCount = 0;
        BlockPos highest = bottomPos;
        for (BlockPos log : logs) {
            if (log.getY() > highest.getY()) highest = log;
        }

        // Scan around and above for leaves
        for (int y = 0; y <= 8; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = highest.add(x, y, z);
                    BlockState checkState = world.getBlockState(checkPos);
                    Block block = checkState.getBlock();

                    if (isLeaf(block, config)) {
                        // Check for player-made leaves (persistent = true)
                        if (config.treeChopperIgnorePlayerMade) {
                            boolean persistent = checkState.contains(Properties.PERSISTENT)
                                    && checkState.get(Properties.PERSISTENT);
                            if (persistent) {
                                return new TreeValidation(false, logs);
                            }
                        }
                        leafCount++;
                    }
                }
            }
        }

        return new TreeValidation(leafCount >= 3, logs);
    }

    private void collectLogs(World world, BlockPos pos, Block logType,
                              List<BlockPos> logs, Set<BlockPos> visited, int depth) {
        if (depth > 256 || visited.contains(pos) || logs.size() > 256) return;
        visited.add(pos);

        if (!isMatchingLog(world, pos, logType)) return;
        logs.add(pos);

        // Check all 26 neighbors (3x3x3 cube minus center)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    collectLogs(world, pos.add(dx, dy, dz), logType, logs, visited, depth + 1);
                }
            }
        }
    }

    private void harvestTree(World world, ServerPlayerEntity player, List<BlockPos> logs, QoLConfig config) {
        ItemStack axe = player.getMainHandStack();
        int durabilityLoss = 0;
        double durabilityMod = config.treeChopperDurabilityModifier;
        int lossPer = Math.max(1, (int) Math.ceil(1.0 / durabilityMod));
        int counter = 0;
        ServerWorld serverWorld = (ServerWorld) world;

        for (BlockPos logPos : logs) {
            BlockState state = world.getBlockState(logPos);
            if (!isLog(state.getBlock())) continue;

            // Drop the block naturally
            Block.dropStacks(state, world, logPos, world.getBlockEntity(logPos), player, axe);
            world.setBlockState(logPos, Blocks.AIR.getDefaultState());

            // Durability loss
            if (!player.isCreative() && axe.getItem() instanceof AxeItem) {
                counter++;
                if (counter >= lossPer) {
                    axe.setDamage(axe.getDamage() + 1);
                    if (axe.getDamage() >= axe.getMaxDamage()) {
                        axe.decrement(1);
                        player.sendEquipmentBreakStatus(net.minecraft.entity.EquipmentSlot.MAINHAND);
                        break; // Axe broke
                    }
                    counter = 0;
                }
            }

            // Exhaustion
            player.addExhaustion(0.025f);
        }
    }

    private void scheduleLeafDecay(ServerWorld world, List<BlockPos> logs, BlockPos bottomPos) {
        // Find the bounding box of the tree and schedule leaf ticks
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (BlockPos log : logs) {
            minX = Math.min(minX, log.getX());
            minZ = Math.min(minZ, log.getZ());
            maxX = Math.max(maxX, log.getX());
            maxZ = Math.max(maxZ, log.getZ());
            maxY = Math.max(maxY, log.getY());
        }

        int dist = 5;
        for (BlockPos pos : BlockPos.iterate(
                new BlockPos(minX - dist, bottomPos.getY(), minZ - dist),
                new BlockPos(maxX + dist, maxY + 5, maxZ + dist))) {
            BlockState state = world.getBlockState(pos);
            if (isLeaf(state.getBlock(), QoLMod.getConfig())) {
                world.scheduleBlockTick(pos, state.getBlock(), world.random.nextInt(20) + 1);
            }
        }
    }

    // === Block type checks ===

    public static boolean isLog(Block block) {
        // Check for vanilla logs, modded logs via tag, mushroom stems
        String name = block.getTranslationKey().toLowerCase();
        return block instanceof PillarBlock && (name.contains("log") || name.contains("stem") || name.contains("wood"))
                && !name.contains("stripped")
                || block instanceof MushroomBlock;
    }

    private boolean isMatchingLog(World world, BlockPos pos, Block logType) {
        Block block = world.getBlockState(pos).getBlock();
        if (block == logType) return true;
        // Check if same wood type (e.g. oak_log matches oak_log)
        return isLog(block) && areSameWoodType(logType, block);
    }

    private boolean areSameWoodType(Block a, Block b) {
        String nameA = a.getTranslationKey().split("\\.")[2]; // e.g. "oak_log" -> first word
        String nameB = b.getTranslationKey().split("\\.")[2];
        // Extract wood type prefix
        String typeA = nameA.replace("_log", "").replace("_stem", "").replace("_wood", "");
        String typeB = nameB.replace("_log", "").replace("_stem", "").replace("_wood", "");
        return typeA.equals(typeB);
    }

    private boolean isLeaf(Block block, QoLConfig config) {
        if (block instanceof LeavesBlock) return true;
        if (config.treeChopperNetherTrees) {
            if (block == Blocks.NETHER_WART_BLOCK || block == Blocks.WARPED_WART_BLOCK || block == Blocks.SHROOMLIGHT)
                return true;
        }
        if (config.treeChopperHugeMushrooms && block instanceof MushroomBlock) return true;
        return false;
    }

    private static class TreeValidation {
        final boolean isValid;
        final List<BlockPos> logs;

        TreeValidation(boolean isValid, List<BlockPos> logs) {
            this.isValid = isValid;
            this.logs = logs;
        }
    }
}
