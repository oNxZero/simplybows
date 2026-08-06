package net.sweenus.simplybows.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.sweenus.simplybows.SimplyBows;
import net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem;
import net.sweenus.simplybows.item.unique.BeeBowItem;
import net.sweenus.simplybows.item.unique.BlossomBowItem;
import net.sweenus.simplybows.item.unique.BubbleBowItem;
import net.sweenus.simplybows.item.unique.CosmicBowItem;
import net.sweenus.simplybows.item.unique.EchoBowItem;
import net.sweenus.simplybows.item.unique.EarthBowItem;
import net.sweenus.simplybows.item.unique.IceBowItem;
import net.sweenus.simplybows.item.unique.VineBowItem;
import net.sweenus.simplybows.upgrade.RuneEtching;

public class ItemRegistry {

    public static final DeferredRegister<Item> ITEM = DeferredRegister.create(SimplyBows.MOD_ID, RegistryKeys.ITEM);

    public static final RegistrySupplier<Item> VINE_BOW = ITEM.register("vine_bow/vine_bow", () -> new VineBowItem(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> ICE_BOW = ITEM.register("ice_bow/ice_bow", () -> new IceBowItem(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BUBBLE_BOW = ITEM.register("bubble_bow/bubble_bow", () -> new BubbleBowItem(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BEE_BOW = ITEM.register("bee_bow/bee_bow", () -> new BeeBowItem(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BLOSSOM_BOW = ITEM.register("blossom_bow/blossom_bow", () -> new BlossomBowItem(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> EARTH_BOW = ITEM.register("earth_bow/earth_bow", () -> new EarthBowItem(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> ECHO_BOW = ITEM.register("echo_bow/echo_bow", () -> new EchoBowItem(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> COSMIC_BOW = ITEM.register("cosmic_bow/cosmic_bow", () -> new CosmicBowItem(
            new Item.Settings()
    ));
    // Dummy item — exists only so BakedModelManager loads the small inventory model.
    public static final RegistrySupplier<Item> ECHO_BOW_INVENTORY = ITEM.register("echo_bow/echo_bow_inventory", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> COSMIC_BOW_INVENTORY = ITEM.register("cosmic_bow/cosmic_bow_inventory", () -> new Item(
            new Item.Settings()
    ));
    // Dummy items — exist only so BakedModelManager loads the small inventory models.
    public static final RegistrySupplier<Item> EARTH_BOW_INVENTORY = ITEM.register("earth_bow/earth_bow_inventory", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> ICE_BOW_INVENTORY = ITEM.register("ice_bow/ice_bow_inventory", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BLOSSOM_BOW_INVENTORY = ITEM.register("blossom_bow/blossom_bow_inventory", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BUBBLE_BOW_INVENTORY = ITEM.register("bubble_bow/bubble_bow_inventory", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BEE_BOW_INVENTORY = ITEM.register("bee_bow/bee_bow_inventory", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> ECHO_BOW_VISUAL_PULL_0 = ITEM.register("echo_bow_visual_pull_0", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> ECHO_BOW_VISUAL_PULL_1 = ITEM.register("echo_bow_visual_pull_1", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> ECHO_BOW_VISUAL_PULL_2 = ITEM.register("echo_bow_visual_pull_2", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> VINE_BOW_VISUAL_PULL_0 = ITEM.register("vine_bow_visual_pull_0", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> VINE_BOW_VISUAL_PULL_1 = ITEM.register("vine_bow_visual_pull_1", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> VINE_BOW_VISUAL_PULL_2 = ITEM.register("vine_bow_visual_pull_2", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> ICE_BOW_VISUAL_PULL_0 = ITEM.register("ice_bow_visual_pull_0", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> ICE_BOW_VISUAL_PULL_1 = ITEM.register("ice_bow_visual_pull_1", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> ICE_BOW_VISUAL_PULL_2 = ITEM.register("ice_bow_visual_pull_2", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BUBBLE_BOW_VISUAL_PULL_0 = ITEM.register("bubble_bow_visual_pull_0", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BUBBLE_BOW_VISUAL_PULL_1 = ITEM.register("bubble_bow_visual_pull_1", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BUBBLE_BOW_VISUAL_PULL_2 = ITEM.register("bubble_bow_visual_pull_2", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BEE_BOW_VISUAL_PULL_0 = ITEM.register("bee_bow_visual_pull_0", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BEE_BOW_VISUAL_PULL_1 = ITEM.register("bee_bow_visual_pull_1", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BEE_BOW_VISUAL_PULL_2 = ITEM.register("bee_bow_visual_pull_2", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BLOSSOM_BOW_VISUAL_PULL_0 = ITEM.register("blossom_bow_visual_pull_0", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BLOSSOM_BOW_VISUAL_PULL_1 = ITEM.register("blossom_bow_visual_pull_1", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> BLOSSOM_BOW_VISUAL_PULL_2 = ITEM.register("blossom_bow_visual_pull_2", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> EARTH_BOW_VISUAL_PULL_0 = ITEM.register("earth_bow_visual_pull_0", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> EARTH_BOW_VISUAL_PULL_1 = ITEM.register("earth_bow_visual_pull_1", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> EARTH_BOW_VISUAL_PULL_2 = ITEM.register("earth_bow_visual_pull_2", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> COSMIC_BOW_VISUAL_PULL_0 = ITEM.register("cosmic_bow_visual_pull_0", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> COSMIC_BOW_VISUAL_PULL_1 = ITEM.register("cosmic_bow_visual_pull_1", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> COSMIC_BOW_VISUAL_PULL_2 = ITEM.register("cosmic_bow_visual_pull_2", () -> new Item(
            new Item.Settings()
    ));
    public static final RegistrySupplier<Item> ENCHANTED_BOW_STRING = ITEM.register("upgrades/enchanted_bow_string", () -> new BowUpgradeComponentItem(
            new Item.Settings(),
            BowUpgradeComponentItem.UpgradeKind.ENCHANTED_STRING,
            RuneEtching.NONE
    ));
    public static final RegistrySupplier<Item> REINFORCED_BOW_FRAME = ITEM.register("upgrades/reinforced_bow_frame", () -> new BowUpgradeComponentItem(
            new Item.Settings(),
            BowUpgradeComponentItem.UpgradeKind.REINFORCED_FRAME,
            RuneEtching.NONE
    ));
    public static final RegistrySupplier<Item> RUNE_ETCHING_PAIN = ITEM.register("upgrades/rune_etching_pain", () -> new BowUpgradeComponentItem(
            new Item.Settings(),
            BowUpgradeComponentItem.UpgradeKind.RUNE_ETCHING,
            RuneEtching.PAIN
    ));
    public static final RegistrySupplier<Item> RUNE_ETCHING_GRACE = ITEM.register("upgrades/rune_etching_grace", () -> new BowUpgradeComponentItem(
            new Item.Settings(),
            BowUpgradeComponentItem.UpgradeKind.RUNE_ETCHING,
            RuneEtching.GRACE
    ));
    public static final RegistrySupplier<Item> RUNE_ETCHING_BOUNTY = ITEM.register("upgrades/rune_etching_bounty", () -> new BowUpgradeComponentItem(
            new Item.Settings(),
            BowUpgradeComponentItem.UpgradeKind.RUNE_ETCHING,
            RuneEtching.BOUNTY
    ));
    public static final RegistrySupplier<Item> RUNE_ETCHING_CHAOS = ITEM.register("upgrades/rune_etching_chaos", () -> new BowUpgradeComponentItem(
            new Item.Settings(),
            BowUpgradeComponentItem.UpgradeKind.RUNE_ETCHING,
            RuneEtching.CHAOS
    ));


    public static void registerItems() {
        ITEM.register();
    }

}
