package dev.qolmod.features.homes;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Server-side paginated homes GUI.
 *
 * Layout (6 rows = 54 slots):
 *   Rows 0-4 (slots 0-44): home items - left-click teleports, right-click opens icon
 *     picker, shift-click shows delete confirm.
 *   Row 5 (slots 45-53): navigation - slot 45 = prev page, slot 49 = info, slot 53 = next.
 *
 * Icon Picker (6 rows):
 *   Rows 0-4 (slots 0-44): icon items from palette (paginated, 45 per page)
 *   Row 5 nav: slot 45 = back, slot 46 = prev icon page, slot 49 = info,
 *              slot 52 = next icon page, slot 53 = "Use Held Item"
 *   Clicking a slot in the player-inventory area (54-89) also sets that item as the icon.
 *
 * Delete Confirm (1 row = 9 slots):
 *   slot 2 = cancel, slot 4 = display, slot 6 = confirm
 */
public class HomeGUI {

    private static final int HOMES_PER_PAGE = 45;
    private static final int ICONS_PER_PAGE = 45;

    private enum Mode { HOMES_LIST, ICON_PICKER, DELETE_CONFIRM }

    private static final Map<UUID, GUIState> playerStates = new HashMap<>();

    private static class GUIState {
        Mode mode;
        int page;
        int iconPage;
        String pendingName;

        GUIState(Mode mode, int page, int iconPage, String pendingName) {
            this.mode = mode;
            this.page = page;
            this.iconPage = iconPage;
            this.pendingName = pendingName;
        }
    }

    // ===== Public open methods =====

    public static void open(ServerPlayerEntity player, HomeManager homeManager) {
        open(player, homeManager, 0);
    }

    public static void open(ServerPlayerEntity player, HomeManager homeManager, int page) {
        playerStates.put(player.getUuid(), new GUIState(Mode.HOMES_LIST, page, 0, null));
        openHomesScreen(player, homeManager, page);
    }

    // ===== Screen openers =====

