package net.sweenus.simplybows.util;

import net.minecraft.text.Text;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.world.VineFlowerFieldManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BowUpgradeTooltip {
    private static final String[] BOW_KEYS = {"vine", "ice", "bubble", "bee", "blossom", "earth"};

    private BowUpgradeTooltip() {
    }

    public static List<String> detailLines(String bowKey, BowUpgradeData upgrades) {
        List<String> lines = new ArrayList<>();
        lines.add(stringLine(bowKey, upgrades));
        lines.add(frameLine(bowKey, upgrades));
        lines.addAll(runeLines(bowKey, upgrades));
        return lines;
    }

    public static String stringGain(String bowKey) {
        var upgrades = SimplyBowsConfig.INSTANCE.upgrades;
        return switch (bowKey) {
            case "vine" -> t("tooltip.simplybows.alt.string.vine", num(VineFlowerFieldManager.flowerPatchRadiusPerString()));
            case "ice" -> t("tooltip.simplybows.alt.string.ice");
            case "bubble" -> t("tooltip.simplybows.alt.string.bubble", seconds(SimplyBowsConfig.INSTANCE.bubbleveil.columnDurationBonusPerString.get()));
            case "bee" -> t("tooltip.simplybows.alt.string.bee", seconds(SimplyBowsConfig.INSTANCE.buzzkill.stringPoisonDurationBonus.get()));
            case "blossom" -> t("tooltip.simplybows.alt.string.blossom", seconds(SimplyBowsConfig.INSTANCE.petalwind.stormDurationBonusPerString.get()));
            case "earth" -> t("tooltip.simplybows.alt.string.earth", num(SimplyBowsConfig.INSTANCE.tremorstrike.stringRadiusBonusPerLevel.get()));
            default -> t("tooltip.simplybows.alt.string.generic");
        };
    }

    public static String frameGain(String bowKey) {
        var upgrades = SimplyBowsConfig.INSTANCE.upgrades;
        return switch (bowKey) {
            case "vine" -> t("tooltip.simplybows.alt.frame.vine", hearts(SimplyBowsConfig.INSTANCE.everbloom.friendlyHeal.get() * 0.6F * 0.5F));
            case "ice", "bee", "blossom" -> t("tooltip.simplybows.alt.frame.damage", pct(upgrades.damageMultiplierPerFrame.get()));
            case "bubble" -> t("tooltip.simplybows.alt.frame.bubble", num(SimplyBowsConfig.INSTANCE.bubbleveil.columnRadiusPerFrame.get()));
            case "earth" -> t("tooltip.simplybows.alt.frame.earth", pct(upgrades.damageMultiplierPerFrame.get()));
            default -> t("tooltip.simplybows.alt.frame.generic");
        };
    }

    public static String stringLine(String bowKey, BowUpgradeData upgrades) {
        int string = upgrades.stringLevel();
        return switch (bowKey) {
            case "vine" -> vineString(upgrades, string);
            case "ice" -> iceString(upgrades, string);
            case "bubble" -> bubbleString(upgrades, string);
            case "bee" -> beeString(upgrades, string);
            case "blossom" -> t("tooltip.simplybows.detail.blossom.string", string, seconds(blossomDuration(upgrades)));
            case "earth" -> t("tooltip.simplybows.detail.earth.string", string, num(earthRadius(upgrades)));
            default -> t("tooltip.simplybows.detail.generic.string", string);
        };
    }

    public static String frameLine(String bowKey, BowUpgradeData upgrades) {
        int frame = upgrades.frameLevel();
        return switch (bowKey) {
            case "vine" -> vineFrame(upgrades, frame);
            case "ice" -> t("tooltip.simplybows.detail.ice.frame", frame, num(iceDamage(upgrades)));
            case "bubble" -> bubbleFrame(upgrades, frame);
            case "bee" -> t("tooltip.simplybows.detail.bee.frame", frame, num(beeDamage(upgrades)));
            case "blossom" -> t("tooltip.simplybows.detail.blossom.frame", frame, num(blossomDamage(upgrades)));
            case "earth" -> t("tooltip.simplybows.detail.earth.frame", frame, num(earthDamage(upgrades)), num(earthKnockup(upgrades)));
            default -> t("tooltip.simplybows.detail.generic.frame", frame);
        };
    }

    public static List<String> runeLines(String bowKey, BowUpgradeData upgrades) {
        RuneEtching rune = upgrades.runeEtching();
        if (rune == RuneEtching.NONE) {
            return List.of();
        }
        String stat = runeStat(bowKey, upgrades);
        return stat == null || stat.isBlank() ? List.of() : List.of(stat);
    }

    public static void appendComponentTooltip(List<Text> tooltip, BowUpgradeComponentItem.UpgradeKind kind, RuneEtching rune) {
        tooltip.add(Text.literal(" "));
        tooltip.add(Text.translatable(componentIntroKey(kind, rune)).setStyle(BowTooltipHelper.STYLE_BODY));
        tooltip.add(Text.translatable("tooltip.simplybows.upgrade_component.depends").setStyle(BowTooltipHelper.STYLE_SECTION));
        if (kind == BowUpgradeComponentItem.UpgradeKind.RUNE_ETCHING) {
            BowUpgradeData sample = new BowUpgradeData(0, 0, rune);
            for (String bowKey : BOW_KEYS) {
                String effect = runeStat(bowKey, sample);
                if (effect == null || effect.isBlank()) {
                    continue;
                }
                tooltip.add(Text.translatable("tooltip.simplybows.rune_for_bow", Text.translatable(bowItemKey(bowKey)), effect)
                        .setStyle(BowTooltipHelper.STYLE_DIM));
            }
        } else if (kind == BowUpgradeComponentItem.UpgradeKind.ENCHANTED_STRING) {
            for (Text line : stringComponentLines()) {
                tooltip.add(line.copy().setStyle(BowTooltipHelper.STYLE_DIM));
            }
        } else {
            for (Text line : frameComponentLines()) {
                tooltip.add(line.copy().setStyle(BowTooltipHelper.STYLE_DIM));
            }
        }
        tooltip.add(Text.translatable(
                "tooltip.simplybows.upgrade_component.cap",
                BowUpgradeData.getMaxTotalUpgradeSlots()
        ).setStyle(BowTooltipHelper.STYLE_HINT));
    }

    private static String vineString(BowUpgradeData upgrades, int string) {
        if (upgrades.runeEtching() == RuneEtching.CHAOS) {
            var cfg = SimplyBowsConfig.INSTANCE.everbloom;
            double radius = cfg.chaosBaseRadius.get() + string * cfg.chaosRadiusPerString.get();
            return t("tooltip.simplybows.detail.vine.string.chaos", string, num(radius));
        }
        double radius = VineFlowerFieldManager.flowerPatchRadius(string);
        int speed = (int) Math.round(string * 5.0);
        return t("tooltip.simplybows.detail.vine.string", string, num(radius), speed);
    }

    private static String vineFrame(BowUpgradeData upgrades, int frame) {
        if (upgrades.runeEtching() == RuneEtching.CHAOS) {
            var cfg = SimplyBowsConfig.INSTANCE.everbloom;
            int ticks = cfg.chaosBaseDurationTicks.get() + frame * cfg.chaosDurationPerFrameTicks.get();
            return t("tooltip.simplybows.detail.vine.frame.chaos", frame, seconds(ticks));
        }
        double arrow = SimplyBowsConfig.INSTANCE.everbloom.baseDamage.get() * upgrades.damageMultiplier();
        double field = SimplyBowsConfig.INSTANCE.everbloom.hostileDamage.get() * upgrades.damageMultiplier();
        if (upgrades.runeEtching() == RuneEtching.PAIN) {
            return t("tooltip.simplybows.detail.vine.frame.pain", frame, num(arrow), num(field));
        }
        if (upgrades.runeEtching() == RuneEtching.GRACE) {
            return t("tooltip.simplybows.detail.vine.frame.grace", frame, hearts(vineHealPerSecond(upgrades)), hearts(2.0 + frame));
        }
        return t("tooltip.simplybows.detail.vine.frame", frame, hearts(vineHealPerSecond(upgrades)), hearts(2.0 + frame), num(arrow), num(field));
    }

    private static double vineHealPerSecond(BowUpgradeData upgrades) {
        return SimplyBowsConfig.INSTANCE.everbloom.friendlyHeal.get() * (1.0F + upgrades.frameLevel() * 0.6F) * 0.5F;
    }

    private static String iceString(BowUpgradeData upgrades, int string) {
        int arrows = SimplyBowsConfig.INSTANCE.winterfang.baseQuantity.get() + string;
        if (upgrades.runeEtching() == RuneEtching.BOUNTY) {
            arrows *= SimplyBowsConfig.INSTANCE.winterfang.bountyExtraArrowMultiplier.get();
        }
        return t("tooltip.simplybows.detail.ice.string", string, arrows);
    }

    private static double iceDamage(BowUpgradeData upgrades) {
        double damage = SimplyBowsConfig.INSTANCE.winterfang.baseDamage.get() * upgrades.damageMultiplier();
        if (upgrades.runeEtching() == RuneEtching.PAIN) {
            damage *= SimplyBowsConfig.INSTANCE.winterfang.painDamageMultiplier.get();
        } else if (upgrades.runeEtching() == RuneEtching.BOUNTY) {
            damage *= SimplyBowsConfig.INSTANCE.winterfang.bountyDamageMultiplier.get();
        }
        return damage;
    }

    private static String bubbleString(BowUpgradeData upgrades, int string) {
        if (upgrades.runeEtching() == RuneEtching.CHAOS) {
            var cfg = SimplyBowsConfig.INSTANCE.bubbleveil;
            int steps = cfg.chaosBaseLengthSteps.get() + string * cfg.chaosLengthStepsPerString.get();
            return t("tooltip.simplybows.detail.bubble.string.chaos", string, steps);
        }
        if (upgrades.runeEtching() == RuneEtching.PAIN) {
            return t("tooltip.simplybows.detail.bubble.string.pain", string, Math.max(1, string + 1));
        }
        var cfg = SimplyBowsConfig.INSTANCE.bubbleveil;
        int ticks = cfg.columnDurationTicks.get() + string * cfg.columnDurationBonusPerString.get();
        return t("tooltip.simplybows.detail.bubble.string", string, seconds(ticks));
    }

    private static String bubbleFrame(BowUpgradeData upgrades, int frame) {
        if (upgrades.runeEtching() == RuneEtching.CHAOS) {
            var cfg = SimplyBowsConfig.INSTANCE.bubbleveil;
            float damage = cfg.chaosBaseDamage.get() + frame * cfg.chaosDamagePerFrame.get();
            return t("tooltip.simplybows.detail.bubble.frame.chaos", frame, num(damage));
        }
        var cfg = SimplyBowsConfig.INSTANCE.bubbleveil;
        double radius = cfg.columnBaseRadius.get() * upgrades.sizeMultiplier() + frame * cfg.columnRadiusPerFrame.get();
        double height = cfg.columnBaseHeight.get() * upgrades.sizeMultiplier() + frame * cfg.columnHeightPerFrame.get();
        return t("tooltip.simplybows.detail.bubble.frame", frame, num(radius), num(height));
    }

    private static String beeString(BowUpgradeData upgrades, int string) {
        int amplifier = Math.min(4, string / 2);
        int ticks = SimplyBowsConfig.INSTANCE.buzzkill.basePoisonDuration.get() + string * SimplyBowsConfig.INSTANCE.buzzkill.stringPoisonDurationBonus.get();
        if (upgrades.runeEtching() == RuneEtching.PAIN) {
            return t("tooltip.simplybows.detail.bee.string.pain", string, Math.max(1, string + 1), amplifier + 1, seconds(ticks));
        }
        return t("tooltip.simplybows.detail.bee.string", string, amplifier + 1, seconds(ticks));
    }

    private static double beeDamage(BowUpgradeData upgrades) {
        return SimplyBowsConfig.INSTANCE.buzzkill.baseDamage.get() * upgrades.damageMultiplier();
    }

    private static int blossomDuration(BowUpgradeData upgrades) {
        var cfg = SimplyBowsConfig.INSTANCE.petalwind;
        return cfg.stormDurationTicks.get() + upgrades.stringLevel() * cfg.stormDurationBonusPerString.get();
    }

    private static double blossomDamage(BowUpgradeData upgrades) {
        return SimplyBowsConfig.INSTANCE.petalwind.stormDamage.get() * upgrades.damageMultiplier();
    }

    private static double earthRadius(BowUpgradeData upgrades) {
        var cfg = SimplyBowsConfig.INSTANCE.tremorstrike;
        return cfg.fieldRadius.get() * upgrades.sizeMultiplier() + upgrades.stringLevel() * cfg.stringRadiusBonusPerLevel.get();
    }

    private static double earthDamage(BowUpgradeData upgrades) {
        return SimplyBowsConfig.INSTANCE.tremorstrike.spikeDamage.get() * upgrades.damageMultiplier();
    }

    private static double earthKnockup(BowUpgradeData upgrades) {
        var cfg = SimplyBowsConfig.INSTANCE.tremorstrike;
        return cfg.baseUpwardKnockback.get() + upgrades.frameLevel() * cfg.frameUpwardKnockbackPerLevel.get();
    }

    private static String runeStat(String bowKey, BowUpgradeData upgrades) {
        RuneEtching rune = upgrades.runeEtching();
        int string = upgrades.stringLevel();
        int frame = upgrades.frameLevel();
        return switch (bowKey) {
            case "vine" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.rune_stat.vine.pain", seconds(SimplyBowsConfig.INSTANCE.everbloom.painAuraInterval.get()));
                case GRACE -> t("tooltip.simplybows.rune_stat.vine.grace", seconds(SimplyBowsConfig.INSTANCE.everbloom.fieldDurationTicks.get()));
                case BOUNTY -> t("tooltip.simplybows.rune_stat.vine.bounty",
                        num(SimplyBowsConfig.INSTANCE.everbloom.bountyDamageMultiplier.get()),
                        seconds(SimplyBowsConfig.INSTANCE.everbloom.bountyAuraInterval.get()));
                case CHAOS -> t("tooltip.simplybows.rune_stat.vine.chaos",
                        num(SimplyBowsConfig.INSTANCE.everbloom.chaosBaseRadius.get() + string * SimplyBowsConfig.INSTANCE.everbloom.chaosRadiusPerString.get()),
                        seconds(SimplyBowsConfig.INSTANCE.everbloom.chaosBaseDurationTicks.get() + frame * SimplyBowsConfig.INSTANCE.everbloom.chaosDurationPerFrameTicks.get()));
                default -> null;
            };
            case "ice" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.rune_stat.ice.pain", num(SimplyBowsConfig.INSTANCE.winterfang.painDamageMultiplier.get()));
                case GRACE -> t("tooltip.simplybows.rune_stat.ice.grace", seconds(SimplyBowsConfig.INSTANCE.winterfang.graceSlownessDuration.get()), SimplyBowsConfig.INSTANCE.winterfang.graceMaxSlownessStacks.get());
                case BOUNTY -> t("tooltip.simplybows.rune_stat.ice.bounty", SimplyBowsConfig.INSTANCE.winterfang.bountyExtraArrowMultiplier.get(), num(SimplyBowsConfig.INSTANCE.winterfang.bountyDamageMultiplier.get()));
                case CHAOS -> t("tooltip.simplybows.rune_stat.ice.chaos",
                        SimplyBowsConfig.INSTANCE.winterfang.chaosWallWidth.get() + string * SimplyBowsConfig.INSTANCE.winterfang.chaosWallWidthPerString.get(),
                        seconds(SimplyBowsConfig.INSTANCE.winterfang.chaosWallDurationTicks.get() + frame * SimplyBowsConfig.INSTANCE.winterfang.chaosWallDurationPerFrameTicks.get()));
                default -> null;
            };
            case "bubble" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.rune_stat.bubble.pain", Math.max(1, string + 1));
                case GRACE -> t("tooltip.simplybows.rune_stat.bubble.grace", seconds(SimplyBowsConfig.INSTANCE.bubbleveil.graceResistanceDuration.get()));
                case BOUNTY -> t("tooltip.simplybows.rune_stat.bubble.bounty", num(SimplyBowsConfig.INSTANCE.bubbleveil.bountyBaseDamage.get() * upgrades.damageMultiplier()));
                case CHAOS -> t("tooltip.simplybows.rune_stat.bubble.chaos",
                        num(SimplyBowsConfig.INSTANCE.bubbleveil.chaosBaseDamage.get() + frame * SimplyBowsConfig.INSTANCE.bubbleveil.chaosDamagePerFrame.get()),
                        SimplyBowsConfig.INSTANCE.bubbleveil.chaosBaseLengthSteps.get() + string * SimplyBowsConfig.INSTANCE.bubbleveil.chaosLengthStepsPerString.get());
                default -> null;
            };
            case "bee" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.rune_stat.bee.pain", Math.max(1, string + 1));
                case GRACE -> t("tooltip.simplybows.rune_stat.bee.grace",
                        Math.min(SimplyBowsConfig.INSTANCE.buzzkill.graceMaxBeesPerTarget.get(), Math.max(1, string + 1)),
                        seconds(SimplyBowsConfig.INSTANCE.buzzkill.graceBaseDuration.get() + string * SimplyBowsConfig.INSTANCE.buzzkill.graceStringDurationBonus.get()));
                case BOUNTY -> t("tooltip.simplybows.rune_stat.bee.bounty",
                        SimplyBowsConfig.INSTANCE.buzzkill.bountyBaseShots.get() + frame * SimplyBowsConfig.INSTANCE.buzzkill.bountyFrameBonusShots.get(),
                        seconds(SimplyBowsConfig.INSTANCE.buzzkill.bountyHiveDuration.get() + string * SimplyBowsConfig.INSTANCE.buzzkill.bountyHiveDurationBonusPerString.get()));
                case CHAOS -> t("tooltip.simplybows.rune_stat.bee.chaos",
                        seconds(SimplyBowsConfig.INSTANCE.buzzkill.chaosBaseDurationTicks.get() + string * SimplyBowsConfig.INSTANCE.buzzkill.chaosDurationPerStringTicks.get()),
                        num(SimplyBowsConfig.INSTANCE.buzzkill.chaosBaseRadius.get() + string * SimplyBowsConfig.INSTANCE.buzzkill.chaosRadiusPerString.get()),
                        num(SimplyBowsConfig.INSTANCE.buzzkill.chaosDiveDamage.get()));
                default -> null;
            };
            case "blossom" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.rune_stat.blossom.pain", num(SimplyBowsConfig.INSTANCE.petalwind.painAreaRadius.get() * upgrades.sizeMultiplier() + string * SimplyBowsConfig.INSTANCE.petalwind.painAreaRadiusPerString.get()));
                case GRACE -> t("tooltip.simplybows.rune_stat.blossom.grace",
                        seconds(100 + frame * 20),
                        num(3.0 + string * 0.45));
                case BOUNTY -> t("tooltip.simplybows.rune_stat.blossom.bounty",
                        SimplyBowsConfig.INSTANCE.petalwind.bountyMaxStorms.get(),
                        seconds(SimplyBowsConfig.INSTANCE.petalwind.stormDurationTicks.get()
                                + upgrades.stringLevel() * SimplyBowsConfig.INSTANCE.petalwind.stormDurationBonusPerString.get()));
                case CHAOS -> t("tooltip.simplybows.rune_stat.blossom.chaos",
                        SimplyBowsConfig.INSTANCE.petalwind.chaosBaseFishCount.get() + frame * SimplyBowsConfig.INSTANCE.petalwind.chaosFishPerFrame.get(),
                        seconds(SimplyBowsConfig.INSTANCE.petalwind.chaosDurationTicks.get() + frame * SimplyBowsConfig.INSTANCE.petalwind.chaosDurationPerFrameTicks.get()),
                        num(SimplyBowsConfig.INSTANCE.petalwind.chaosContactDamage.get()));
                default -> null;
            };
            case "earth" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.rune_stat.earth.pain", num(SimplyBowsConfig.INSTANCE.tremorstrike.painWaveMaxDistance.get() + string * SimplyBowsConfig.INSTANCE.tremorstrike.painStringWaveDistanceBonusPerLevel.get()));
                case GRACE -> t("tooltip.simplybows.rune_stat.earth.grace", seconds(SimplyBowsConfig.INSTANCE.tremorstrike.graceResistanceDuration.get()), seconds(SimplyBowsConfig.INSTANCE.tremorstrike.graceSlowFallingDuration.get()));
                case BOUNTY -> t("tooltip.simplybows.rune_stat.earth.bounty", num(SimplyBowsConfig.INSTANCE.tremorstrike.bountyCenterDamageBaseMultiplier.get()));
                case CHAOS -> t("tooltip.simplybows.rune_stat.earth.chaos",
                        seconds(SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderDurationTicks.get() + string * SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderDurationPerStringTicks.get()),
                        num(SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderAcquisitionRange.get() + frame * SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderAcquisitionRangePerFrame.get()));
                default -> null;
            };
            default -> null;
        };
    }

    private static List<Text> stringComponentLines() {
        var upgrades = SimplyBowsConfig.INSTANCE.upgrades;
        var bubble = SimplyBowsConfig.INSTANCE.bubbleveil;
        var bee = SimplyBowsConfig.INSTANCE.buzzkill;
        var blossom = SimplyBowsConfig.INSTANCE.petalwind;
        var earth = SimplyBowsConfig.INSTANCE.tremorstrike;
        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable("tooltip.simplybows.per.string.vine", pct(upgrades.sizeMultiplierPerString.get()), 1, 5));
        lines.add(Text.translatable("tooltip.simplybows.per.string.ice"));
        lines.add(Text.translatable("tooltip.simplybows.per.string.bubble", seconds(bubble.columnDurationBonusPerString.get())));
        lines.add(Text.translatable("tooltip.simplybows.per.string.bee", seconds(bee.stringPoisonDurationBonus.get())));
        lines.add(Text.translatable("tooltip.simplybows.per.string.blossom", seconds(blossom.stormDurationBonusPerString.get())));
        lines.add(Text.translatable("tooltip.simplybows.per.string.earth", num(earth.stringRadiusBonusPerLevel.get())));
        return lines;
    }

    private static List<Text> frameComponentLines() {
        var upgrades = SimplyBowsConfig.INSTANCE.upgrades;
        var vine = SimplyBowsConfig.INSTANCE.everbloom;
        var bubble = SimplyBowsConfig.INSTANCE.bubbleveil;
        var earth = SimplyBowsConfig.INSTANCE.tremorstrike;
        double healPerFrame = vine.friendlyHeal.get() * 0.6 * (20.0 / vine.auraIntervalTicks.get());
        List<Text> lines = new ArrayList<>();
        lines.add(Text.translatable("tooltip.simplybows.per.frame.vine", hearts(healPerFrame), hearts(1.0), pct(upgrades.damageMultiplierPerFrame.get())));
        lines.add(Text.translatable("tooltip.simplybows.per.frame.ice", pct(upgrades.damageMultiplierPerFrame.get())));
        lines.add(Text.translatable("tooltip.simplybows.per.frame.bubble", num(bubble.columnRadiusPerFrame.get()), num(bubble.columnHeightPerFrame.get())));
        lines.add(Text.translatable("tooltip.simplybows.per.frame.bee", pct(upgrades.damageMultiplierPerFrame.get())));
        lines.add(Text.translatable("tooltip.simplybows.per.frame.blossom", pct(upgrades.damageMultiplierPerFrame.get())));
        lines.add(Text.translatable("tooltip.simplybows.per.frame.earth", pct(upgrades.damageMultiplierPerFrame.get()), num(earth.frameUpwardKnockbackPerLevel.get())));
        return lines;
    }

    private static String componentIntroKey(BowUpgradeComponentItem.UpgradeKind kind, RuneEtching rune) {
        return switch (kind) {
            case ENCHANTED_STRING -> "tooltip.simplybows.upgrade_component.enchanted_string";
            case REINFORCED_FRAME -> "tooltip.simplybows.upgrade_component.reinforced_frame";
            case RUNE_ETCHING -> "tooltip.simplybows.upgrade_component.rune." + rune.id();
        };
    }

    private static String bowItemKey(String bowKey) {
        return switch (bowKey) {
            case "vine" -> "item.simplybows.vine_bow.vine_bow";
            case "ice" -> "item.simplybows.ice_bow.ice_bow";
            case "bubble" -> "item.simplybows.bubble_bow.bubble_bow";
            case "bee" -> "item.simplybows.bee_bow.bee_bow";
            case "blossom" -> "item.simplybows.blossom_bow.blossom_bow";
            case "earth" -> "item.simplybows.earth_bow.earth_bow";
            default -> "item.simplybows.vine_bow.vine_bow";
        };
    }

    private static String t(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }

    private static String num(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        if (Math.abs(rounded - Math.rint(rounded)) < 0.001) {
            return Integer.toString((int) Math.rint(rounded));
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    private static String hearts(double health) {
        return num(health / 2.0);
    }

    private static String seconds(int ticks) {
        return num(ticks / 20.0);
    }

    private static String seconds(double ticks) {
        return num(ticks / 20.0);
    }

    private static String pct(double fraction) {
        return num(fraction * 100.0);
    }
}
