package dev.qolmod.features.warps;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Manages server warps. Ops only can create/delete.
 * Anyone can use /warp to teleport.
 */
public class WarpManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, WarpData> warps = new LinkedHashMap<>();
    private Path warpsFile;

    public void load(MinecraftServer server) {
        warpsFile = server.getSavePath(WorldSavePath.ROOT).resolve("qolmod_warps.json");
        warps.clear();

        if (Files.exists(warpsFile)) {
            try (Reader reader = Files.newBufferedReader(warpsFile, StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    JsonObject wObj = entry.getValue().getAsJsonObject();
                    String dimId = wObj.get("dimension").getAsString();
                    int x = wObj.get("x").getAsInt();
                    int y = wObj.get("y").getAsInt();
                    int z = wObj.get("z").getAsInt();
                    float yaw = wObj.has("yaw") ? wObj.get("yaw").getAsFloat() : 0;
                    float pitch = wObj.has("pitch") ? wObj.get("pitch").getAsFloat() : 0;

                    RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, parseIdentifier(dimId));
                    warps.put(entry.getKey(), new WarpData(entry.getKey(), dimension, new BlockPos(x, y, z), yaw, pitch));
                }
            } catch (Exception e) {
                System.err.println("[QoLMod] Failed to load warps: " + e.getMessage());
            }
        }
    }

    public void save(MinecraftServer server) {
        if (warpsFile == null) return;
        try {
            Files.createDirectories(warpsFile.getParent());
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, WarpData> entry : warps.entrySet()) {
                WarpData warp = entry.getValue();
                JsonObject wObj = new JsonObject();
                wObj.addProperty("dimension", warp.dimension.getValue().toString());
                wObj.addProperty("x", warp.pos.getX());
                wObj.addProperty("y", warp.pos.getY());
                wObj.addProperty("z", warp.pos.getZ());
                wObj.addProperty("yaw", warp.yaw);
                wObj.addProperty("pitch", warp.pitch);
                obj.add(entry.getKey(), wObj);
            }
            try (Writer writer = Files.newBufferedWriter(warpsFile, StandardCharsets.UTF_8)) {
                GSON.toJson(obj, writer);
            }
        } catch (IOException e) {
            System.err.println("[QoLMod] Failed to save warps: " + e.getMessage());
        }
    }

    public void setWarp(String name, RegistryKey<World> dimension, BlockPos pos, float yaw, float pitch) {
        warps.put(name.toLowerCase(Locale.ROOT), new WarpData(name, dimension, pos, yaw, pitch));
    }

    public boolean deleteWarp(String name) {
        return warps.remove(name.toLowerCase(Locale.ROOT)) != null;
    }

    public WarpData getWarp(String name) {
        return warps.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<WarpData> getAll() {
        return Collections.unmodifiableCollection(warps.values());
    }

    public Set<String> getNames() {
        return Collections.unmodifiableSet(warps.keySet());
    }

    public static class WarpData {
        public final String name;
        public final RegistryKey<World> dimension;
        public final BlockPos pos;
        public final float yaw;
        public final float pitch;

        public WarpData(String name, RegistryKey<World> dimension, BlockPos pos, float yaw, float pitch) {
            this.name = name;
            this.dimension = dimension;
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static Identifier parseIdentifier(String id) {
        if (id.contains(":")) {
            String[] parts = id.split(":", 2);
            return Identifier.of(parts[0], parts[1]);
        }
        return Identifier.of("minecraft", id);
    }
}
