package dev.qolmod.config;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Manages per-user command aliases.
 * Aliases are stored per user profile in the config directory.
 * Supports create, edit, delete, JSON export/import.
 */
public class CommandAliasManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path aliasDir;
    private final Map<String, String> aliases = new LinkedHashMap<>();

    public CommandAliasManager(Path configDir) {
        this.aliasDir = configDir.resolve("qolmod_aliases");
    }

    /**
     * Load aliases for a specific user profile.
     */
    public void load(String profileName) {
        aliases.clear();
        Path file = aliasDir.resolve(profileName + ".json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        aliases.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            } catch (Exception e) {
                System.err.println("[QoLMod] Failed to load aliases for profile '" + profileName + "': " + e.getMessage());
            }
        }
    }

    /**
     * Save aliases for a specific user profile.
     */
    public void save(String profileName) {
        Path file = aliasDir.resolve(profileName + ".json");
        try {
            Files.createDirectories(aliasDir);
            JsonObject obj = new JsonObject();
            for (Map.Entry<String, String> entry : aliases.entrySet()) {
                obj.addProperty(entry.getKey(), entry.getValue());
            }
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(obj, writer);
            }
        } catch (IOException e) {
            System.err.println("[QoLMod] Failed to save aliases for profile '" + profileName + "': " + e.getMessage());
        }
    }

    public void addAlias(String alias, String command) {
        aliases.put(alias.toLowerCase(Locale.ROOT), command);
    }

    public void removeAlias(String alias) {
        aliases.remove(alias.toLowerCase(Locale.ROOT));
    }

    public String resolve(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        // Check if the first word matches an alias
        String[] parts = lower.split("\\s+", 2);
        String replacement = aliases.get(parts[0]);
        if (replacement != null) {
            return parts.length > 1 ? replacement + " " + parts[1] : replacement;
        }
        return null; // No alias match
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(aliases);
    }

    /**
     * Export aliases to a JSON file at the given path.
     */
    public void exportTo(Path exportPath) throws IOException {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            obj.addProperty(entry.getKey(), entry.getValue());
        }
        try (Writer writer = Files.newBufferedWriter(exportPath, StandardCharsets.UTF_8)) {
            GSON.toJson(obj, writer);
        }
    }

    /**
     * Import aliases from a JSON file, merging with existing.
     */
    public void importFrom(Path importPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(importPath, StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    aliases.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().getAsString());
                }
            }
        }
    }
}
