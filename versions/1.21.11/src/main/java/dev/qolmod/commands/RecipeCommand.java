package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.Registries;
import java.lang.reflect.Method;
import java.util.ArrayList;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Collection;

public class RecipeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("recipe")
            .then(CommandManager.argument("item", StringArgumentType.greedyString())
                .executes(RecipeCommand::executeRecipeLookup))
            .executes(ctx -> {
                ctx.getSource().sendFeedback(() -> Text.literal("Usage: /recipe <item name>").formatted(Formatting.YELLOW), false);
                return 1;
            }));
    }

    private static int executeRecipeLookup(CommandContext<ServerCommandSource> ctx) {
        String query = StringArgumentType.getString(ctx, "item").toLowerCase().replace(" ", "_");
        ServerCommandSource source = ctx.getSource();
        RecipeManager recipeManager = source.getServer().getRecipeManager();

        Item targetItem = null;
        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            if (id.getPath().contains(query)) {
                targetItem = item;
                break;
            }
        }

        if (targetItem == null) {
            source.sendFeedback(() -> Text.literal("No item found matching: " + query).formatted(Formatting.RED), false);
            return 0;
        }

        final Item foundItem = targetItem;
        final Identifier itemId = Registries.ITEM.getId(foundItem);

        boolean foundRecipe = false;

        // Try to obtain a collection of recipes from the recipe manager using reflection so
        // this code can work across multiple Minecraft versions with different Recipe APIs.
        Collection<?> allRecipes = new ArrayList<>();
        try {
            Method valuesMethod = recipeManager.getClass().getMethod("values");
            Object v = valuesMethod.invoke(recipeManager);
            if (v instanceof Collection) allRecipes = (Collection<?>) v;
        } catch (Throwable t) {
            // Fallbacks: try common alternative method names
            try {
                Method getAll = recipeManager.getClass().getMethod("getAll");
                Object v = getAll.invoke(recipeManager);
                if (v instanceof Collection) allRecipes = (Collection<?>) v;
            } catch (Throwable t2) {
                // Give up — leave allRecipes empty
            }
        }

        // Iterate and inspect recipes. Different MC versions expose recipes differently —
        // entries may be RecipeEntry<?> or Recipe<?> instances. Use reflection to extract a
        // result ItemStack when possible and skip otherwise.
        for (Object obj : allRecipes) {
            Recipe<?> recipe = null;
            if (obj instanceof RecipeEntry) {
                try { recipe = ((RecipeEntry<?>) obj).value(); } catch (Throwable ignored) {}
            } else if (obj instanceof Recipe) {
                recipe = (Recipe<?>) obj;
            }

            if (recipe == null) continue;

            try {
                ItemStack result = null;
                // Try common result access patterns via reflection
                try {
                    Method getResultWithRegistry = recipe.getClass().getMethod("getResult", source.getServer().getRegistryManager().getClass());
                    Object r = getResultWithRegistry.invoke(recipe, source.getServer().getRegistryManager());
                    if (r instanceof ItemStack) result = (ItemStack) r;
                } catch (Throwable ignored) {}

                if (result == null) {
                    try {
                        Method getResultNoArgs = recipe.getClass().getMethod("getResult");
                        Object r = getResultNoArgs.invoke(recipe);
                        if (r instanceof ItemStack) result = (ItemStack) r;
                    } catch (Throwable ignored) {}
                }

                // If still null, skip this recipe (API mismatch)
                if (result == null) continue;

                if (result.getItem() == foundItem) {
                    // Try to extract an id for the recipe entry if possible
                    String idTemp = recipe.toString();
                    if (obj instanceof RecipeEntry) {
                        try { idTemp = ((RecipeEntry<?>) obj).id().toString(); } catch (Throwable ignored) {}
                    }
                    final String finalIdStr = idTemp;
                    final ItemStack finalResult = result;

                    source.sendFeedback(() -> Text.literal("=== Recipe for ")
                        .append(Text.literal(itemId.getPath()).formatted(Formatting.GOLD))
                        .append(Text.literal(" ===")), false);
                    source.sendFeedback(() -> Text.literal("  Recipe ID: ")
                        .append(Text.literal(finalIdStr).formatted(Formatting.AQUA)), false);
                    source.sendFeedback(() -> Text.literal("  Result: ")
                        .append(Text.literal(finalResult.getCount() + "x " + itemId.getPath()).formatted(Formatting.GREEN)), false);
                    foundRecipe = true;
                }
            } catch (Throwable ignored) {
                // Skip recipes we cannot inspect on this version
            }
        }

        if (!foundRecipe) {
            source.sendFeedback(() -> Text.literal("No crafting recipe found for: " + itemId.getPath()).formatted(Formatting.RED), false);
        }

        return 1;
    }
}
