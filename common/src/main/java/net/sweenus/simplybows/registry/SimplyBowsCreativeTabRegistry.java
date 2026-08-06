package net.sweenus.simplybows.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.sweenus.simplybows.SimplyBows;

public final class SimplyBowsCreativeTabRegistry {

    public static final DeferredRegister<ItemGroup> ITEM_GROUPS = DeferredRegister.create(SimplyBows.MOD_ID, RegistryKeys.ITEM_GROUP);

    public static final RegistrySupplier<ItemGroup> SIMPLY_BOWS_TAB = ITEM_GROUPS.register("simplybows",
            () -> ItemGroup.create(ItemGroup.Row.TOP, 0)
                    .displayName(Text.translatable("itemGroup.simplybows.simplybows"))
                    .icon(() -> new ItemStack(ItemRegistry.RUNE_ETCHING_GRACE.get()))
                    .entries((displayContext, entries) -> {
                        entries.add(new ItemStack(ItemRegistry.VINE_BOW.get()));
                        entries.add(new ItemStack(ItemRegistry.ICE_BOW.get()));
                        entries.add(new ItemStack(ItemRegistry.BUBBLE_BOW.get()));
                        entries.add(new ItemStack(ItemRegistry.BEE_BOW.get()));
                        entries.add(new ItemStack(ItemRegistry.BLOSSOM_BOW.get()));
                        entries.add(new ItemStack(ItemRegistry.EARTH_BOW.get()));
                        entries.add(new ItemStack(ItemRegistry.ECHO_BOW.get()));
                        entries.add(new ItemStack(ItemRegistry.COSMIC_BOW.get()));
                        entries.add(new ItemStack(ItemRegistry.ENCHANTED_BOW_STRING.get()));
                        entries.add(new ItemStack(ItemRegistry.REINFORCED_BOW_FRAME.get()));
                        entries.add(new ItemStack(ItemRegistry.RUNE_ETCHING_PAIN.get()));
                        entries.add(new ItemStack(ItemRegistry.RUNE_ETCHING_GRACE.get()));
                        entries.add(new ItemStack(ItemRegistry.RUNE_ETCHING_BOUNTY.get()));
                        entries.add(new ItemStack(ItemRegistry.RUNE_ETCHING_CHAOS.get()));
                    })
                    .build()
    );

    private SimplyBowsCreativeTabRegistry() {
    }

    public static void register() {
        ITEM_GROUPS.register();
    }
}
