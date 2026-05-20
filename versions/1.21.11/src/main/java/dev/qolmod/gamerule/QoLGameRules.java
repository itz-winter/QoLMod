package dev.qolmod.gamerule;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Identifier;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleCategory;
import net.minecraft.world.rule.GameRuleType;

/**
 * Custom gamerules for QoLMod.
 *
 * Usage (server-side):
 *   world.getGameRules().getValue(QoLGameRules.TP_COUNTDOWN_SECONDS)
 *   world.getGameRules().getValue(QoLGameRules.HOMES_MAX_PER_PLAYER)
 *   world.getGameRules().getValue(QoLGameRules.SILK_TOUCH_SPAWNERS)
 *   world.getGameRules().getValue(QoLGameRules.SPAWN_IMMUNITY_TICKS)
 *   world.getGameRules().getValue(QoLGameRules.SILK_TOUCH_BUDDING_AMETHYST)
 *   world.getGameRules().getValue(QoLGameRules.SILK_TOUCH_REINFORCED_DEEPSLATE)
 *   world.getGameRules().getValue(QoLGameRules.SILK_TOUCH_SUSPICIOUS_SAND)
 *   world.getGameRules().getValue(QoLGameRules.SILK_TOUCH_SUSPICIOUS_GRAVEL)
 *
 * These are changeable in-game with:
 *   /gamerule qolmod:tp_countdown <seconds>
 *   /gamerule qolmod:homes_max <count>
 *   /gamerule qolmod:silk_touch_spawners true|false
 *   /gamerule qolmod:spawn_immunity_ticks <ticks>
 *   /gamerule qolmod:silk_touch_budding_amethyst true|false
 *   /gamerule qolmod:silk_touch_reinforced_deepslate true|false
 *   /gamerule qolmod:silk_touch_suspicious_sand true|false
 *   /gamerule qolmod:silk_touch_suspicious_gravel true|false
 *
 * Default values are taken from config on first world load.
 */
public final class QoLGameRules {

    /** Seconds of countdown before any teleport fires. 0 = instant. */
    public static final GameRule<Integer> TP_COUNTDOWN_SECONDS = Registry.register(
            Registries.GAME_RULE,
            Identifier.of("qolmod", "tp_countdown"),
            new GameRule<>(
                    GameRuleCategory.MISC,
                    GameRuleType.INT,
                    IntegerArgumentType.integer(0),
                    (visitor, rule) -> visitor.visitInt(rule),
                    Codec.INT,
                    i -> i,
                    0,
                    FeatureSet.empty()
            )
    );

    /** Maximum number of homes a non-op player can have. */
    public static final GameRule<Integer> HOMES_MAX_PER_PLAYER = Registry.register(
            Registries.GAME_RULE,
            Identifier.of("qolmod", "homes_max"),
            new GameRule<>(
                    GameRuleCategory.MISC,
                    GameRuleType.INT,
                    IntegerArgumentType.integer(1),
                    (visitor, rule) -> visitor.visitInt(rule),
                    Codec.INT,
                    i -> i,
                    12,
                    FeatureSet.empty()
            )
    );

    /**
     * Whether players can pick up spawners using a Silk Touch tool.
     * The spawner retains its mob type (NBT data). Default: false.
     */
    public static final GameRule<Boolean> SILK_TOUCH_SPAWNERS = Registry.register(
            Registries.GAME_RULE,
            Identifier.of("qolmod", "silk_touch_spawners"),
            new GameRule<>(
                    GameRuleCategory.DROPS,
                    GameRuleType.BOOL,
                    BoolArgumentType.bool(),
                    (visitor, rule) -> visitor.visitBoolean(rule),
                    Codec.BOOL,
                    b -> b ? 1 : 0,
                    false,
                    FeatureSet.empty()
            )
    );

    /**
     * How many ticks of damage immunity a player receives when they spawn or respawn.
     * 20 ticks = 1 second. Set to 0 to disable. Default: 60 (3 seconds).
     */
    public static final GameRule<Integer> SPAWN_IMMUNITY_TICKS = Registry.register(
            Registries.GAME_RULE,
            Identifier.of("qolmod", "spawn_immunity_ticks"),
            new GameRule<>(
                    GameRuleCategory.SPAWNING,
                    GameRuleType.INT,
                    IntegerArgumentType.integer(0),
                    (visitor, rule) -> visitor.visitInt(rule),
                    Codec.INT,
                    i -> i,
                    60,
                    FeatureSet.empty()
            )
    );

    /** Whether budding amethyst can be picked up with a Silk Touch tool. Default: true. */
    public static final GameRule<Boolean> SILK_TOUCH_BUDDING_AMETHYST = Registry.register(
            Registries.GAME_RULE,
            Identifier.of("qolmod", "silk_touch_budding_amethyst"),
            new GameRule<>(
                    GameRuleCategory.DROPS,
                    GameRuleType.BOOL,
                    BoolArgumentType.bool(),
                    (visitor, rule) -> visitor.visitBoolean(rule),
                    Codec.BOOL,
                    b -> b ? 1 : 0,
                    true,
                    FeatureSet.empty()
            )
    );

    /** Whether reinforced deepslate can be picked up with a Silk Touch tool. Default: false. */
    public static final GameRule<Boolean> SILK_TOUCH_REINFORCED_DEEPSLATE = Registry.register(
            Registries.GAME_RULE,
            Identifier.of("qolmod", "silk_touch_reinforced_deepslate"),
            new GameRule<>(
                    GameRuleCategory.DROPS,
                    GameRuleType.BOOL,
                    BoolArgumentType.bool(),
                    (visitor, rule) -> visitor.visitBoolean(rule),
                    Codec.BOOL,
                    b -> b ? 1 : 0,
                    false,
                    FeatureSet.empty()
            )
    );

    /** Whether suspicious sand can be picked up with a Silk Touch tool. Default: true. */
    public static final GameRule<Boolean> SILK_TOUCH_SUSPICIOUS_SAND = Registry.register(
            Registries.GAME_RULE,
            Identifier.of("qolmod", "silk_touch_suspicious_sand"),
            new GameRule<>(
                    GameRuleCategory.DROPS,
                    GameRuleType.BOOL,
                    BoolArgumentType.bool(),
                    (visitor, rule) -> visitor.visitBoolean(rule),
                    Codec.BOOL,
                    b -> b ? 1 : 0,
                    true,
                    FeatureSet.empty()
            )
    );

    /** Whether suspicious gravel can be picked up with a Silk Touch tool. Default: true. */
    public static final GameRule<Boolean> SILK_TOUCH_SUSPICIOUS_GRAVEL = Registry.register(
            Registries.GAME_RULE,
            Identifier.of("qolmod", "silk_touch_suspicious_gravel"),
            new GameRule<>(
                    GameRuleCategory.DROPS,
                    GameRuleType.BOOL,
                    BoolArgumentType.bool(),
                    (visitor, rule) -> visitor.visitBoolean(rule),
                    Codec.BOOL,
                    b -> b ? 1 : 0,
                    true,
                    FeatureSet.empty()
            )
    );

    private QoLGameRules() {}

    /**
     * Calling this method forces the static initializer to run and registers
     * all gamerules. Call once during mod initialization.
     */
    public static void init() {
        // static fields are initialized on first class access — this is enough
    }
}