package net.sweenus.simplybows.registry;

import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.sweenus.simplybows.config.SimplyBowsConfig;

@Environment(EnvType.CLIENT)
public class SimplyBowsItemProperties {
    public static void addSimplyBowsItemProperties() {
        register();
    }

    public static void register() {
        makeBows(ItemRegistry.VINE_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedEverbloom.get());
        makeBows(ItemRegistry.ICE_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedWinterfang.get());
        makeBows(ItemRegistry.BUBBLE_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedBubbleveil.get());
        makeBows(ItemRegistry.BEE_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedBuzzkill.get());
        makeBows(ItemRegistry.BLOSSOM_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedPetalwind.get());
        makeBows(ItemRegistry.EARTH_BOW.get(), SimplyBowsConfig.INSTANCE.upgrades.drawSpeedTremorstrike.get());
    }

    public static void makeBows(Item item, float drawSpeed) {
        // Register "pull" predicate
        ItemPropertiesRegistry.register(item, Identifier.of("pull") , (itemStack, clientWorld, livingEntity, seed) -> {
            if (livingEntity == null) {
                return 0.0F;
            } else {
                int useTicks = itemStack.getMaxUseTime(livingEntity) - livingEntity.getItemUseTimeLeft();
                return livingEntity.getActiveItem() != itemStack ? 0.0F : (float) useTicks / drawSpeed;
            }
        });

        // Register "pulling" predicate
        ItemPropertiesRegistry.register(item, Identifier.of("pulling"), (itemStack, clientWorld, livingEntity, seed) ->
                livingEntity != null
                        && livingEntity.isUsingItem()
                        && livingEntity.getActiveItem() == itemStack ? 1.0F : 0.0F);
    }
}
