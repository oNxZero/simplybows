package net.sweenus.simplybows.fabric.loot;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.loot.SimplyBowsChestLootRules;

public final class SimplyBowsLootInjector {

    private SimplyBowsLootInjector() {
    }

    public static void init() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            Identifier id = key.getValue();
            boolean chestContext = tableBuilder.build().getType() == LootContextTypes.CHEST;
            for (LootPool.Builder pool : SimplyBowsChestLootRules.createPoolsForChestTable(id.getNamespace(), id.getPath(), chestContext)) {
                tableBuilder.pool(pool);
            }
        });
    }

}
