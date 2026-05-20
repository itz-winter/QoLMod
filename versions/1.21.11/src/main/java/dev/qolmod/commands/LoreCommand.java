package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.qolmod.QoLMod;
import dev.qolmod.util.TextUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;

/**
 * /lore command — view and modify the lore of the held item.
 *
 * Subcommands:
 *   /lore set <text>          — Replace all lore with the given text (\n splits lines)
 *   /lore add <text>          — Append one or more lines
 *   /lore insert <line> <text>— Insert before a given 1-based line number
 *   /lore remove <line>       — Remove a specific 1-based line number
 *   /lore clear               — Remove all lore
 *
 * Permission: op (level 2) OR (creative AND loreCommandCreativeByDefault config)
 * Config gate: commandLoreEnabled OR (loreCommandCreativeByDefault AND player creative)
 */
public class LoreCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(CommandManager.literal("lore")
                .requires(LoreCommand::canUse)

                // /lore set <text>
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String text = StringArgumentType.getString(ctx, "text");
                                    return executeSet(ctx.getSource(), text);
                                })))

                // /lore add <text>
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String text = StringArgumentType.getString(ctx, "text");
                                    return executeAdd(ctx.getSource(), text);
                                })))

                // /lore clear
                .then(CommandManager.literal("clear")
                        .executes(ctx -> executeClear(ctx.getSource())))

                // /lore remove <line>
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("line", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int line = IntegerArgumentType.getInteger(ctx, "line");
                                    return executeRemove(ctx.getSource(), line);
                                })))

                // /lore insert <line> <text>
                .then(CommandManager.literal("insert")
                        .then(CommandManager.argument("line", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            int line = IntegerArgumentType.getInteger(ctx, "line");
                                            String text = StringArgumentType.getString(ctx, "text");
                                            return executeInsert(ctx.getSource(), line, text);
                                        }))))
        );
    }

    private static boolean canUse(ServerCommandSource source) {
        var config = QoLMod.getConfig();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return false;
        boolean isCreative = player.interactionManager.getGameMode() == GameMode.CREATIVE;
        // Allow if lore enabled globally, or if creative mode + creativeByDefault config
        if (config.commandLoreEnabled) return true;
        if (config.loreCommandCreativeByDefault && isCreative) return true;
        return false;
    }

    // ===== Subcommands =====

    private static int executeSet(ServerCommandSource source, String rawText) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty() || stack.getItem() == Items.AIR) {
            player.sendMessage(Text.literal("§cYou must hold an item."));
            return 0;
        }

        List<Text> lore = parseLines(rawText);
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        player.sendMessage(Text.literal("§aLore set (" + lore.size() + " line(s))."));
        return 1;
    }

    private static int executeAdd(ServerCommandSource source, String rawText) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty() || stack.getItem() == Items.AIR) {
            player.sendMessage(Text.literal("§cYou must hold an item."));
            return 0;
        }

        List<Text> current = getMutableLore(stack);
        List<Text> newLines = parseLines(rawText);
        current.addAll(newLines);
        stack.set(DataComponentTypes.LORE, new LoreComponent(current));
        player.sendMessage(Text.literal("§aAdded " + newLines.size() + " lore line(s). Total: " + current.size() + "."));
        return 1;
    }

    private static int executeClear(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty() || stack.getItem() == Items.AIR) {
            player.sendMessage(Text.literal("§cYou must hold an item."));
            return 0;
        }

        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of()));
        player.sendMessage(Text.literal("§aLore cleared."));
        return 1;
    }

    private static int executeRemove(ServerCommandSource source, int lineNumber) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty() || stack.getItem() == Items.AIR) {
            player.sendMessage(Text.literal("§cYou must hold an item."));
            return 0;
        }

        List<Text> current = getMutableLore(stack);
        if (lineNumber < 1 || lineNumber > current.size()) {
            player.sendMessage(Text.literal("§cLine " + lineNumber + " does not exist. Lore has " + current.size() + " line(s)."));
            return 0;
        }

        current.remove(lineNumber - 1);
        stack.set(DataComponentTypes.LORE, new LoreComponent(current));
        player.sendMessage(Text.literal("§aRemoved line " + lineNumber + ". " + current.size() + " line(s) remaining."));
        return 1;
    }

    private static int executeInsert(ServerCommandSource source, int lineNumber, String rawText) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty() || stack.getItem() == Items.AIR) {
            player.sendMessage(Text.literal("§cYou must hold an item."));
            return 0;
        }

        List<Text> current = getMutableLore(stack);
        int insertAt = Math.min(lineNumber - 1, current.size());
        List<Text> newLines = parseLines(rawText);
        current.addAll(insertAt, newLines);
        stack.set(DataComponentTypes.LORE, new LoreComponent(current));
        player.sendMessage(Text.literal("§aInserted " + newLines.size() + " line(s) at position " + lineNumber + "."));
        return 1;
    }

    // ===== Helpers =====

    /** Parses raw text, splitting on \n, applying & color codes. */
    private static List<Text> parseLines(String rawText) {
        String translated = TextUtils.translateAmpersand(rawText);
        String[] parts = translated.split("\\\\n|\\n");
        List<Text> result = new ArrayList<>();
        for (String part : parts) {
            result.add(Text.literal(part).setStyle(Style.EMPTY.withItalic(false)));
        }
        return result;
    }

    /** Returns a mutable copy of the current lore, or an empty list. */
    private static List<Text> getMutableLore(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return new ArrayList<>();
        return new ArrayList<>(lore.lines());
    }
}
