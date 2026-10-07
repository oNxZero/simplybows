package net.sweenus.simplybows.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.registry.ComponentRegistry;

public record BowUpgradeData(int stringLevel, int frameLevel, RuneEtching runeEtching) {

    private static final String ROOT_KEY = "simplybows_upgrades";
    private static final String STRING_KEY = "enchanted_string";
    private static final String FRAME_KEY = "reinforced_frame";
    private static final String RUNE_KEY = "rune";
    private static int maxLevelPerType() { return SimplyBowsConfig.INSTANCE.upgrades.maxLevelPerType.get(); }
    private static int maxTotalUpgradeSlots() { return SimplyBowsConfig.INSTANCE.upgrades.maxTotalSlots.get(); }
    private static double sizeMultiplierPerString() { return SimplyBowsConfig.INSTANCE.upgrades.sizeMultiplierPerString.get(); }
    private static double damageMultiplierPerFrame() { return SimplyBowsConfig.INSTANCE.upgrades.damageMultiplierPerFrame.get(); }

    public static final Codec<BowUpgradeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf(STRING_KEY).orElse(0).forGetter(BowUpgradeData::stringLevel),
            Codec.INT.fieldOf(FRAME_KEY).orElse(0).forGetter(BowUpgradeData::frameLevel),
            Codec.STRING.xmap(RuneEtching::fromId, RuneEtching::id).fieldOf(RUNE_KEY).orElse(RuneEtching.NONE).forGetter(BowUpgradeData::runeEtching)
    ).apply(instance, BowUpgradeData::new));

    public static BowUpgradeData none() {
        return new BowUpgradeData(0, 0, RuneEtching.NONE);
    }

    public static BowUpgradeData from(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return none();
        }
        BowUpgradeData stored = stack.get(ComponentRegistry.UPGRADES.get());
        if (stored != null) {
            return stored.clamped();
        }
        return readLegacy(stack);
    }

    public static void migrateLegacy(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.get(ComponentRegistry.UPGRADES.get()) != null) {
            return;
        }
        BowUpgradeData legacy = readLegacy(stack);
        if (legacy.equals(none())) {
            return;
        }
        stack.set(ComponentRegistry.UPGRADES.get(), legacy);
    }

    public void write(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.set(ComponentRegistry.UPGRADES.get(), this.clamped());
    }

    private BowUpgradeData clamped() {
        return new BowUpgradeData(clampLevel(this.stringLevel), clampLevel(this.frameLevel), this.runeEtching == null ? RuneEtching.NONE : this.runeEtching);
    }

    private static BowUpgradeData readLegacy(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return none();
        }
        NbtCompound root = customData.copyNbt();
        if (!root.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)) {
            return none();
        }
        NbtCompound upgrades = root.getCompound(ROOT_KEY);
        return new BowUpgradeData(
                clampLevel(upgrades.getInt(STRING_KEY)),
                clampLevel(upgrades.getInt(FRAME_KEY)),
                RuneEtching.fromId(upgrades.getString(RUNE_KEY))
        );
    }

    public BowUpgradeData withIncreasedString() {
        if (this.stringLevel >= maxLevelPerType() || this.stringLevel + this.frameLevel >= maxTotalUpgradeSlots()) {
            return this;
        }
        return new BowUpgradeData(this.stringLevel + 1, this.frameLevel, this.runeEtching);
    }

    public BowUpgradeData withIncreasedFrame() {
        if (this.frameLevel >= maxLevelPerType() || this.stringLevel + this.frameLevel >= maxTotalUpgradeSlots()) {
            return this;
        }
        return new BowUpgradeData(this.stringLevel, this.frameLevel + 1, this.runeEtching);
    }

    public BowUpgradeData withRune(RuneEtching rune) {
        return new BowUpgradeData(this.stringLevel, this.frameLevel, rune == null ? RuneEtching.NONE : rune);
    }

    public double sizeMultiplier() {
        return 1.0 + this.stringLevel * sizeMultiplierPerString();
    }

    public double damageMultiplier() {
        return 1.0 + this.frameLevel * damageMultiplierPerFrame();
    }

    public int bonusKnockback() {
        return this.frameLevel;
    }

    private static int clampLevel(int level) {
        return Math.max(0, Math.min(maxLevelPerType(), level));
    }

    public static int getMaxLevelPerType() {
        return maxLevelPerType();
    }

    public static int getMaxTotalUpgradeSlots() {
        return maxTotalUpgradeSlots();
    }
}