    private static void openHomesScreen(ServerPlayerEntity player, HomeManager homeManager, int page) {
        List<HomeManager.HomeData> homes = new ArrayList<>(homeManager.getHomes(player.getUuid()).values());
        int totalHomes = homes.size();
        int maxPage = totalHomes == 0 ? 0 : (totalHomes - 1) / HOMES_PER_PAGE;
        int clampedPage = Math.max(0, Math.min(page, maxPage));

        SimpleInventory inv = new SimpleInventory(54);
        ItemStack filler = makeFiller();

        int startIndex = clampedPage * HOMES_PER_PAGE;
        for (int i = 0; i < HOMES_PER_PAGE && startIndex + i < homes.size(); i++) {
            inv.setStack(i, makeHomeItem(homes.get(startIndex + i)));
        }
        for (int i = 0; i < 45; i++) {
            if (inv.getStack(i).isEmpty()) inv.setStack(i, filler);
        }

        for (int i = 45; i < 54; i++) inv.setStack(i, filler);
        if (clampedPage > 0) {
            inv.setStack(45, makeNavItem(Items.ARROW, "§ePrevious Page",
                    "§7Page " + clampedPage + " of " + (maxPage + 1)));
        }
        inv.setStack(49, makeNavItem(Items.BOOK,
                "§6Homes §7(" + totalHomes + ")",
                "§7Page " + (clampedPage + 1) + " of " + (maxPage + 1),
                "§eLeft-click §fhome: Teleport",
                "§eRight-click §fhome: Change icon",
                "§eShift-click §fhome: Delete"));
        if ((clampedPage + 1) * HOMES_PER_PAGE < totalHomes) {
            inv.setStack(53, makeNavItem(Items.ARROW, "§eNext Page",
                    "§7Page " + (clampedPage + 2) + " of " + (maxPage + 1)));
        }

        final int finalPage = clampedPage;
        Text title = Text.literal("§8Homes §7(" + totalHomes + ")");
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> makeHandler(syncId, playerInv, inv,
                        (ServerPlayerEntity) p, homeManager, Mode.HOMES_LIST, finalPage, 0, null),
                title
        ));
    }

    private static void openIconPickerScreen(ServerPlayerEntity player, HomeManager homeManager,
                                             String homeName, int returnPage, int iconPage) {
        playerStates.put(player.getUuid(), new GUIState(Mode.ICON_PICKER, returnPage, iconPage, homeName));

        String[] icons = getIconPalette();
        int totalIcons = icons.length;
        int maxIconPage = totalIcons == 0 ? 0 : (totalIcons - 1) / ICONS_PER_PAGE;
        int clampedIconPage = Math.max(0, Math.min(iconPage, maxIconPage));

        SimpleInventory inv = new SimpleInventory(54);
        ItemStack filler = makeFiller();

        int startIdx = clampedIconPage * ICONS_PER_PAGE;
        for (int i = 0; i < ICONS_PER_PAGE && startIdx + i < totalIcons; i++) {
            inv.setStack(i, makeIconItem(icons[startIdx + i]));
        }
        for (int i = 0; i < 45; i++) {
            if (inv.getStack(i).isEmpty()) inv.setStack(i, filler);
        }

        for (int i = 45; i < 54; i++) inv.setStack(i, filler);
        inv.setStack(45, makeNavItem(Items.BARRIER, "§cBack", "§7Return to homes list"));
        if (clampedIconPage > 0) {
            inv.setStack(46, makeNavItem(Items.ARROW, "§ePrevious Icons",
                    "§7Page " + clampedIconPage + " of " + (maxIconPage + 1)));
        }
        inv.setStack(49, makeNavItem(Items.COMPASS,
                "§6Choose Icon §7for §e" + homeName,
                "§7Page " + (clampedIconPage + 1) + " of " + (maxIconPage + 1),
                "§eClick §fan icon above to set it",
                "§eOr click §fany item in your inventory",
                "§eSlot 53 §f= Use your held item"));
        if ((clampedIconPage + 1) * ICONS_PER_PAGE < totalIcons) {
            inv.setStack(52, makeNavItem(Items.ARROW, "§eNext Icons",
                    "§7Page " + (clampedIconPage + 2) + " of " + (maxIconPage + 1)));
        }
        inv.setStack(53, makeNavItem(Items.STICK, "§aUse Held Item",
                "§7Sets the icon to whatever",
                "§7item you are currently holding"));

        final int finalIconPage = clampedIconPage;
        Text title = Text.literal("§8Choose Icon §7for §e" + homeName);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> makeHandler(syncId, playerInv, inv,
                        (ServerPlayerEntity) p, homeManager, Mode.ICON_PICKER, returnPage, finalIconPage, homeName),
                title
        ));
    }

    private static void openDeleteConfirmScreen(ServerPlayerEntity player, HomeManager homeManager,
                                               String homeName, int returnPage) {
        playerStates.put(player.getUuid(), new GUIState(Mode.DELETE_CONFIRM, returnPage, 0, homeName));

        SimpleInventory inv = new SimpleInventory(9);
        ItemStack filler = makeFiller();
        for (int i = 0; i < 9; i++) inv.setStack(i, filler);
        inv.setStack(2, makeNavItem(Items.RED_STAINED_GLASS_PANE, "§cCancel", "§7Keep §e" + homeName));
        inv.setStack(4, makeNavItem(Items.BARRIER, "§4Delete §e" + homeName, "§7This cannot be undone!"));
        inv.setStack(6, makeNavItem(Items.LIME_STAINED_GLASS_PANE, "§aConfirm Delete",
                "§7Permanently delete §e" + homeName));

        Text title = Text.literal("§cDelete §e" + homeName + "§c?");
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInv, p) -> makeDeleteConfirmHandler(syncId, playerInv, inv,
                        (ServerPlayerEntity) p, homeManager, homeName, returnPage),
                title
        ));
    }

    // ===== Screen handler factories =====

    private static GenericContainerScreenHandler makeHandler(int syncId, PlayerInventory playerInv,
                                                             SimpleInventory inv, ServerPlayerEntity player,
                                                             HomeManager homeManager,
                                                             Mode mode, int page, int iconPage, String pendingName) {
        return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X6, syncId, playerInv, inv, 6) {
            @Override
            public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity p) {
                if (slotIndex < 0) return;
                ServerPlayerEntity spe = (ServerPlayerEntity) p;
                GUIState state = playerStates.getOrDefault(spe.getUuid(),
                        new GUIState(mode, page, iconPage, pendingName));

                if (state.mode == Mode.HOMES_LIST) {
                    handleHomesListClick(slotIndex, button, actionType, spe, homeManager, state.page);
                } else if (state.mode == Mode.ICON_PICKER) {
                    handleIconPickerClick(slotIndex, button, actionType, spe, homeManager,
                            state.page, state.iconPage, state.pendingName);
                }
                sendContentUpdates();
            }

            @Override
            public ItemStack quickMove(PlayerEntity player, int index) { return ItemStack.EMPTY; }

            @Override
            public boolean canUse(PlayerEntity player) { return true; }
        };
    }

    private static GenericContainerScreenHandler makeDeleteConfirmHandler(int syncId, PlayerInventory playerInv,
                                                                          SimpleInventory inv, ServerPlayerEntity player,
                                                                          HomeManager homeManager,
                                                                          String homeName, int returnPage) {
        return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X1, syncId, playerInv, inv, 1) {
            @Override
            public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity p) {
                if (slotIndex < 0) return;
                ServerPlayerEntity spe = (ServerPlayerEntity) p;
                if (slotIndex == 6) {
                    homeManager.deleteHome(spe.getUuid(), homeName);
                    spe.sendMessage(Text.literal("§aHome §e" + homeName + " §adeleted."));
                    playerStates.remove(spe.getUuid());
                    openHomesScreen(spe, homeManager, returnPage);
                } else if (slotIndex == 2) {
                    playerStates.put(spe.getUuid(), new GUIState(Mode.HOMES_LIST, returnPage, 0, null));
                    openHomesScreen(spe, homeManager, returnPage);
                }
                sendContentUpdates();
            }

            @Override
            public ItemStack quickMove(PlayerEntity player, int index) { return ItemStack.EMPTY; }

            @Override
            public boolean canUse(PlayerEntity player) { return true; }
        };
    }

    // ===== Click handlers =====

    private static void handleHomesListClick(int slot, int button, SlotActionType type,
                                             ServerPlayerEntity player, HomeManager homeManager, int page) {
        if (slot < HOMES_PER_PAGE) {
            List<HomeManager.HomeData> homes = new ArrayList<>(homeManager.getHomes(player.getUuid()).values());
            int homeIndex = page * HOMES_PER_PAGE + slot;
            if (homeIndex >= homes.size()) return;
            HomeManager.HomeData home = homes.get(homeIndex);

            if (type == SlotActionType.QUICK_MOVE) {
                openDeleteConfirmScreen(player, homeManager, home.name, page);
            } else if (type == SlotActionType.PICKUP && button == 1) {
                openIconPickerScreen(player, homeManager, home.name, page, 0);
            } else if (type == SlotActionType.PICKUP && button == 0) {
                player.closeHandledScreen();
                playerStates.remove(player.getUuid());
                dev.qolmod.features.homes.HomeTeleportHelper.teleport(player, homeManager, home.name);
            }
        } else if (slot >= 45) {
            List<HomeManager.HomeData> homes = new ArrayList<>(homeManager.getHomes(player.getUuid()).values());
            int totalHomes = homes.size();
            if (slot == 45 && page > 0) {
                openHomesScreen(player, homeManager, page - 1);
            } else if (slot == 53 && (page + 1) * HOMES_PER_PAGE < totalHomes) {
                openHomesScreen(player, homeManager, page + 1);
            }
        }
    }

    private static void handleIconPickerClick(int slot, int button, SlotActionType type,
                                              ServerPlayerEntity player, HomeManager homeManager,
                                              int returnPage, int iconPage, String homeName) {
        // Player inventory slots in 6-row handler:
        //   54-80 = player main inventory rows 0-2 (player inv slots 9-35)
        //   81-89 = hotbar (player inv slots 0-8)
        if (slot >= 54) {
            int invIndex;
            if (slot >= 81) {
                invIndex = slot - 81; // hotbar 0-8
            } else {
                invIndex = slot - 54 + 9; // main inv 9-35
            }
            if (invIndex >= 0 && invIndex < player.getInventory().size()) {
                ItemStack held = player.getInventory().getStack(invIndex);
                if (!held.isEmpty()) {
                    setIconFromStack(player, homeManager, homeName, returnPage, held);
                }
            }
            return;
        }

        if (slot == 45) {
            playerStates.put(player.getUuid(), new GUIState(Mode.HOMES_LIST, returnPage, 0, null));
            openHomesScreen(player, homeManager, returnPage);
            return;
        }
        if (slot == 46 && iconPage > 0) {
            openIconPickerScreen(player, homeManager, homeName, returnPage, iconPage - 1);
            return;
        }
        if (slot == 52) {
            String[] icons = getIconPalette();
            int maxIconPage = icons.length == 0 ? 0 : (icons.length - 1) / ICONS_PER_PAGE;
            if (iconPage < maxIconPage) {
                openIconPickerScreen(player, homeManager, homeName, returnPage, iconPage + 1);
            }
            return;
        }
        if (slot == 53) {
            ItemStack held = player.getMainHandStack();
            if (!held.isEmpty()) {
                setIconFromStack(player, homeManager, homeName, returnPage, held);
            } else {
                player.sendMessage(Text.literal("§cYou are not holding any item."));
            }
            return;
        }

        if (slot < ICONS_PER_PAGE && type == SlotActionType.PICKUP) {
            String[] icons = getIconPalette();
            int idx = iconPage * ICONS_PER_PAGE + slot;
            if (idx >= icons.length) return;
            homeManager.setHomeIcon(player.getUuid(), homeName, icons[idx]);
            player.sendMessage(Text.literal("§aIcon updated for home §e" + homeName + "§a."));
            playerStates.put(player.getUuid(), new GUIState(Mode.HOMES_LIST, returnPage, 0, null));
            openHomesScreen(player, homeManager, returnPage);
        }
    }

    private static void setIconFromStack(ServerPlayerEntity player, HomeManager homeManager,
                                         String homeName, int returnPage, ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (id == null || stack.getItem() == Items.AIR) {
            player.sendMessage(Text.literal("§cCannot use that item as an icon."));
            return;
        }
        homeManager.setHomeIcon(player.getUuid(), homeName, id.toString());
        player.sendMessage(Text.literal("§aIcon updated for home §e" + homeName + "§a."));
        playerStates.put(player.getUuid(), new GUIState(Mode.HOMES_LIST, returnPage, 0, null));
        openHomesScreen(player, homeManager, returnPage);
    }

    // ===== Item builders =====

    private static ItemStack makeHomeItem(HomeManager.HomeData home) {
        Item item = Registries.ITEM.get(Identifier.tryParse(home.icon));
        if (item == Items.AIR) item = Items.OAK_SIGN;
        ItemStack stack = new ItemStack(item);

        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("§a" + home.name).setStyle(Style.EMPTY.withItalic(false)));

        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal("§7" + home.pos.getX() + ", " + home.pos.getY() + ", " + home.pos.getZ())
                .setStyle(Style.EMPTY.withItalic(false)));
        lore.add(Text.literal("§7" + formatDimension(home.dimension.getValue().toString()))
                .setStyle(Style.EMPTY.withItalic(false)));
        if (!home.description.isEmpty()) {
            lore.add(Text.literal("§8" + home.description).setStyle(Style.EMPTY.withItalic(false)));
        }
        lore.add(Text.literal("").setStyle(Style.EMPTY.withItalic(false)));
        lore.add(Text.literal("§eLeft-click§f: Teleport").setStyle(Style.EMPTY.withItalic(false)));
        lore.add(Text.literal("§eRight-click§f: Change icon").setStyle(Style.EMPTY.withItalic(false)));
        lore.add(Text.literal("§eShift-click§f: Delete").setStyle(Style.EMPTY.withItalic(false)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return stack;
    }

    private static ItemStack makeIconItem(String iconId) {
        Item item = Registries.ITEM.get(Identifier.tryParse(iconId));
        if (item == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        String displayName = iconId.contains(":") ? iconId.substring(iconId.indexOf(':') + 1) : iconId;
        displayName = displayName.replace('_', ' ');
        if (!displayName.isEmpty())
            displayName = Character.toUpperCase(displayName.charAt(0)) + displayName.substring(1);
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal("§f" + displayName).setStyle(Style.EMPTY.withItalic(false)));
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§7Click to use as icon").setStyle(Style.EMPTY.withItalic(false))
        )));
        return stack;
    }

    private static ItemStack makeNavItem(Item item, String... lines) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(lines[0]).setStyle(Style.EMPTY.withItalic(false)));
        if (lines.length > 1) {
            List<Text> lore = new ArrayList<>();
            for (int i = 1; i < lines.length; i++)
                lore.add(Text.literal(lines[i]).setStyle(Style.EMPTY.withItalic(false)));
            stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        }
        return stack;
    }

    private static ItemStack makeFiller() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" ").setStyle(Style.EMPTY.withItalic(false)));
        return stack;
    }

    private static String formatDimension(String dimId) {
        return switch (dimId) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "The End";
            default -> dimId;
        };
    }

    // ===== Expanded icon palette (200+ items across 5 pages) =====

    private static String[] getIconPalette() {
        return new String[]{
            // === Signs ===
            "minecraft:oak_sign", "minecraft:spruce_sign", "minecraft:birch_sign",
            "minecraft:jungle_sign", "minecraft:acacia_sign", "minecraft:dark_oak_sign",
            "minecraft:cherry_sign", "minecraft:mangrove_sign", "minecraft:bamboo_sign",
            "minecraft:crimson_sign", "minecraft:warped_sign",

            // === Homes / Containers / Furniture ===
            "minecraft:bed", "minecraft:white_bed", "minecraft:red_bed", "minecraft:blue_bed",
            "minecraft:chest", "minecraft:ender_chest", "minecraft:trapped_chest",
            "minecraft:barrel", "minecraft:shulker_box", "minecraft:white_shulker_box",
            "minecraft:purple_shulker_box", "minecraft:blue_shulker_box",
            "minecraft:crafting_table", "minecraft:furnace", "minecraft:blast_furnace",
            "minecraft:smoker", "minecraft:anvil", "minecraft:grindstone",
            "minecraft:smithing_table", "minecraft:stonecutter", "minecraft:cartography_table",
            "minecraft:loom", "minecraft:enchanting_table", "minecraft:bookshelf",
            "minecraft:chiseled_bookshelf", "minecraft:lectern", "minecraft:brewing_stand",
            "minecraft:cauldron", "minecraft:composter",

            // === Lights ===
            "minecraft:lantern", "minecraft:soul_lantern", "minecraft:torch",
            "minecraft:soul_torch", "minecraft:glowstone", "minecraft:sea_lantern",
            "minecraft:shroomlight", "minecraft:campfire", "minecraft:soul_campfire",
            "minecraft:end_rod", "minecraft:beacon", "minecraft:crying_obsidian",
            "minecraft:jack_o_lantern", "minecraft:froglight",

            // === Nature / Plants / Flowers ===
            "minecraft:oak_sapling", "minecraft:spruce_sapling", "minecraft:birch_sapling",
            "minecraft:jungle_sapling", "minecraft:acacia_sapling", "minecraft:dark_oak_sapling",
            "minecraft:cherry_sapling", "minecraft:mangrove_propagule",
            "minecraft:dandelion", "minecraft:poppy", "minecraft:blue_orchid",
            "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip",
            "minecraft:orange_tulip", "minecraft:pink_tulip", "minecraft:white_tulip",
            "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:lily_of_the_valley",
            "minecraft:sunflower", "minecraft:lilac", "minecraft:rose_bush",
            "minecraft:peony", "minecraft:pitcher_plant", "minecraft:torchflower",
            "minecraft:red_mushroom", "minecraft:brown_mushroom",
            "minecraft:cactus", "minecraft:bamboo", "minecraft:sugar_cane",
            "minecraft:lily_pad", "minecraft:vine", "minecraft:sea_pickle",
            "minecraft:kelp", "minecraft:seagrass", "minecraft:dried_kelp_block",
            "minecraft:wheat", "minecraft:melon", "minecraft:pumpkin",
            "minecraft:chorus_flower", "minecraft:glow_berries",

            // === Ores & Minerals ===
            "minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot",
            "minecraft:iron_ingot", "minecraft:netherite_ingot", "minecraft:amethyst_shard",
            "minecraft:copper_ingot", "minecraft:lapis_lazuli", "minecraft:quartz",
            "minecraft:coal", "minecraft:redstone", "minecraft:glowstone_dust",
            "minecraft:prismarine_crystals", "minecraft:prismarine_shard",
            "minecraft:diamond_ore", "minecraft:emerald_ore", "minecraft:deepslate_gold_ore",
            "minecraft:deepslate_iron_ore", "minecraft:deepslate_diamond_ore",
            "minecraft:ancient_debris", "minecraft:raw_gold", "minecraft:raw_iron",
            "minecraft:raw_copper",

            // === Building Blocks ===
            "minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log",
            "minecraft:jungle_log", "minecraft:acacia_log", "minecraft:dark_oak_log",
            "minecraft:cherry_log", "minecraft:mangrove_log", "minecraft:bamboo_block",
            "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks",
            "minecraft:jungle_planks", "minecraft:acacia_planks", "minecraft:dark_oak_planks",
            "minecraft:cherry_planks", "minecraft:mangrove_planks",
            "minecraft:stone", "minecraft:cobblestone", "minecraft:mossy_cobblestone",
            "minecraft:smooth_stone", "minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
            "minecraft:cracked_stone_bricks", "minecraft:chiseled_stone_bricks",
            "minecraft:granite", "minecraft:polished_granite",
            "minecraft:diorite", "minecraft:polished_diorite",
            "minecraft:andesite", "minecraft:polished_andesite",
            "minecraft:calcite", "minecraft:tuff", "minecraft:deepslate",
            "minecraft:cobbled_deepslate", "minecraft:deepslate_bricks", "minecraft:deepslate_tiles",
            "minecraft:sand", "minecraft:red_sand", "minecraft:gravel",
            "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol",
            "minecraft:grass_block", "minecraft:mycelium", "minecraft:rooted_dirt",
            "minecraft:mud", "minecraft:packed_mud", "minecraft:mud_bricks",
            "minecraft:snow_block", "minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice",
            "minecraft:obsidian", "minecraft:bricks",
            "minecraft:terracotta", "minecraft:white_terracotta", "minecraft:orange_terracotta",
            "minecraft:light_blue_terracotta", "minecraft:yellow_terracotta",
            "minecraft:lime_terracotta", "minecraft:pink_terracotta",
            "minecraft:gray_terracotta", "minecraft:cyan_terracotta", "minecraft:purple_terracotta",
            "minecraft:blue_terracotta", "minecraft:brown_terracotta",
            "minecraft:green_terracotta", "minecraft:red_terracotta", "minecraft:black_terracotta",
            "minecraft:white_glazed_terracotta", "minecraft:orange_glazed_terracotta",
            "minecraft:yellow_glazed_terracotta", "minecraft:lime_glazed_terracotta",
            "minecraft:cyan_glazed_terracotta", "minecraft:blue_glazed_terracotta",
            "minecraft:clay", "minecraft:hay_block", "minecraft:sponge",
            "minecraft:prismarine", "minecraft:dark_prismarine", "minecraft:prismarine_bricks",
            "minecraft:sandstone", "minecraft:smooth_sandstone", "minecraft:chiseled_sandstone",
            "minecraft:red_sandstone", "minecraft:smooth_red_sandstone",
            "minecraft:quartz_block", "minecraft:smooth_quartz", "minecraft:quartz_pillar",

            // === Wool & Concrete ===
            "minecraft:white_wool", "minecraft:orange_wool", "minecraft:magenta_wool",
            "minecraft:light_blue_wool", "minecraft:yellow_wool", "minecraft:lime_wool",
            "minecraft:pink_wool", "minecraft:gray_wool", "minecraft:light_gray_wool",
            "minecraft:cyan_wool", "minecraft:purple_wool", "minecraft:blue_wool",
            "minecraft:brown_wool", "minecraft:green_wool", "minecraft:red_wool",
            "minecraft:black_wool",
            "minecraft:white_concrete", "minecraft:orange_concrete", "minecraft:magenta_concrete",
            "minecraft:light_blue_concrete", "minecraft:yellow_concrete", "minecraft:lime_concrete",
            "minecraft:pink_concrete", "minecraft:gray_concrete", "minecraft:cyan_concrete",
            "minecraft:purple_concrete", "minecraft:blue_concrete", "minecraft:brown_concrete",
            "minecraft:green_concrete", "minecraft:red_concrete", "minecraft:black_concrete",

            // === Nether ===
            "minecraft:nether_brick", "minecraft:red_nether_bricks",
            "minecraft:netherrack", "minecraft:soul_sand", "minecraft:soul_soil",
            "minecraft:basalt", "minecraft:polished_basalt", "minecraft:smooth_basalt",
            "minecraft:blackstone", "minecraft:polished_blackstone", "minecraft:gilded_blackstone",
            "minecraft:crimson_planks", "minecraft:warped_planks",
            "minecraft:crimson_stem", "minecraft:warped_stem",
            "minecraft:crimson_fungus", "minecraft:warped_fungus",
            "minecraft:netherwart_block", "minecraft:warped_wart_block",
            "minecraft:magma_block", "minecraft:nether_wart",
            "minecraft:blaze_rod", "minecraft:ghast_tear", "minecraft:bone_block",

            // === End ===
            "minecraft:end_stone", "minecraft:end_stone_bricks",
            "minecraft:purpur_block", "minecraft:purpur_pillar",
            "minecraft:chorus_plant", "minecraft:dragon_egg",
            "minecraft:ender_pearl", "minecraft:eye_of_ender",
            "minecraft:shulker_shell", "minecraft:elytra",

            // === Tools & Combat ===
            "minecraft:wooden_sword", "minecraft:stone_sword", "minecraft:iron_sword",
            "minecraft:diamond_sword", "minecraft:netherite_sword",
            "minecraft:iron_axe", "minecraft:diamond_axe", "minecraft:netherite_axe",
            "minecraft:iron_pickaxe", "minecraft:diamond_pickaxe", "minecraft:netherite_pickaxe",
            "minecraft:iron_shovel", "minecraft:diamond_shovel",
            "minecraft:iron_hoe", "minecraft:diamond_hoe",
            "minecraft:bow", "minecraft:crossbow", "minecraft:trident",
            "minecraft:shield",
            "minecraft:iron_helmet", "minecraft:diamond_helmet", "minecraft:netherite_helmet",
            "minecraft:iron_chestplate", "minecraft:diamond_chestplate", "minecraft:netherite_chestplate",
            "minecraft:iron_leggings", "minecraft:diamond_leggings", "minecraft:netherite_leggings",
            "minecraft:iron_boots", "minecraft:diamond_boots", "minecraft:netherite_boots",
            "minecraft:totem_of_undying", "minecraft:fishing_rod",
            "minecraft:flint_and_steel", "minecraft:shears",
            "minecraft:mace",

            // === Food ===
            "minecraft:apple", "minecraft:golden_apple", "minecraft:enchanted_golden_apple",
            "minecraft:bread", "minecraft:cooked_beef", "minecraft:cooked_porkchop",
            "minecraft:cooked_chicken", "minecraft:cooked_mutton", "minecraft:cooked_salmon",
            "minecraft:cooked_cod", "minecraft:pumpkin_pie", "minecraft:cake",
            "minecraft:cookie", "minecraft:honey_bottle", "minecraft:suspicious_stew",
            "minecraft:carrot", "minecraft:golden_carrot", "minecraft:beetroot",
            "minecraft:melon_slice", "minecraft:sweet_berries",

            // === Potions & Magic ===
            "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion",
            "minecraft:experience_bottle", "minecraft:enchanted_book",
            "minecraft:name_tag", "minecraft:compass", "minecraft:clock",
            "minecraft:spyglass", "minecraft:map", "minecraft:filled_map",
            "minecraft:book", "minecraft:writable_book",

            // === Redstone ===
            "minecraft:redstone_block", "minecraft:observer", "minecraft:piston",
            "minecraft:sticky_piston", "minecraft:dispenser", "minecraft:dropper",
            "minecraft:hopper", "minecraft:comparator", "minecraft:repeater",
            "minecraft:lever", "minecraft:redstone_torch", "minecraft:target",
            "minecraft:tripwire_hook", "minecraft:daylight_detector",
            "minecraft:note_block", "minecraft:jukebox", "minecraft:tnt",
            "minecraft:crafting_table",

            // === Mob drops & misc ===
            "minecraft:bone", "minecraft:feather", "minecraft:egg",
            "minecraft:string", "minecraft:spider_eye", "minecraft:gunpowder",
            "minecraft:ink_sac", "minecraft:glow_ink_sac", "minecraft:rabbit_foot",
            "minecraft:leather", "minecraft:slime_ball", "minecraft:magma_cream",
            "minecraft:nether_star", "minecraft:heart_of_the_sea",
            "minecraft:nautilus_shell", "minecraft:turtle_scute",
            "minecraft:honeycomb", "minecraft:amethyst_block",
            "minecraft:wither_skeleton_skull", "minecraft:skeleton_skull",
            "minecraft:creeper_head", "minecraft:dragon_head", "minecraft:piglin_head",
            "minecraft:player_head",

            // === Special / Decorative ===
            "minecraft:painting", "minecraft:item_frame", "minecraft:glow_item_frame",
            "minecraft:armor_stand", "minecraft:flower_pot", "minecraft:decorated_pot",
            "minecraft:candle", "minecraft:white_candle", "minecraft:orange_candle",
            "minecraft:yellow_candle", "minecraft:lime_candle", "minecraft:cyan_candle",
            "minecraft:blue_candle", "minecraft:purple_candle", "minecraft:red_candle",
            "minecraft:black_candle",
            "minecraft:banner", "minecraft:white_banner", "minecraft:red_banner",
            "minecraft:blue_banner", "minecraft:yellow_banner", "minecraft:black_banner",
            "minecraft:globe_banner_pattern",
            "minecraft:spire_armor_trim_smithing_template",
            "minecraft:silence_armor_trim_smithing_template",
        };
    }

    /** Cleans up GUI state when a player leaves. */
    public static void onPlayerDisconnect(UUID playerId) {
        playerStates.remove(playerId);
    }
}
