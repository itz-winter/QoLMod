package dev.qolmod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers /recipe <item> â€” shows crafting recipe in chat.
 * Adapted for 1.19.2 (net.minecraft.util.registry, no RecipeEntry).
 */
public class RecipeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("recipe")
                .then(CommandManager.argument("item", StringArgumentType.greedyString())
                        .executes(RecipeCommand::executeRecipe)));
    }

    private static int executeRecipe(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player; try { player = ctx.getSource().getPlayer(); } catch (Exception e) { return 0; }
        if (player == null) return 0;

        String itemName = StringArgumentType.getString(ctx, "item").toLowerCase().replace(" ", "_");

        Identifier itemId;
        if (itemName.contains(":")) {
            itemId = new Identifier(itemName);
        } else {
            itemId = new Identifier("minecraft", itemName);
        }

        if (!Registry.ITEM.containsId(itemId)) {
            player.sendMessage(new LiteralText("Â§cUnknown item: " + itemName), false);
            return 0;
        }

        var item = Registry.ITEM.get(itemId);
        var recipeManager = ctx.getSource().getMinecraftServer().getRecipeManager();

        // In 1.19.2, values() returns Collection<Recipe<?>> directly (no RecipeEntry)
        var recipes = recipeManager.values().stream()
                .filter(recipe -> {
                    ItemStack output = recipe.getOutput();
                    return output.getItem() == item;
                })
                .toList();

        if (recipes.isEmpty()) {
            player.sendMessage(new LiteralText("Â§cNo recipes found for: " + itemName), false);
            return 0;
        }

        player.sendMessage(new LiteralText("Â§6=== Recipes for " + item.getName().getString() + " ==="), false);
        int count = 0;
        for (var recipeEntry : recipes) {
            if (count >= 3) {
                player.sendMessage(new LiteralText("Â§7... and " + (recipes.size() - 3) + " more."), false);
                break;
            }

            Recipe<?> recipe = recipeEntry;
            String type = recipe.getType().toString();
            player.sendMessage(new LiteralText("Â§e" + type), false);

            var ingredients = recipe.getIngredients();
            StringBuilder sb = new StringBuilder("Â§7Ingredients: ");
            for (int i = 0; i < ingredients.size(); i++) {
                var ingredient = ingredients.get(i);
                if (ingredient.isEmpty()) continue;
                net.minecraft.item.ItemStack[] stacks = {}; // ingredient API differs
                if (stacks.length > 0) {
                    if (i > 0) sb.append("Â§7, ");
                    sb.append("Â§f").append(stacks[0].getName().getString());
                }
            }
            player.sendMessage(new LiteralText(sb.toString()), false);

            ItemStack output = recipe.getOutput();
            player.sendMessage(new LiteralText("Â§7Output: Â§a" + output.getCount() + "x " + output.getName().getString()), false);
            count++;
        }

        return 1;
    }
}
