package net.sweenus.simplybows.loot;

import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.registry.ItemRegistry;

import java.util.ArrayList;
import java.util.List;

public final class SimplyBowsChestLootRules {

    private static final float LOOT_UI_CHANCE_SCALE = 1000.0F;

    private static float baseStringChance() { return normalizeLootChance(SimplyBowsConfig.INSTANCE.loot.baseStringChance.get()); }
    private static float baseFrameChance() { return normalizeLootChance(SimplyBowsConfig.INSTANCE.loot.baseFrameChance.get()); }
    private static float baseRuneChance() { return normalizeLootChance(SimplyBowsConfig.INSTANCE.loot.baseRuneChance.get()); }
    private static float baseUniqueBowChance() { return normalizeLootChance(SimplyBowsConfig.INSTANCE.loot.baseUniqueBowChance.get()); }
    private static float boostedBowChance() { return normalizeLootChance(SimplyBowsConfig.INSTANCE.loot.boostedBowChance.get()); }
    private static float boostedRuneChanceAncientCity() { return normalizeLootChance(SimplyBowsConfig.INSTANCE.loot.boostedRuneChanceAncientCity.get()); }

    private SimplyBowsChestLootRules() {
    }

    public static List<LootPool.Builder> createPoolsForChestTable(String namespace, String path) {
        if (!"minecraft".equals(namespace) || path == null || !path.startsWith("chests/")) {
            return List.of();
        }

        List<LootPool.Builder> pools = new ArrayList<>();
        addGlobalPools(pools);
        addBiomeSpecificBoosts(path, pools);
        return pools;
    }

    private static void addGlobalPools(List<LootPool.Builder> pools) {
        pools.add(singleItemChancePool(ItemRegistry.ENCHANTED_BOW_STRING.get(), baseStringChance()));
        pools.add(singleItemChancePool(ItemRegistry.REINFORCED_BOW_FRAME.get(), baseFrameChance()));
        pools.add(oneOfChancePool(
                baseRuneChance(),
                ItemRegistry.RUNE_ETCHING_PAIN.get(),
                ItemRegistry.RUNE_ETCHING_GRACE.get(),
                ItemRegistry.RUNE_ETCHING_BOUNTY.get(),
                ItemRegistry.RUNE_ETCHING_CHAOS.get()
        ));
        pools.add(oneOfChancePool(
                baseUniqueBowChance(),
                ItemRegistry.VINE_BOW.get(),
                ItemRegistry.ICE_BOW.get(),
                ItemRegistry.BUBBLE_BOW.get(),
                ItemRegistry.BEE_BOW.get(),
                ItemRegistry.BLOSSOM_BOW.get(),
                ItemRegistry.EARTH_BOW.get(),
                ItemRegistry.ECHO_BOW.get(),
                ItemRegistry.COSMIC_BOW.get()
        ));
    }

    private static void addBiomeSpecificBoosts(String path, List<LootPool.Builder> pools) {
        if (matchesAny(path, "chests/ocean_monument", "chests/underwater_ruin_big", "chests/underwater_ruin_small", "chests/ocean_ruin_cold", "chests/ocean_ruin_warm", "chests/shipwreck_supply", "chests/shipwreck_map", "chests/shipwreck_treasure")) {
            pools.add(singleItemChancePool(ItemRegistry.BUBBLE_BOW.get(), boostedBowChance()));
        }
        if (matchesAny(path, "chests/swamp_hut", "chests/woodland_mansion")) {
            pools.add(singleItemChancePool(ItemRegistry.ECHO_BOW.get(), boostedBowChance()));
        }
        if (matchesAny(path, "chests/jungle_temple")) {
            pools.add(singleItemChancePool(ItemRegistry.VINE_BOW.get(), boostedBowChance()));
        }
        if (path.startsWith("chests/village/")) {
            pools.add(singleItemChancePool(ItemRegistry.BEE_BOW.get(), boostedBowChance()));
        }
        if (matchesAny(path, "chests/abandoned_mineshaft")) {
            pools.add(singleItemChancePool(ItemRegistry.EARTH_BOW.get(), boostedBowChance()));
        }
        if (matchesAny(path, "chests/buried_treasure")) {
            pools.add(singleItemChancePool(ItemRegistry.BLOSSOM_BOW.get(), boostedBowChance()));
        }
        if (matchesAny(path, "chests/igloo_chest")) {
            pools.add(singleItemChancePool(ItemRegistry.ICE_BOW.get(), boostedBowChance()));
        }
        if (matchesAny(path, "chests/ancient_city")) {
            pools.add(oneOfChancePool(
                    boostedRuneChanceAncientCity(),
                    ItemRegistry.RUNE_ETCHING_PAIN.get(),
                    ItemRegistry.RUNE_ETCHING_GRACE.get(),
                    ItemRegistry.RUNE_ETCHING_BOUNTY.get(),
                    ItemRegistry.RUNE_ETCHING_CHAOS.get()
            ));
        }
        if (matchesAny(path, "chests/end_city_treasure")) {
            pools.add(singleItemChancePool(ItemRegistry.COSMIC_BOW.get(), boostedBowChance()));
        }
    }

    private static LootPool.Builder singleItemChancePool(Item item, float chance) {
        return LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .conditionally(RandomChanceLootCondition.builder(chance))
                .with(ItemEntry.builder(item));
    }

    private static LootPool.Builder oneOfChancePool(float chance, Item... items) {
        LootPool.Builder pool = LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .conditionally(RandomChanceLootCondition.builder(chance));
        for (Item item : items) {
            pool.with(ItemEntry.builder(item));
        }
        return pool;
    }

    private static boolean matchesAny(String path, String... expectedPaths) {
        for (String expected : expectedPaths) {
            if (expected.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private static float normalizeLootChance(float configuredValue) {
        if (configuredValue <= 0.0F) {
            return 0.0F;
        }
        // Backward compatibility: legacy configs used raw [0..1] chance values.
        if (configuredValue <= 1.0F) {
            return Math.min(1.0F, configuredValue);
        }
        return Math.min(1.0F, configuredValue / LOOT_UI_CHANCE_SCALE);
    }
}
