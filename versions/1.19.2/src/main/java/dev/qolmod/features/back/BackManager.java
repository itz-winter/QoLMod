package dev.qolmod.features.back;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.*;

/**
 * Manages /back (previous location) and /dback (death location) data.
 * Works both client-side (singleplayer) and server-side.
 */
public class BackManager {

    private final Map<UUID, LocationData> previousLocations = new HashMap<>();
    private final Map<UUID, LocationData> deathLocations = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public void recordTeleport(ServerPlayerEntity player) {
        previousLocations.put(player.getUuid(), new LocationData(
                player.getWorld().getRegistryKey(),
                player.getBlockPos(),
                player.getYaw(),
                player.getPitch()
        ));
    }

    public void recordDeath(ServerPlayerEntity player) {
        deathLocations.put(player.getUuid(), new LocationData(
                player.getWorld().getRegistryKey(),
                player.getBlockPos(),
                player.getYaw(),
                player.getPitch()
        ));
    }

    public LocationData getPreviousLocation(UUID playerId) {
        return previousLocations.get(playerId);
    }

    public LocationData getDeathLocation(UUID playerId) {
        return deathLocations.get(playerId);
    }

    public boolean isOnCooldown(UUID playerId, int cooldownSeconds) {
        Long lastUse = cooldowns.get(playerId);
        if (lastUse == null) return false;
        return (System.currentTimeMillis() - lastUse) < (cooldownSeconds * 1000L);
    }

    public void setCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    public void clearPlayer(UUID playerId) {
        previousLocations.remove(playerId);
        deathLocations.remove(playerId);
        cooldowns.remove(playerId);
    }

    public static class LocationData {
        public final RegistryKey<World> dimension;
        public final BlockPos pos;
        public final float yaw;
        public final float pitch;

        public LocationData(RegistryKey<World> dimension, BlockPos pos, float yaw, float pitch) {
            this.dimension = dimension;
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
