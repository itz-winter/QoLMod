package dev.qolmod.teleport;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Manages pre-teleport countdown sequences.
 * If countdownSeconds == 0, the teleport fires immediately.
 * If the player takes damage during the countdown, the teleport is cancelled.
 * Action-bar messages and sounds are sent each second.
 */
public class TeleportManager {

    private static final Map<UUID, PendingTeleport> pending = new HashMap<>();

    /**
     * Requests a teleport with an optional countdown.
     * @param player         the player to teleport
     * @param world          destination world
     * @param x, y, z       destination coordinates
     * @param yaw            destination yaw
     * @param pitch          destination pitch
     * @param countdownSeconds seconds to wait (0 = immediate)
     * @param successMessage message sent in chat after teleport (null = none)
     */
    public static void requestTeleport(ServerPlayerEntity player, ServerWorld world,
                                       double x, double y, double z, float yaw, float pitch,
                                       int countdownSeconds, String successMessage) {
        // Cancel any in-flight TP for this player
        cancelTeleport(player.getUuid());

        if (countdownSeconds <= 0) {
            executeTeleport(player, world, x, y, z, yaw, pitch);
            if (successMessage != null) player.sendMessage(Text.literal(successMessage));
            return;
        }

        pending.put(player.getUuid(),
                new PendingTeleport(world, x, y, z, yaw, pitch, countdownSeconds, successMessage, player.getHealth()));

        player.sendMessage(Text.literal("§eTeleporting in §f" + countdownSeconds + "§e second(s)... §7(Don't take damage!)"), true);
        playSound(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.7f);
    }

    /** Cancels any pending teleport for the given player. */
    public static void cancelTeleport(UUID playerId) {
        pending.remove(playerId);
    }

    /** Must be called every server tick from ServerTickEvents. */
    public static void tick(MinecraftServer server) {
        if (pending.isEmpty()) return;

        Iterator<Map.Entry<UUID, PendingTeleport>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingTeleport> entry = it.next();
            UUID id = entry.getKey();
            PendingTeleport tp = entry.getValue();

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            if (player == null || player.isRemoved() || !player.isAlive()) {
                it.remove();
                continue;
            }

            // Damage check — cancel if health dropped significantly
            if (player.getHealth() < tp.healthAtStart - 0.5f) {
                player.sendMessage(Text.literal("§cTeleport cancelled! (took damage)"), true);
                playSound(player, SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                it.remove();
                continue;
            }

            long elapsed = System.currentTimeMillis() - tp.startTimeMs;
            int secondsElapsed = (int) (elapsed / 1000L);
            int secondsRemaining = tp.countdownSeconds - secondsElapsed;

            // Send action-bar update each new second
            if (secondsElapsed != tp.lastAnnouncedSecond && secondsRemaining > 0) {
                tp.lastAnnouncedSecond = secondsElapsed;
                player.sendMessage(Text.literal("§eTeleporting in §f" + secondsRemaining + "§e..."), true);
                    float pitch = 0.7f + (secondsElapsed * 0.1f);
                    playSound(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0f, Math.min(pitch, 2.0f));
            }

            // Execute when countdown complete
            if (elapsed >= tp.countdownSeconds * 1000L) {
                player.sendMessage(Text.literal("§a✔ Teleporting!"), true);
                playSound(player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                executeTeleport(player, tp.world, tp.x, tp.y, tp.z, tp.yaw, tp.pitch);
                if (tp.successMessage != null) player.sendMessage(Text.literal(tp.successMessage));
                it.remove();
            }
        }
    }

    /** True if the player has a pending teleport. */
    public static boolean hasPending(UUID playerId) {
        return pending.containsKey(playerId);
    }

    private static void executeTeleport(ServerPlayerEntity player, ServerWorld world,
                                        double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, Set.of(), yaw, pitch, true);
    }

    /** Play a sound at the player's position using the server world (SoundEvent overload). */
    private static void playSound(ServerPlayerEntity player, SoundEvent sound, float volume, float pitch) {
        BlockPos pos = player.getBlockPos();
        ((ServerWorld) player.getEntityWorld()).playSound(null, pos, sound, SoundCategory.PLAYERS, volume, pitch);
    }

    /** Play a sound at the player's position using the server world (RegistryEntry overload). */
    private static void playSound(ServerPlayerEntity player, RegistryEntry<SoundEvent> sound, float volume, float pitch) {
        BlockPos pos = player.getBlockPos();
        ((ServerWorld) player.getEntityWorld()).playSound(null, pos, sound.value(), SoundCategory.PLAYERS, volume, pitch);
    }

    // ===== Data =====

    private static class PendingTeleport {
        final ServerWorld world;
        final double x, y, z;
        final float yaw, pitch;
        final int countdownSeconds;
        final String successMessage;
        final float healthAtStart;
        final long startTimeMs = System.currentTimeMillis();
        int lastAnnouncedSecond = 0;

        PendingTeleport(ServerWorld world, double x, double y, double z, float yaw, float pitch,
                        int countdownSeconds, String successMessage, float healthAtStart) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.countdownSeconds = countdownSeconds;
            this.successMessage = successMessage;
            this.healthAtStart = healthAtStart;
        }
    }
}
