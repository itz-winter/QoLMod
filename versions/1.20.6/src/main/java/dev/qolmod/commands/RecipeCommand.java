package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Registers /recipe <item> — shows crafting recipe in chat.
 * Uses reflection for cross-version compatibility with Recipe API changes.
 */
public class RecipeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("recipe")
                .then(CommandManager.argument("item", StringArgumentType.greedyString())
                        .executes(RecipeCommand::executeRecipe)));
    }

    private static int executeRecipe(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String itemName = StringArgumentType.getString(ctx, "item").toLowerCase().replace(" ", "_");

        // Find the item in the registry
        Item targetItem = null;
        Identifier targetId = null;
        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            if (id.getPath().contains(itemName)) {
                targetItem = item;
                targetId = id;
                break;
            }
        }

        if (targetItem == null) {
            player.sendMessage(Text.literal("No item found matching: " + itemName).formatted(Formatting.RED));
            return 0;
        }

        final Item foundItem = targetItem;
        final Identifier foundId = targetId;

        // Get recipe manager and iterate recipes via reflection
        var recipeManager = ctx.getSource().getServer().getRecipeManager();
        Collection<?> allRecipes = new ArrayList<>();
        try {
            Method valuesMethod = recipeManager.getClass().getMethod("values");
            Object v = valuesMethod.invoke(recipeManager);
            if (v instanceof Collection) allRecipes = (Collection<?>) v;
        } catch (Throwable ignored) {}

        boolean found = false;
        int count = 0;
        for (Object obj : allRecipes) {
            if (count >= 5) break;
            try {
                Recipe<?> recipe = null;
                if (obj instanceof RecipeEntry) {
                    recipe = ((RecipeEntry<?>) obj).value();
                } else if (obj instanceof Recipe) {
                    recipe = (Recipe<?>) obj;
                }
                if (recipe == null) continue;

                // Try to get result via reflection
                ItemStack result = null;
                try {
                    Method m = recipe.getClass().getMethod("getResult", ctx.getSource().getServer().getRegistryManager().getClass());
                    Object r = m.invoke(recipe, ctx.getSource().getServer().getRegistryManager());
                    if (r instanceof ItemStack) result = (ItemStack) r;
                } catch (Throwable ignored) {}
                if (result == null) {
                    try {
                        Method m = recipe.getClass().getMethod("getResult");
                        Object r = m.invoke(recipe);
                        if (r instanceof ItemStack) result = (ItemStack) r;
                    } catch (Throwable ignored) {}
                }
                if (result == null || result.getItem() != foundItem) continue;

                final ItemStack finalResult = result;
                String entryId = obj instanceof RecipeEntry ? ((RecipeEntry<?>) obj).id().toString() : recipe.toString();
                final String fEntryId = entryId;

                if (!found) {
                    player.sendMessage(Text.literal("=== Recipes for " + foundId.getPath() + " ===").formatted(Formatting.GOLD));
                    found = true;
                }
                player.sendMessage(Text.literal("  Recipe: ").append(Text.literal(fEntryId).formatted(Formatting.AQUA)));
                player.sendMessage(Text.literal("  Output: " + finalResult.getCount() + "x " + foundId.getPath()).formatted(Formatting.GREEN));
                count++;
            } catch (Throwable ignored) {}
        }

        if (!found) {
            player.sendMessage(Text.literal("No crafting recipe found for: " + foundId.getPath()).formatted(Formatting.RED));
        }

        return 1;
    }
}
