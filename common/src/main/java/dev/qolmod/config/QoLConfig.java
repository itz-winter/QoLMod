package dev.qolmod.config;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified config system for QoLMod.
 * All features share a single qolmod.json config file.
 * Changes to priority/override settings require a restart.
 */
public class QoLConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static QoLConfig INSTANCE;
    private final Path configPath;
    private JsonObject root;

    // ===== GENERAL =====
    public boolean overrideOtherMods = true; // Restart required
    public int commandPriority = 1000; // Higher = takes precedence. Restart required.

    // ===== FULLBRIGHT =====
    public boolean fullbrightEnabled = true;
    public boolean fullbrightDefault = false;
    public String fullbrightKeybind = "key.keyboard.g";

    // ===== TREE CHOPPER =====
    public boolean treeChopperEnabled = true;
    public boolean treeChopperDefault = false;
    public String treeChopperKeybind = "key.keyboard.unknown";
    public boolean treeChopperMustHoldAxe = true;
    public boolean treeChopperSneakToChop = false;
    public boolean treeChopperIgnorePlayerMade = true;
    public boolean treeChopperReplantSaplings = true;
    public boolean treeChopperFastLeafDecay = true;
    public boolean treeChopperNetherTrees = true;
    public boolean treeChopperHugeMushrooms = true;
    public double treeChopperDurabilityModifier = 1.0;

    // ===== VILLAGER TRADE REFRESH =====
    public boolean tradeRefreshEnabled = true;
    public String tradeRefreshKeybind = "key.keyboard.f";

    // ===== HUNGER DISPLAY (AppleSkin-like) =====
    public boolean hungerDisplayEnabled = true;
    public boolean hungerDisplayShowSaturation = true;
    public boolean hungerDisplayShowExhaustion = true;
    public boolean hungerDisplayShowFoodValues = true;
    public String hungerDisplaySaturationColor = "#FFAA00";
    public String hungerDisplayExhaustionColor = "#FF0000";
    public String hungerDisplayFoodValueColor = "#00FF00";

    // ===== ACCURATE BLOCK PLACEMENT =====
    public boolean accurateBlockPlacementEnabled = true;
    public boolean accurateBlockPlacementDisableFastBreakOnServers = true;

    // ===== INV MOVE =====
    public boolean invMoveEnabled = false;
    public boolean invMoveDefault = false;
    public String invMoveKeybind = "key.keyboard.unknown";

    // ===== RECIPE VIEWER =====
    public boolean recipeViewerEnabled = true;

    // ===== COMMANDS =====
    public boolean commandTpaEnabled = true;
    public boolean commandWarpEnabled = true;
    public boolean commandBanEnabled = false;
    public boolean commandKickEnabled = false;
    public boolean commandChannelEnabled = true;
    public boolean commandHomeEnabled = true;
    public boolean commandBackEnabled = true;
    public boolean commandDbackEnabled = true;
    public boolean commandRecipeEnabled = true;
    public boolean commandSpawnEnabled = true;
    public boolean commandRtpEnabled = true;
    public boolean commandGamemodeEnabled = true;

    // ===== HOMES =====
    public int homesMaxPerPlayer = 12;

    // ===== TELEPORT =====
    public int tpaCooldownSeconds = 2;
    public int tpaTimeoutSeconds = 120;
    public int tpaInvulnerabilitySeconds = 4;

    // ===== BACK =====
    public int backCooldownSeconds = 3;

    // ===== TELEPORT COUNTDOWN =====
    /** Seconds of countdown before a teleport fires. 0 = instant. */
    public int tpCountdownSeconds = 0;

    // ===== RTP =====
    public int rtpRadius = 2000;
    public int rtpCooldownSeconds = 30;
    public int rtpMinRadius = 100;

    // ===== WARPS =====
    // Warps are ops-only to create, anyone can use

    // ===== LORE COMMAND =====
    /** Globally enable /lore. Default: off. */
    public boolean commandLoreEnabled = false;
    /**
     * If true, /lore is also allowed when the player is in Creative mode,
     * even if commandLoreEnabled = false.
     */
    public boolean loreCommandCreativeByDefault = true;

    // ===== WORKBENCH COMMANDS =====
    /** Master switch for all virtual workbench commands. Default: on. */
    public boolean commandWorkbenchEnabled = true;
    public boolean commandWorkbenchCraft = true;
    public boolean commandWorkbenchEnderChest = true;
    public boolean commandWorkbenchAnvil = true;
    public boolean commandWorkbenchGrindstone = true;
    public boolean commandWorkbenchStonecutter = true;
    public boolean commandWorkbenchSmithing = true;
    public boolean commandWorkbenchCartography = true;
    public boolean commandWorkbenchLoom = true;

    // ===== COMMAND ALIASES =====
    public boolean commandAliasesEnabled = true;

    // ===== CHEAT COMMANDS (require cheats/op) =====
    public boolean commandFlyEnabled = true;
    public boolean commandGodEnabled = true;
    public boolean commandHealEnabled = true;
    public boolean commandFeedEnabled = true;

    // ===== SILK TOUCH SPAWNERS =====
    /** Master switch — if false, the feature is fully disabled regardless of gamerule. */
    public boolean silkTouchSpawnersEnabled = true;

    // ===== SILK TOUCH SPECIAL BLOCKS =====
    public boolean silkTouchBuddingAmethystEnabled = true;
    public boolean silkTouchReinforcedDeepslateEnabled = true;
    public boolean silkTouchSuspiciousSandEnabled = true;
    public boolean silkTouchSuspiciousGravelEnabled = true;

    // ===== VILLAGER IN A BUCKET =====
    /** Master switch — enables bucketing villagers and wandering traders. */
    public boolean villagerBucketEnabled = true;
    /**
     * Whether zombie villagers can also be bucketed.
     * Requires {@link #villagerBucketEnabled} = true.
     */
    public boolean villagerBucketZombieEnabled = false;

    // ===== SPAWN IMMUNITY =====
    /**
     * Master switch for spawn immunity. When false, the feature is completely
     * disabled regardless of the gamerule value.
     */
    public boolean spawnImmunityEnabled = true;

    // ===== ENCHANTMENT CONFLICT OVERRIDE =====
    /**
     * When true, conflicting enchantments can be combined on an anvil, except
     * Fortune + Silk Touch which remains blocked as vanilla intends.
     */
    public boolean enchantConflictOverrideEnabled = true;

    private QoLConfig(Path configDir) {
        this.configPath = configDir.resolve("qolmod.json");
    }

    public static QoLConfig getInstance() {
        return INSTANCE;
    }

    public static QoLConfig init(Path configDir) {
        INSTANCE = new QoLConfig(configDir);
        INSTANCE.load();
        return INSTANCE;
    }

    public void load() {
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                System.err.println("[QoLMod] Failed to load config, using defaults: " + e.getMessage());
                root = new JsonObject();
            }
        } else {
            root = new JsonObject();
        }

        readValues();
        save(); // Write back to fill in any missing defaults
    }

    private void readValues() {
        // General
        overrideOtherMods = getBool("general.overrideOtherMods", overrideOtherMods);
        commandPriority = getInt("general.commandPriority", commandPriority);

        // Fullbright
        fullbrightEnabled = getBool("fullbright.enabled", fullbrightEnabled);
        fullbrightDefault = getBool("fullbright.defaultState", fullbrightDefault);
        fullbrightKeybind = getString("fullbright.keybind", fullbrightKeybind);

        // Tree Chopper
        treeChopperEnabled = getBool("treeChopper.enabled", treeChopperEnabled);
        treeChopperDefault = getBool("treeChopper.defaultState", treeChopperDefault);
        treeChopperKeybind = getString("treeChopper.keybind", treeChopperKeybind);
        treeChopperMustHoldAxe = getBool("treeChopper.mustHoldAxe", treeChopperMustHoldAxe);
        treeChopperSneakToChop = getBool("treeChopper.sneakToChop", treeChopperSneakToChop);
        treeChopperIgnorePlayerMade = getBool("treeChopper.ignorePlayerMade", treeChopperIgnorePlayerMade);
        treeChopperReplantSaplings = getBool("treeChopper.replantSaplings", treeChopperReplantSaplings);
        treeChopperFastLeafDecay = getBool("treeChopper.fastLeafDecay", treeChopperFastLeafDecay);
        treeChopperNetherTrees = getBool("treeChopper.netherTrees", treeChopperNetherTrees);
        treeChopperHugeMushrooms = getBool("treeChopper.hugeMushrooms", treeChopperHugeMushrooms);
        treeChopperDurabilityModifier = getDouble("treeChopper.durabilityModifier", treeChopperDurabilityModifier);

        // Trade Refresh
        tradeRefreshEnabled = getBool("tradeRefresh.enabled", tradeRefreshEnabled);
        tradeRefreshKeybind = getString("tradeRefresh.keybind", tradeRefreshKeybind);

        // Hunger Display
        hungerDisplayEnabled = getBool("hungerDisplay.enabled", hungerDisplayEnabled);
        hungerDisplayShowSaturation = getBool("hungerDisplay.showSaturation", hungerDisplayShowSaturation);
        hungerDisplayShowExhaustion = getBool("hungerDisplay.showExhaustion", hungerDisplayShowExhaustion);
        hungerDisplayShowFoodValues = getBool("hungerDisplay.showFoodValues", hungerDisplayShowFoodValues);
        hungerDisplaySaturationColor = getString("hungerDisplay.saturationColor", hungerDisplaySaturationColor);
        hungerDisplayExhaustionColor = getString("hungerDisplay.exhaustionColor", hungerDisplayExhaustionColor);
        hungerDisplayFoodValueColor = getString("hungerDisplay.foodValueColor", hungerDisplayFoodValueColor);

        // Accurate Block Placement
        accurateBlockPlacementEnabled = getBool("accurateBlockPlacement.enabled", accurateBlockPlacementEnabled);
        accurateBlockPlacementDisableFastBreakOnServers = getBool("accurateBlockPlacement.disableFastBreakOnServers", accurateBlockPlacementDisableFastBreakOnServers);

        // InvMove
        invMoveEnabled = getBool("invMove.enabled", invMoveEnabled);
        invMoveDefault = getBool("invMove.defaultState", invMoveDefault);
        invMoveKeybind = getString("invMove.keybind", invMoveKeybind);

        // Recipe Viewer
        recipeViewerEnabled = getBool("recipeViewer.enabled", recipeViewerEnabled);

        // Commands
        commandTpaEnabled = getBool("commands.tpa", commandTpaEnabled);
        commandWarpEnabled = getBool("commands.warp", commandWarpEnabled);
        commandBanEnabled = getBool("commands.ban", commandBanEnabled);
        commandKickEnabled = getBool("commands.kick", commandKickEnabled);
        commandChannelEnabled = getBool("commands.channel", commandChannelEnabled);
        commandHomeEnabled = getBool("commands.home", commandHomeEnabled);
        commandBackEnabled = getBool("commands.back", commandBackEnabled);
        commandDbackEnabled = getBool("commands.dback", commandDbackEnabled);
        commandRecipeEnabled = getBool("commands.recipe", commandRecipeEnabled);
        commandSpawnEnabled = getBool("commands.spawn", commandSpawnEnabled);
        commandRtpEnabled = getBool("commands.rtp", commandRtpEnabled);
        commandGamemodeEnabled = getBool("commands.gamemode", commandGamemodeEnabled);

        // Homes
        homesMaxPerPlayer = getInt("homes.maxPerPlayer", homesMaxPerPlayer);

        // Teleport
        tpaCooldownSeconds = getInt("teleport.cooldownSeconds", tpaCooldownSeconds);
        tpaTimeoutSeconds = getInt("teleport.timeoutSeconds", tpaTimeoutSeconds);
        tpaInvulnerabilitySeconds = getInt("teleport.invulnerabilitySeconds", tpaInvulnerabilitySeconds);

        // Back
        backCooldownSeconds = getInt("back.cooldownSeconds", backCooldownSeconds);

        // Teleport Countdown
        tpCountdownSeconds = getInt("teleport.countdownSeconds", tpCountdownSeconds);

        // RTP
        rtpRadius = getInt("rtp.radius", rtpRadius);
        rtpCooldownSeconds = getInt("rtp.cooldownSeconds", rtpCooldownSeconds);
        rtpMinRadius = getInt("rtp.minRadius", rtpMinRadius);

        // Command Aliases
        commandAliasesEnabled = getBool("commandAliases.enabled", commandAliasesEnabled);

        // Lore command
        commandLoreEnabled = getBool("commands.lore", commandLoreEnabled);
        loreCommandCreativeByDefault = getBool("lore.creativeByDefault", loreCommandCreativeByDefault);

        // Workbench commands
        commandWorkbenchEnabled = getBool("commands.workbench", commandWorkbenchEnabled);
        commandWorkbenchCraft = getBool("workbench.craft", commandWorkbenchCraft);
        commandWorkbenchEnderChest = getBool("workbench.enderchest", commandWorkbenchEnderChest);
        commandWorkbenchAnvil = getBool("workbench.anvil", commandWorkbenchAnvil);
        commandWorkbenchGrindstone = getBool("workbench.grindstone", commandWorkbenchGrindstone);
        commandWorkbenchStonecutter = getBool("workbench.stonecutter", commandWorkbenchStonecutter);
        commandWorkbenchSmithing = getBool("workbench.smithing", commandWorkbenchSmithing);
        commandWorkbenchCartography = getBool("workbench.cartography", commandWorkbenchCartography);
        commandWorkbenchLoom = getBool("workbench.loom", commandWorkbenchLoom);

        // Cheat commands
        commandFlyEnabled = getBool("commands.fly", commandFlyEnabled);
        commandGodEnabled = getBool("commands.god", commandGodEnabled);
        commandHealEnabled = getBool("commands.heal", commandHealEnabled);
        commandFeedEnabled = getBool("commands.feed", commandFeedEnabled);

        // Silk Touch Spawners
        silkTouchSpawnersEnabled = getBool("silkTouchSpawners.enabled", silkTouchSpawnersEnabled);

        // Silk Touch Special Blocks
        silkTouchBuddingAmethystEnabled = getBool("silkTouchBuddingAmethyst.enabled", silkTouchBuddingAmethystEnabled);
        silkTouchReinforcedDeepslateEnabled = getBool("silkTouchReinforcedDeepslate.enabled", silkTouchReinforcedDeepslateEnabled);
        silkTouchSuspiciousSandEnabled = getBool("silkTouchSuspiciousSand.enabled", silkTouchSuspiciousSandEnabled);
        silkTouchSuspiciousGravelEnabled = getBool("silkTouchSuspiciousGravel.enabled", silkTouchSuspiciousGravelEnabled);

        // Villager in a Bucket
        villagerBucketEnabled = getBool("villagerBucket.enabled", villagerBucketEnabled);
        villagerBucketZombieEnabled = getBool("villagerBucket.zombieEnabled", villagerBucketZombieEnabled);

        // Spawn Immunity
        spawnImmunityEnabled = getBool("spawnImmunity.enabled", spawnImmunityEnabled);

        // Enchantment Conflict Override
        enchantConflictOverrideEnabled = getBool("enchantConflictOverride.enabled", enchantConflictOverrideEnabled);
    }

    public void save() {
        root = new JsonObject();

        // General
        putBool("general.overrideOtherMods", overrideOtherMods);
        putInt("general.commandPriority", commandPriority);

        // Fullbright
        putBool("fullbright.enabled", fullbrightEnabled);
        putBool("fullbright.defaultState", fullbrightDefault);
        putString("fullbright.keybind", fullbrightKeybind);

        // Tree Chopper
        putBool("treeChopper.enabled", treeChopperEnabled);
        putBool("treeChopper.defaultState", treeChopperDefault);
        putString("treeChopper.keybind", treeChopperKeybind);
        putBool("treeChopper.mustHoldAxe", treeChopperMustHoldAxe);
        putBool("treeChopper.sneakToChop", treeChopperSneakToChop);
        putBool("treeChopper.ignorePlayerMade", treeChopperIgnorePlayerMade);
        putBool("treeChopper.replantSaplings", treeChopperReplantSaplings);
        putBool("treeChopper.fastLeafDecay", treeChopperFastLeafDecay);
        putBool("treeChopper.netherTrees", treeChopperNetherTrees);
        putBool("treeChopper.hugeMushrooms", treeChopperHugeMushrooms);
        putDouble("treeChopper.durabilityModifier", treeChopperDurabilityModifier);

        // Trade Refresh
        putBool("tradeRefresh.enabled", tradeRefreshEnabled);
        putString("tradeRefresh.keybind", tradeRefreshKeybind);

        // Hunger Display
        putBool("hungerDisplay.enabled", hungerDisplayEnabled);
        putBool("hungerDisplay.showSaturation", hungerDisplayShowSaturation);
        putBool("hungerDisplay.showExhaustion", hungerDisplayShowExhaustion);
        putBool("hungerDisplay.showFoodValues", hungerDisplayShowFoodValues);
        putString("hungerDisplay.saturationColor", hungerDisplaySaturationColor);
        putString("hungerDisplay.exhaustionColor", hungerDisplayExhaustionColor);
        putString("hungerDisplay.foodValueColor", hungerDisplayFoodValueColor);

        // Accurate Block Placement
        putBool("accurateBlockPlacement.enabled", accurateBlockPlacementEnabled);
        putBool("accurateBlockPlacement.disableFastBreakOnServers", accurateBlockPlacementDisableFastBreakOnServers);

        // InvMove
        putBool("invMove.enabled", invMoveEnabled);
        putBool("invMove.defaultState", invMoveDefault);
        putString("invMove.keybind", invMoveKeybind);

        // Recipe Viewer
        putBool("recipeViewer.enabled", recipeViewerEnabled);

        // Commands
        putBool("commands.tpa", commandTpaEnabled);
        putBool("commands.warp", commandWarpEnabled);
        putBool("commands.ban", commandBanEnabled);
        putBool("commands.kick", commandKickEnabled);
        putBool("commands.channel", commandChannelEnabled);
        putBool("commands.home", commandHomeEnabled);
        putBool("commands.back", commandBackEnabled);
        putBool("commands.dback", commandDbackEnabled);
        putBool("commands.recipe", commandRecipeEnabled);
        putBool("commands.spawn", commandSpawnEnabled);
        putBool("commands.rtp", commandRtpEnabled);
        putBool("commands.gamemode", commandGamemodeEnabled);

        // Homes
        putInt("homes.maxPerPlayer", homesMaxPerPlayer);

        // Teleport
        putInt("teleport.cooldownSeconds", tpaCooldownSeconds);
        putInt("teleport.timeoutSeconds", tpaTimeoutSeconds);
        putInt("teleport.invulnerabilitySeconds", tpaInvulnerabilitySeconds);

        // Back
        putInt("back.cooldownSeconds", backCooldownSeconds);

        // Teleport Countdown
        putInt("teleport.countdownSeconds", tpCountdownSeconds);

        // RTP
        putInt("rtp.radius", rtpRadius);
        putInt("rtp.cooldownSeconds", rtpCooldownSeconds);
        putInt("rtp.minRadius", rtpMinRadius);

        // Command Aliases
        putBool("commandAliases.enabled", commandAliasesEnabled);

        // Lore command
        putBool("commands.lore", commandLoreEnabled);
        putBool("lore.creativeByDefault", loreCommandCreativeByDefault);

        // Workbench commands
        putBool("commands.workbench", commandWorkbenchEnabled);
        putBool("workbench.craft", commandWorkbenchCraft);
        putBool("workbench.enderchest", commandWorkbenchEnderChest);
        putBool("workbench.anvil", commandWorkbenchAnvil);
        putBool("workbench.grindstone", commandWorkbenchGrindstone);
        putBool("workbench.stonecutter", commandWorkbenchStonecutter);
        putBool("workbench.smithing", commandWorkbenchSmithing);
        putBool("workbench.cartography", commandWorkbenchCartography);
        putBool("workbench.loom", commandWorkbenchLoom);

        // Cheat commands
        putBool("commands.fly", commandFlyEnabled);
        putBool("commands.god", commandGodEnabled);
        putBool("commands.heal", commandHealEnabled);
        putBool("commands.feed", commandFeedEnabled);

        // Silk Touch Spawners
        putBool("silkTouchSpawners.enabled", silkTouchSpawnersEnabled);

        // Silk Touch Special Blocks
        putBool("silkTouchBuddingAmethyst.enabled", silkTouchBuddingAmethystEnabled);
        putBool("silkTouchReinforcedDeepslate.enabled", silkTouchReinforcedDeepslateEnabled);
        putBool("silkTouchSuspiciousSand.enabled", silkTouchSuspiciousSandEnabled);
        putBool("silkTouchSuspiciousGravel.enabled", silkTouchSuspiciousGravelEnabled);

        // Villager in a Bucket
        putBool("villagerBucket.enabled", villagerBucketEnabled);
        putBool("villagerBucket.zombieEnabled", villagerBucketZombieEnabled);

        // Spawn Immunity
        putBool("spawnImmunity.enabled", spawnImmunityEnabled);

        // Enchantment Conflict Override
        putBool("enchantConflictOverride.enabled", enchantConflictOverrideEnabled);

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            System.err.println("[QoLMod] Failed to save config: " + e.getMessage());
        }
    }

    // ===== Nested JSON helpers (dot-separated paths) =====

    private JsonObject getOrCreateSection(String sectionName) {
        if (!root.has(sectionName) || !root.get(sectionName).isJsonObject()) {
            root.add(sectionName, new JsonObject());
        }
        return root.getAsJsonObject(sectionName);
    }

    private boolean getBool(String path, boolean def) {
        String[] parts = path.split("\\.", 2);
        JsonObject section = root.has(parts[0]) && root.get(parts[0]).isJsonObject()
                ? root.getAsJsonObject(parts[0]) : null;
        if (section != null && section.has(parts[1]) && section.get(parts[1]).isJsonPrimitive()) {
            return section.get(parts[1]).getAsBoolean();
        }
        return def;
    }

    private int getInt(String path, int def) {
        String[] parts = path.split("\\.", 2);
        JsonObject section = root.has(parts[0]) && root.get(parts[0]).isJsonObject()
                ? root.getAsJsonObject(parts[0]) : null;
        if (section != null && section.has(parts[1]) && section.get(parts[1]).isJsonPrimitive()) {
            return section.get(parts[1]).getAsInt();
        }
        return def;
    }

    private double getDouble(String path, double def) {
        String[] parts = path.split("\\.", 2);
        JsonObject section = root.has(parts[0]) && root.get(parts[0]).isJsonObject()
                ? root.getAsJsonObject(parts[0]) : null;
        if (section != null && section.has(parts[1]) && section.get(parts[1]).isJsonPrimitive()) {
            return section.get(parts[1]).getAsDouble();
        }
        return def;
    }

    private String getString(String path, String def) {
        String[] parts = path.split("\\.", 2);
        JsonObject section = root.has(parts[0]) && root.get(parts[0]).isJsonObject()
                ? root.getAsJsonObject(parts[0]) : null;
        if (section != null && section.has(parts[1]) && section.get(parts[1]).isJsonPrimitive()) {
            return section.get(parts[1]).getAsString();
        }
        return def;
    }

    private void putBool(String path, boolean value) {
        String[] parts = path.split("\\.", 2);
        getOrCreateSection(parts[0]).addProperty(parts[1], value);
    }

    private void putInt(String path, int value) {
        String[] parts = path.split("\\.", 2);
        getOrCreateSection(parts[0]).addProperty(parts[1], value);
    }

    private void putDouble(String path, double value) {
        String[] parts = path.split("\\.", 2);
        getOrCreateSection(parts[0]).addProperty(parts[1], value);
    }

    private void putString(String path, String value) {
        String[] parts = path.split("\\.", 2);
        getOrCreateSection(parts[0]).addProperty(parts[1], value);
    }

    // ===== Public accessors for Mod Menu config screen =====

    public boolean getBool(String key) {
        try {
            var field = QoLConfig.class.getDeclaredField(toCamelCase(key));
            field.setAccessible(true);
            return field.getBoolean(this);
        } catch (Exception e) {
            return getBool(mapKeyToConfigPath(key), false);
        }
    }

    public void put(String key, boolean value) {
        try {
            var field = QoLConfig.class.getDeclaredField(toCamelCase(key));
            field.setAccessible(true);
            field.setBoolean(this, value);
        } catch (Exception e) {
            // Fallback — update root JSON directly
        }
    }

    /** Get a String config value by dot-path key (e.g. "hungerDisplay.saturationColor"). */
    public String getStr(String key, String defaultValue) {
        try {
            var field = QoLConfig.class.getDeclaredField(toCamelCase(key));
            field.setAccessible(true);
            Object val = field.get(this);
            return val instanceof String s ? s : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** Set a String config value by dot-path key. */
    public void put(String key, String value) {
        try {
            var field = QoLConfig.class.getDeclaredField(toCamelCase(key));
            field.setAccessible(true);
            field.set(this, value);
        } catch (Exception e) {
            // Fallback — ignore
        }
    }

    private String toCamelCase(String dotPath) {
        String[] parts = dotPath.split("\\.");
        if (parts.length == 1) return parts[0];
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1));
        }
        return sb.toString();
    }

    private String mapKeyToConfigPath(String key) {
        // Map simple keys to config paths
        return switch (key) {
            case "overrideOtherMods" -> "general.overrideOtherMods";
            case "fullbright.enabled" -> "fullbright.enabled";
            case "treeChopper.enabled" -> "treeChopper.enabled";
            case "tradeRefresh.enabled" -> "tradeRefresh.enabled";
            case "hungerDisplay.enabled" -> "hungerDisplay.enabled";
            case "accurateBlockPlacement.enabled" -> "accurateBlockPlacement.enabled";
            case "recipeViewer.enabled" -> "recipeViewer.enabled";
            case "invMove.enabled" -> "invMove.enabled";
            default -> key;
        };
    }
}
