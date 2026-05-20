package dev.qolmod.features.homes;

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
 * Manages player homes. Homes persist across restarts.
 * Supports icon (item ID), description, creation timestamp, and renaming.
 */
public class HomeManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path homesDir;
    private final Map<UUID, Map<String, HomeData>> playerHomes = new HashMap<>();

    public HomeManager(MinecraftServer server) {
        this.homesDir = server.getSavePath(WorldSavePath.ROOT).resolve("qolmod_homes");
        loadAll();
    }

    public void setHome(UUID playerId, String name, RegistryKey<World> dimension, BlockPos pos, float yaw, float pitch) {
        Map<String, HomeData> homes = playerHomes.computeIfAbsent(playerId, k -> new LinkedHashMap<>());
        String key = name.toLowerCase(Locale.ROOT);
        HomeData existing = homes.get(key);
        String icon = existing != null ? existing.icon : "minecraft:oak_sign";
        String desc = existing != null ? existing.description : "";
        long createdAt = existing != null ? existing.createdAt : System.currentTimeMillis();
        homes.put(key, new HomeData(name, dimension, pos, yaw, pitch, icon, desc, createdAt));
        savePlayer(playerId);
    }

    public HomeData getHome(UUID playerId, String name) {
        Map<String, HomeData> homes = playerHomes.get(playerId);
        if (homes == null) return null;
        return homes.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean deleteHome(UUID playerId, String name) {
        Map<String, HomeData> homes = playerHomes.get(playerId);
        if (homes == null) return false;
        boolean removed = homes.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) savePlayer(playerId);
        return removed;
    }

    /**
     * Renames a home. Returns true on success, false if home not found or name conflict.
     */
    public boolean renameHome(UUID playerId, String oldName, String newName) {
        Map<String, HomeData> homes = playerHomes.get(playerId);
        if (homes == null) return false;
        String oldKey = oldName.toLowerCase(Locale.ROOT);
        String newKey = newName.toLowerCase(Locale.ROOT);
        HomeData existing = homes.get(oldKey);
        if (existing == null) return false;
        if (homes.containsKey(newKey) && !oldKey.equals(newKey)) return false;
        homes.remove(oldKey);
        homes.put(newKey, new HomeData(newName, existing.dimension, existing.pos,
                existing.yaw, existing.pitch, existing.icon, existing.description, existing.createdAt));
        savePlayer(playerId);
        return true;
    }

    /** Updates the icon (item ID string) for a home. */
    public boolean setHomeIcon(UUID playerId, String name, String icon) {
        Map<String, HomeData> homes = playerHomes.get(playerId);
        if (homes == null) return false;
        String key = name.toLowerCase(Locale.ROOT);
        HomeData h = homes.get(key);
        if (h == null) return false;
        homes.put(key, new HomeData(h.name, h.dimension, h.pos, h.yaw, h.pitch, icon, h.description, h.createdAt));
        savePlayer(playerId);
        return true;
    }

    /** Updates the description for a home. */
    public boolean setHomeDescription(UUID playerId, String name, String description) {
        Map<String, HomeData> homes = playerHomes.get(playerId);
        if (homes == null) return false;
        String key = name.toLowerCase(Locale.ROOT);
        HomeData h = homes.get(key);
        if (h == null) return false;
        homes.put(key, new HomeData(h.name, h.dimension, h.pos, h.yaw, h.pitch, h.icon, description, h.createdAt));
        savePlayer(playerId);
        return true;
    }

    public Map<String, HomeData> getHomes(UUID playerId) {
        return playerHomes.getOrDefault(playerId, Collections.emptyMap());
    }

    public int getHomeCount(UUID playerId) {
        return getHomes(playerId).size();
    }

    private void loadAll() {
        if (!Files.exists(homesDir)) return;
        try (var stream = Files.list(homesDir)) {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(this::loadPlayerFile);
        } catch (IOException e) {
            System.err.println("[QoLMod] Failed to load homes: " + e.getMessage());
        }
    }

    private void loadPlayerFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String filename = file.getFileName().toString().replace(".json", "");
            UUID playerId = UUID.fromString(filename);

            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, HomeData> homes = new LinkedHashMap<>();

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                JsonObject homeObj = entry.getValue().getAsJsonObject();
                String name = homeObj.get("name").getAsString();
                String dimId = homeObj.get("dimension").getAsString();
                int x = homeObj.get("x").getAsInt();
                int y = homeObj.get("y").getAsInt();
                int z = homeObj.get("z").getAsInt();
                float yaw = homeObj.get("yaw").getAsFloat();
                float pitch = homeObj.get("pitch").getAsFloat();
                String icon = homeObj.has("icon") ? homeObj.get("icon").getAsString() : "minecraft:oak_sign";
                String description = homeObj.has("description") ? homeObj.get("description").getAsString() : "";
                long createdAt = homeObj.has("createdAt") ? homeObj.get("createdAt").getAsLong() : System.currentTimeMillis();

                RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dimId));
                homes.put(entry.getKey(), new HomeData(name, dimension, new BlockPos(x, y, z), yaw, pitch, icon, description, createdAt));
            }

            playerHomes.put(playerId, homes);
        } catch (Exception e) {
            System.err.println("[QoLMod] Failed to load home file " + file + ": " + e.getMessage());
        }
    }

    private void savePlayer(UUID playerId) {
        Map<String, HomeData> homes = playerHomes.get(playerId);
        if (homes == null) return;

        try {
            Files.createDirectories(homesDir);
            Path file = homesDir.resolve(playerId.toString() + ".json");

            JsonObject obj = new JsonObject();
            for (Map.Entry<String, HomeData> entry : homes.entrySet()) {
                HomeData home = entry.getValue();
                JsonObject homeObj = new JsonObject();
                homeObj.addProperty("name", home.name);
                homeObj.addProperty("dimension", home.dimension.getValue().toString());
                homeObj.addProperty("x", home.pos.getX());
                homeObj.addProperty("y", home.pos.getY());
                homeObj.addProperty("z", home.pos.getZ());
                homeObj.addProperty("yaw", home.yaw);
                homeObj.addProperty("pitch", home.pitch);
                homeObj.addProperty("icon", home.icon);
                homeObj.addProperty("description", home.description);
                homeObj.addProperty("createdAt", home.createdAt);
                obj.add(entry.getKey(), homeObj);
            }

            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(obj, writer);
            }
        } catch (IOException e) {
            System.err.println("[QoLMod] Failed to save homes for " + playerId + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        for (UUID playerId : playerHomes.keySet()) {
            savePlayer(playerId);
        }
    }

    public static class HomeData {
        public final String name;
        public final RegistryKey<World> dimension;
        public final BlockPos pos;
        public final float yaw;
        public final float pitch;
        /** Item ID used as the GUI icon, e.g. "minecraft:oak_sign". */
        public final String icon;
        /** Optional short description shown in GUI lore. */
        public final String description;
        /** Unix timestamp (ms) when this home was created. */
        public final long createdAt;

        public HomeData(String name, RegistryKey<World> dimension, BlockPos pos, float yaw, float pitch,
                        String icon, String description, long createdAt) {
            this.name = name;
            this.dimension = dimension;
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
            this.icon = icon;
            this.description = description;
            this.createdAt = createdAt;
        }
    }
}

