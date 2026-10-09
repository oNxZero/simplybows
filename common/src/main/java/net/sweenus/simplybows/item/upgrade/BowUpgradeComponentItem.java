package net.sweenus.simplybows.item.upgrade;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.item.tooltip.TooltipType;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.BowUpgradeTooltip;

import java.util.List;

public class BowUpgradeComponentItem extends Item {

    private final UpgradeKind upgradeKind;
    private final RuneEtching runeEtching;

    public BowUpgradeComponentItem(Settings settings, UpgradeKind upgradeKind, RuneEtching runeEtching) {
        super(settings);
        this.upgradeKind = upgradeKind;
        this.runeEtching = runeEtching == null ? RuneEtching.NONE : runeEtching;
    }

    public UpgradeKind getUpgradeKind() { return upgradeKind; }

    public RuneEtching getRuneEtching() { return runeEtching; }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        BowUpgradeTooltip.appendComponentTooltip(tooltip, this.upgradeKind, this.runeEtching);
    }

    public BowUpgradeData applyTo(BowUpgradeData current) {
        return switch (this.upgradeKind) {
            case ENCHANTED_STRING -> current.withIncreasedString();
            case REINFORCED_FRAME -> current.withIncreasedFrame();
            case RUNE_ETCHING -> current.withRune(this.runeEtching);
        };
    }

    public int getAnvilCost(BowUpgradeData before, BowUpgradeData after) {
        if (before.equals(after)) {
            return 0;
        }
        return switch (this.upgradeKind) {
            case ENCHANTED_STRING -> 2 + after.stringLevel();
            case REINFORCED_FRAME -> 2 + after.frameLevel();
            case RUNE_ETCHING -> 5;
        };
    }

    public enum UpgradeKind {
        ENCHANTED_STRING,
        REINFORCED_FRAME,
        RUNE_ETCHING;

        Text displayName(RuneEtching runeEtching) {
            return switch (this) {
                case ENCHANTED_STRING -> Text.translatable("item.simplybows.upgrades.enchanted_bow_string");
                case REINFORCED_FRAME -> Text.translatable("item.simplybows.upgrades.reinforced_bow_frame");
                case RUNE_ETCHING -> Text.translatable("item.simplybows.upgrades.rune_etching_" + runeEtching.id());
            };
        }
    }

    private String getComponentTooltipKey() {
        return switch (this.upgradeKind) {
            case ENCHANTED_STRING -> "tooltip.simplybows.upgrade_component.enchanted_string";
            case REINFORCED_FRAME -> "tooltip.simplybows.upgrade_component.reinforced_frame";
            case RUNE_ETCHING -> "tooltip.simplybows.upgrade_component.rune." + this.runeEtching.id();
        };
    }
}
