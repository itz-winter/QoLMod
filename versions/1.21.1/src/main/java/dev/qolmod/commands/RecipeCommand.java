package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Registers /recipe <item> — shows crafting recipe in chat.
 * A simple text-based recipe viewer (full GUI would require client-side screen).
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

        // Try to find the item
        Identifier itemId;
        if (itemName.contains(":")) {
            itemId = Identifier.of(itemName);
        } else {
            itemId = Identifier.of("minecraft", itemName);
        }

        if (!Registries.ITEM.containsId(itemId)) {
            player.sendMessage(Text.literal("§cUnknown item: " + itemName));
            return 0;
        }

        var item = Registries.ITEM.get(itemId);
        var recipeManager = ctx.getSource().getServer().getRecipeManager();

        // Find recipes that produce this item
        var recipes = recipeManager.values().stream()
                .filter(entry -> {
                    ItemStack output = entry.value().getResult(ctx.getSource().getServer().getRegistryManager());
                    return output.getItem() == item;
                })
                .toList();

        if (recipes.isEmpty()) {
            player.sendMessage(Text.literal("§cNo recipes found for: " + itemName));
            return 0;
        }

        player.sendMessage(Text.literal("§6=== Recipes for " + item.getName().getString() + " ==="));
        int count = 0;
        for (var recipeEntry : recipes) {
            if (count >= 3) {
                player.sendMessage(Text.literal("§7... and " + (recipes.size() - 3) + " more."));
                break;
            }

            Recipe<?> recipe = recipeEntry.value();
            String type = recipe.getType().toString();
            player.sendMessage(Text.literal("§e" + type));

            var ingredients = recipe.getIngredients();
            StringBuilder sb = new StringBuilder("§7Ingredients: ");
            for (int i = 0; i < ingredients.size(); i++) {
                var ingredient = ingredients.get(i);
                if (ingredient.isEmpty()) continue;
                var stacks = ingredient.getMatchingStacks();
                if (stacks.length > 0) {
                    if (i > 0) sb.append("§7, ");
                    sb.append("§f").append(stacks[0].getName().getString());
                }
            }
            player.sendMessage(Text.literal(sb.toString()));

            ItemStack output = recipe.getResult(ctx.getSource().getServer().getRegistryManager());
            player.sendMessage(Text.literal("§7Output: §a" + output.getCount() + "x " + output.getName().getString()));
            count++;
        }

        return 1;
    }
}
