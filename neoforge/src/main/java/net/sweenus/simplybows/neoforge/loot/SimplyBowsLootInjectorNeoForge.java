package net.sweenus.simplybows.neoforge.loot;

import net.minecraft.loot.LootPool;
import net.minecraft.loot.context.LootContextTypes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.sweenus.simplybows.loot.SimplyBowsChestLootRules;

public final class SimplyBowsLootInjectorNeoForge {

    private SimplyBowsLootInjectorNeoForge() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(SimplyBowsLootInjectorNeoForge::onLootTableLoad);
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        String namespace = event.getName().getNamespace();
        String path = event.getName().getPath();
        boolean chestContext = event.getTable().getType() == LootContextTypes.CHEST;
        for (LootPool.Builder pool : SimplyBowsChestLootRules.createPoolsForChestTable(namespace, path, chestContext)) {
            event.getTable().addPool(pool.build());
        }
    }
}
