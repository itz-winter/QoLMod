package dev.qolmod;

import dev.qolmod.item.VillagerBucketItem;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

/**
 * Registers all custom items added by QoLMod.
 */
public final class QoLItems {

    /** Bucket containing a villager with full profession/trade data preserved. */
    public static final VillagerBucketItem VILLAGER_IN_A_BUCKET = register(
            "villager_in_a_bucket",
            new VillagerBucketItem(
                    EntityType.VILLAGER,
                    SoundEvents.ENTITY_VILLAGER_TRADE,
                    settings("villager_in_a_bucket")
            )
    );

    /** Bucket containing a wandering trader. */
    public static final VillagerBucketItem WANDERING_TRADER_IN_A_BUCKET = register(
            "wandering_trader_in_a_bucket",
            new VillagerBucketItem(
                    EntityType.WANDERING_TRADER,
                    SoundEvents.ENTITY_WANDERING_TRADER_NO,
                    settings("wandering_trader_in_a_bucket")
            )
    );

    /** Bucket containing a zombie villager. */
    public static final VillagerBucketItem ZOMBIE_VILLAGER_IN_A_BUCKET = register(
            "zombie_villager_in_a_bucket",
            new VillagerBucketItem(
                    EntityType.ZOMBIE_VILLAGER,
                    SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT,
                    settings("zombie_villager_in_a_bucket")
            )
    );

    private static Item.Settings settings(String name) {
        return new Item.Settings()
                .maxCount(1)
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of("qolmod", name)));
    }

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, Identifier.of("qolmod", name), item);
    }

    /** Force static initialisation and registration of all items. */
    public static void init() {
        // Static fields are registered on first class access.
    }

    private QoLItems() {}
}
