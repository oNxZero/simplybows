package net.sweenus.simplybows.util;

import net.minecraft.text.Text;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.item.upgrade.BowUpgradeComponentItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.world.BeeHiveSwarmManager;
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

    public static List<String> previewLines(String bow, BowUpgradeData current) {
        List<String> lines = new ArrayList<>();
        for (boolean string : new boolean[]{true, false}) {
            int level = string ? current.stringLevel() : current.frameLevel();
            lines.add((string ? "String" : "Frame") + ": level " + level + ":");
            lines.add((string ? stringLine(bow, current) : frameLine(bow, current)).replaceFirst("^[^:]+: ", "Current: "));
            boolean full = current.stringLevel() + current.frameLevel() >= BowUpgradeData.getMaxTotalUpgradeSlots()
                    || level >= BowUpgradeData.getMaxLevelPerType();
            if (!full) {
                BowUpgradeData next = new BowUpgradeData(current.stringLevel() + (string ? 1 : 0),
                        current.frameLevel() + (string ? 0 : 1), current.runeEtching());
                lines.add("One more: " + (string ? stringGain(bow, current) : frameGain(bow, current)));
                lines.add((string ? stringLine(bow, next) : frameLine(bow, next)).replaceFirst("^[^:]+: ", "After upgrade: "));
            }
        }
        if (current.stringLevel() + current.frameLevel() >= BowUpgradeData.getMaxTotalUpgradeSlots()) {
            lines.add("Upgrade slots: full.");
        }
        return lines;
    }

    public static String stringGain(String bowKey) {
        return stringGain(bowKey, new BowUpgradeData(0, 0, RuneEtching.NONE));
    }

    public static String stringGain(String bowKey, BowUpgradeData upgrades) {
        RuneEtching rune = upgrades != null ? upgrades.runeEtching() : RuneEtching.NONE;
        return switch (bowKey) {
            case "vine" -> rune == RuneEtching.CHAOS
                    ? t("tooltip.simplybows.alt.string.vine.chaos", num(SimplyBowsConfig.INSTANCE.everbloom.chaosRadiusPerString.get()))
                    : t("tooltip.simplybows.alt.string.vine", num(VineFlowerFieldManager.flowerPatchRadiusPerString()));
            case "ice" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.alt.string.ice.pain", num(SimplyBowsConfig.INSTANCE.winterfang.painFrostRadiusPerString.get()));
                case GRACE -> t("tooltip.simplybows.alt.string.ice.grace");
                case BOUNTY -> t("tooltip.simplybows.alt.string.ice.bounty", num(SimplyBowsConfig.INSTANCE.winterfang.bountyFrostRadiusPerString.get()));
                case CHAOS -> t("tooltip.simplybows.alt.string.ice.chaos");
                default -> t("tooltip.simplybows.alt.string.ice");
            };
            case "bubble" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.alt.string.bubble.pain");
                case CHAOS -> t("tooltip.simplybows.alt.string.bubble.chaos");
                default -> t("tooltip.simplybows.alt.string.bubble", seconds(SimplyBowsConfig.INSTANCE.bubbleveil.columnDurationBonusPerString.get()));
            };
            case "bee" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.alt.string.bee.pain");
                case GRACE -> t("tooltip.simplybows.alt.string.bee.grace");
                case BOUNTY -> t("tooltip.simplybows.alt.string.bee.bounty");
                case CHAOS -> t("tooltip.simplybows.alt.string.bee.chaos", num(SimplyBowsConfig.INSTANCE.buzzkill.chaosRadiusPerString.get()));
                default -> t("tooltip.simplybows.alt.string.bee", seconds(SimplyBowsConfig.INSTANCE.buzzkill.stringPoisonDurationBonus.get()));
            };
            case "blossom" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.alt.string.blossom.pain", num(SimplyBowsConfig.INSTANCE.petalwind.painAreaRadiusPerString.get()));
                case GRACE -> t("tooltip.simplybows.alt.string.blossom.grace");
                case CHAOS -> "+" + num(SimplyBowsConfig.INSTANCE.petalwind.chaosRadiusPerString.get()) + " radius, -" + seconds(SimplyBowsConfig.INSTANCE.petalwind.chaosOrbitPeriodReductionPerStringTicks.get()) + "s circle time";
                default -> t("tooltip.simplybows.alt.string.blossom", seconds(SimplyBowsConfig.INSTANCE.petalwind.stormDurationBonusPerString.get()));
            };
            case "earth" -> t("tooltip.simplybows.alt.string.earth", num(SimplyBowsConfig.INSTANCE.tremorstrike.stringRadiusBonusPerLevel.get()));
            default -> t("tooltip.simplybows.alt.string.generic");
        };
    }

    public static String frameGain(String bowKey) {
        return frameGain(bowKey, new BowUpgradeData(0, 0, RuneEtching.NONE));
    }

    public static String frameGain(String bowKey, BowUpgradeData upgrades) {
        RuneEtching rune = upgrades != null ? upgrades.runeEtching() : RuneEtching.NONE;
        var cfg = SimplyBowsConfig.INSTANCE.upgrades;
        return switch (bowKey) {
            case "vine" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.alt.frame.damage", pct(cfg.damageMultiplierPerFrame.get()));
                case BOUNTY -> t("tooltip.simplybows.alt.frame.vine.bounty");
                case CHAOS -> t("tooltip.simplybows.alt.frame.vine.chaos", seconds(SimplyBowsConfig.INSTANCE.everbloom.chaosDurationPerFrameTicks.get()));
                default -> t("tooltip.simplybows.alt.frame.vine", hearts(SimplyBowsConfig.INSTANCE.everbloom.friendlyHeal.get() * 0.6F * 0.5F));
            };
            case "ice" -> switch (rune) {
                case BOUNTY -> t("tooltip.simplybows.alt.frame.ice.bounty");
                case CHAOS -> t("tooltip.simplybows.alt.frame.ice.chaos", seconds(SimplyBowsConfig.INSTANCE.winterfang.chaosWallDurationPerFrameTicks.get()));
                default -> t("tooltip.simplybows.alt.frame.damage", 18);
            };
            case "bee" -> switch (rune) {
                case GRACE -> t("tooltip.simplybows.alt.frame.bee.grace");
                case BOUNTY -> t("tooltip.simplybows.alt.frame.bee.bounty");
                default -> t("tooltip.simplybows.alt.frame.damage", pct(cfg.damageMultiplierPerFrame.get()));
            };
            case "earth" -> rune == RuneEtching.GRACE
                    ? t("tooltip.simplybows.alt.frame.earth.grace")
                    : t("tooltip.simplybows.alt.frame.earth", pct(cfg.damageMultiplierPerFrame.get()));
            case "blossom" -> switch (rune) {
                case GRACE -> t("tooltip.simplybows.alt.frame.blossom.grace");
                case CHAOS -> t("tooltip.simplybows.alt.frame.blossom.chaos");
                default -> t("tooltip.simplybows.alt.frame.damage", pct(cfg.damageMultiplierPerFrame.get()));
            };
            case "bubble" -> rune == RuneEtching.CHAOS
                    ? t("tooltip.simplybows.alt.frame.bubble.chaos", num(SimplyBowsConfig.INSTANCE.bubbleveil.chaosDamagePerFrame.get() * 0.5))
                    : t("tooltip.simplybows.alt.frame.bubble", num(SimplyBowsConfig.INSTANCE.bubbleveil.columnRadiusPerFrame.get()));
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
            case "blossom" -> blossomString(upgrades);
            case "earth" -> earthString(upgrades, string);
            default -> t("tooltip.simplybows.detail.generic.string", string);
        };
    }

    public static String frameLine(String bowKey, BowUpgradeData upgrades) {
        int frame = upgrades.frameLevel();
        return switch (bowKey) {
            case "vine" -> vineFrame(upgrades, frame);
            case "ice" -> iceFrame(upgrades, frame);
            case "bubble" -> bubbleFrame(upgrades, frame);
            case "bee" -> beeFrame(upgrades, frame);
            case "blossom" -> blossomFrame(upgrades);
            case "earth" -> earthFrame(upgrades, frame);
            default -> t("tooltip.simplybows.detail.generic.frame", frame);
        };
    }

    public static List<String> runeLines(String bowKey, BowUpgradeData upgrades) {
        RuneEtching rune = upgrades.runeEtching();
        if (rune == RuneEtching.NONE) {
            return List.of();
        }
        String stat = runeStat(bowKey, upgrades);
        List<String> lines = new ArrayList<>();
        if (stat != null && !stat.isBlank()) lines.addAll(List.of(stat.split(" — |, ")));
        var vine = SimplyBowsConfig.INSTANCE.everbloom;
        var bubble = SimplyBowsConfig.INSTANCE.bubbleveil;
        int frame = upgrades.frameLevel();
        if (bowKey.equals("vine") && (rune == RuneEtching.PAIN || rune == RuneEtching.BOUNTY)) {
            boolean pain = rune == RuneEtching.PAIN;
            double damage = pain ? vine.hostileDamage.get() * upgrades.damageMultiplier() * BowAbilityBalance.EVERBLOOM_PAIN_DAMAGE_SCALE
                    : (1.0 + frame * 0.425) * 0.125;
            double undead = pain ? vine.undeadBonusDamage.get() * upgrades.damageMultiplier() * BowAbilityBalance.EVERBLOOM_PAIN_DAMAGE_SCALE
                    : (0.3 + frame * 0.1) * 0.125;
            lines.add(t("tooltip.simplybows.metrics.damage", num(damage), num(damage + undead)));
            lines.add(t("tooltip.simplybows.metrics.interval", pain ? vine.painAuraInterval.get() : vine.bountyAuraInterval.get(),
                    seconds(pain ? vine.painAuraInterval.get() : vine.bountyAuraInterval.get())));
            lines.add(t("tooltip.simplybows.metrics.duration", seconds(vine.fieldDurationTicks.get()), vine.fieldDurationTicks.get()));

        }
        if (bowKey.equals("bubble") && rune == RuneEtching.BOUNTY) {
            lines.add(t("tooltip.simplybows.metrics.pulse", num(bubble.bountyBaseDamage.get() * (1 + frame * 0.06) * 0.075)));
            lines.add(t("tooltip.simplybows.metrics.interval", bubble.bountyDamageIntervalTicks.get(), seconds(bubble.bountyDamageIntervalTicks.get())));
            int duration = bubble.columnDurationTicks.get() + upgrades.stringLevel() * bubble.columnDurationBonusPerString.get();
            lines.add(t("tooltip.simplybows.metrics.duration", seconds(duration), duration));
        }
        if (bowKey.equals("blossom") && (rune == RuneEtching.PAIN || rune == RuneEtching.BOUNTY)) {
            var petal = SimplyBowsConfig.INSTANCE.petalwind;
            int interval = rune == RuneEtching.PAIN ? Math.max(8, petal.damageIntervalTicks.get() - 2) : petal.damageIntervalTicks.get();
            int duration = blossomDuration(upgrades) + (rune == RuneEtching.PAIN ? 20 : 0);
            lines.add(t("tooltip.simplybows.metrics.pulse", num(blossomDamage(upgrades))));
            lines.add(t("tooltip.simplybows.metrics.interval", interval, seconds(interval)));
            lines.add(t("tooltip.simplybows.metrics.duration", seconds(duration), duration));
        }
        if (bowKey.equals("bee")) lines.add(t("tooltip.simplybows.metrics.bee_cooldown"));
        return lines;
    }

    public static void appendComponentTooltip(List<Text> tooltip, BowUpgradeComponentItem.UpgradeKind kind, RuneEtching rune) {
        tooltip.add(Text.literal(kind == BowUpgradeComponentItem.UpgradeKind.RUNE_ETCHING
                ? BowTooltipPages.runeIntro(rune)
                : kind == BowUpgradeComponentItem.UpgradeKind.ENCHANTED_STRING
                ? "Improves the reach, duration, or number of your bow's special attacks."
                : "Improves the strength of your bow's special ability.").setStyle(BowTooltipHelper.STYLE_BODY));
        tooltip.add(Text.literal("Apply in an anvil: bow first, upgrade second.").setStyle(BowTooltipHelper.STYLE_HINT));
        BowUpgradeData sample = new BowUpgradeData(0, 0, rune);
        for (String bow : BOW_KEYS) {
            tooltip.add(Text.translatable(bowItemKey(bow)).setStyle(BowTooltipHelper.STYLE_SECTION));
            String description = kind == BowUpgradeComponentItem.UpgradeKind.RUNE_ETCHING ? BowTooltipPages.rune(bow, rune)
                    : kind == BowUpgradeComponentItem.UpgradeKind.ENCHANTED_STRING ? BowTooltipPages.stringEffect(bow, sample)
                    : BowTooltipPages.frameEffect(bow, sample);
            BowTooltipHelper.addWrappedLine(tooltip, Text.literal(description), BowTooltipHelper.STYLE_BODY);
        }
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
        if (upgrades.runeEtching() == RuneEtching.PAIN) field *= BowAbilityBalance.EVERBLOOM_PAIN_DAMAGE_SCALE;
        if (upgrades.runeEtching() == RuneEtching.PAIN) {
            return t("tooltip.simplybows.detail.vine.frame.pain", frame, num(arrow), num(field));
        }
        if (upgrades.runeEtching() == RuneEtching.BOUNTY) {
            return t("tooltip.simplybows.detail.vine.frame.bounty", frame, num((1.0 + frame * 0.425) * 0.125));
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
        var cfg = SimplyBowsConfig.INSTANCE.winterfang;
        return switch (upgrades.runeEtching()) {
            case PAIN -> t("tooltip.simplybows.detail.ice.string.pain", string,
                    num(cfg.painFrostRadius.get() + string * cfg.painFrostRadiusPerString.get()));
            case GRACE -> t("tooltip.simplybows.detail.ice.string.grace", string, num(3.25 + string * 0.35));
            case BOUNTY -> t("tooltip.simplybows.detail.ice.string.bounty", string,
                    num(cfg.bountyFrostRadius.get() + string * cfg.bountyFrostRadiusPerString.get()));
            case CHAOS -> t("tooltip.simplybows.detail.ice.string.chaos", string,
                    cfg.chaosWallWidth.get() + string * cfg.chaosWallWidthPerString.get());
            default -> t("tooltip.simplybows.detail.ice.string", string, cfg.baseQuantity.get() + string);
        };
    }

    private static String iceFrame(BowUpgradeData upgrades, int frame) {
        var cfg = SimplyBowsConfig.INSTANCE.winterfang;
        return switch (upgrades.runeEtching()) {
            case BOUNTY -> t("tooltip.simplybows.detail.ice.frame.bounty", frame,
                    Math.max(2, cfg.bountyFrostPulseCount.get() + frame));
            case CHAOS -> t("tooltip.simplybows.detail.ice.frame.chaos", frame,
                    seconds(cfg.chaosWallDurationTicks.get() + frame * cfg.chaosWallDurationPerFrameTicks.get()));
            default -> t("tooltip.simplybows.detail.ice.frame", frame, num(iceDamage(upgrades)));
        };
    }

    private static double iceDamage(BowUpgradeData upgrades) {
        return SimplyBowsConfig.INSTANCE.winterfang.baseDamage.get() * (1 + upgrades.frameLevel() * 0.18) * (upgrades.runeEtching() == RuneEtching.GRACE ? 0.35 : 1);
    }

    private static String bubbleString(BowUpgradeData upgrades, int string) {
        if (upgrades.runeEtching() == RuneEtching.CHAOS) {
            var cfg = SimplyBowsConfig.INSTANCE.bubbleveil;
            double range = net.sweenus.simplybows.world.BubbleChaosWaveManager.waveTravelDistance(upgrades);
            return t("tooltip.simplybows.detail.bubble.string.chaos", string, num(range));
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
            float damage = cfg.chaosBaseDamage.get() + frame * cfg.chaosDamagePerFrame.get() * 0.5F;
            return t("tooltip.simplybows.detail.bubble.frame.chaos", frame, num(damage));
        }
        var cfg = SimplyBowsConfig.INSTANCE.bubbleveil;
        double radius = cfg.columnBaseRadius.get() * upgrades.sizeMultiplier() + frame * cfg.columnRadiusPerFrame.get();
        double height = cfg.columnBaseHeight.get() * upgrades.sizeMultiplier() + frame * cfg.columnHeightPerFrame.get();
        return t("tooltip.simplybows.detail.bubble.frame", frame, num(radius), num(height));
    }

    private static String beeString(BowUpgradeData upgrades, int string) {
        var cfg = SimplyBowsConfig.INSTANCE.buzzkill;
        int amplifier = Math.min(4, string / 2);
        int ticks = cfg.basePoisonDuration.get() + string * cfg.stringPoisonDurationBonus.get();
        return switch (upgrades.runeEtching()) {
            case PAIN -> t("tooltip.simplybows.detail.bee.string.pain", string, Math.max(1, string + 1), amplifier + 1, seconds(ticks));
            case GRACE -> t("tooltip.simplybows.detail.bee.string.grace", string, Math.max(1, 1 + string));
            case BOUNTY -> t("tooltip.simplybows.detail.bee.string.bounty", string,
                    BeeHiveSwarmManager.beeCountFor(upgrades),
                    seconds(BeeHiveSwarmManager.hiveDurationTicks(upgrades)));
            case CHAOS -> t("tooltip.simplybows.detail.bee.string.chaos", string,
                    num(cfg.chaosBaseRadius.get() + string * cfg.chaosRadiusPerString.get()));
            default -> t("tooltip.simplybows.detail.bee.string", string, amplifier + 1, seconds(ticks));
        };
    }

    private static String beeFrame(BowUpgradeData upgrades, int frame) {
        if (upgrades.runeEtching() == RuneEtching.GRACE) {
            return t("tooltip.simplybows.detail.bee.frame.grace", frame, Math.max(1, 1 + frame));
        }
        if (upgrades.runeEtching() == RuneEtching.BOUNTY) {
            float dmg = (float) (SimplyBowsConfig.INSTANCE.buzzkill.baseDamage.get() * (1.35 + frame * 0.35) * 0.5);
            return t("tooltip.simplybows.detail.bee.frame.bounty", frame, num(dmg), Math.min(3, 1 + Math.min(2, frame)));
        }
        return t("tooltip.simplybows.detail.bee.frame", frame, num(beeDamage(upgrades)));
    }

    private static String earthString(BowUpgradeData upgrades, int string) {
        if (upgrades.runeEtching() == RuneEtching.GRACE) {
            return t("tooltip.simplybows.detail.earth.string.grace", string, num(2.75 + string * 0.28));
        }
        return t("tooltip.simplybows.detail.earth.string", string, num(earthRadius(upgrades)));
    }

    private static String earthFrame(BowUpgradeData upgrades, int frame) {
        if (upgrades.runeEtching() == RuneEtching.GRACE) {
            return t("tooltip.simplybows.detail.earth.frame.grace", frame, seconds(120 + frame * 20));
        }
        return t("tooltip.simplybows.detail.earth.frame", frame, num(earthDamage(upgrades)), num(earthKnockup(upgrades)));
    }

    private static double beeDamage(BowUpgradeData upgrades) {
        return SimplyBowsConfig.INSTANCE.buzzkill.baseDamage.get() * upgrades.damageMultiplier();
    }

    private static int blossomDuration(BowUpgradeData upgrades) {
        var cfg = SimplyBowsConfig.INSTANCE.petalwind;
        return cfg.stormDurationTicks.get() + upgrades.stringLevel() * cfg.stormDurationBonusPerString.get();
    }

    private static String blossomString(BowUpgradeData u) {
        var c = SimplyBowsConfig.INSTANCE.petalwind;
        int s = u.stringLevel();
        return "String " + s + ": " + switch (u.runeEtching()) {
            case PAIN -> "radius " + num(Math.min(4.5, Math.max(3.0, c.painAreaRadius.get() * 0.55)) + s * Math.min(0.45, c.painAreaRadiusPerString.get())) + " blocks, duration " + seconds(blossomDuration(u) + 20) + "s";
            case GRACE -> "radius " + num(3 + s * 0.45 + (c.graceAuraDamageRadius.get() > 0 ? Math.min(1.5, c.graceAuraRadiusPerString.get() * s * 0.25) : 0)) + " blocks, duration " + seconds(60 + s * 10) + "s";
            case CHAOS -> "radius " + num(Math.max(1.5, c.chaosRadius.get() + s * c.chaosRadiusPerString.get())) + " blocks, circle time " + seconds(Math.max(c.chaosMinOrbitPeriodTicks.get(), c.chaosBaseOrbitPeriodTicks.get() - s * c.chaosOrbitPeriodReductionPerStringTicks.get())) + "s";
            default -> "duration " + seconds(blossomDuration(u)) + "s";
        };
    }

    private static String blossomFrame(BowUpgradeData u) {
        var c = SimplyBowsConfig.INSTANCE.petalwind;
        int f = u.frameLevel();
        return "Frame " + f + ": " + switch (u.runeEtching()) {
            case GRACE -> "Strength duration " + seconds(100 + f * 20) + "s";
            case CHAOS -> Math.max(1, c.chaosBaseFishCount.get() + f * c.chaosFishPerFrame.get()) + " koi, duration " + seconds(Math.max(40, c.chaosDurationTicks.get() + f * c.chaosDurationPerFrameTicks.get())) + "s";
            default -> "damage per hit " + num(blossomDamage(u));
        };
    }

    private static double blossomDamage(BowUpgradeData upgrades) {
        return SimplyBowsConfig.INSTANCE.petalwind.stormDamage.get() * upgrades.damageMultiplier()
                * BowAbilityBalance.petalDamageScale(upgrades.runeEtching(), upgrades.frameLevel());
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
                        num((1.0 + frame * 0.425) * 0.125),
                        seconds(SimplyBowsConfig.INSTANCE.everbloom.bountyAuraInterval.get()));
                case CHAOS -> t("tooltip.simplybows.rune_stat.vine.chaos",
                        num(SimplyBowsConfig.INSTANCE.everbloom.chaosBaseRadius.get() + string * SimplyBowsConfig.INSTANCE.everbloom.chaosRadiusPerString.get()),
                        seconds(SimplyBowsConfig.INSTANCE.everbloom.chaosBaseDurationTicks.get() + frame * SimplyBowsConfig.INSTANCE.everbloom.chaosDurationPerFrameTicks.get()));
                default -> null;
            };
            case "ice" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.rune_stat.ice.pain",
                        num(SimplyBowsConfig.INSTANCE.winterfang.painFrostRadius.get() + string * SimplyBowsConfig.INSTANCE.winterfang.painFrostRadiusPerString.get()));
                case GRACE -> t("tooltip.simplybows.rune_stat.ice.grace", seconds(120), seconds(100));
                case BOUNTY -> t("tooltip.simplybows.rune_stat.ice.bounty",
                        Math.max(2, SimplyBowsConfig.INSTANCE.winterfang.bountyFrostPulseCount.get() + frame),
                        num(SimplyBowsConfig.INSTANCE.winterfang.bountyFrostRadius.get() + string * SimplyBowsConfig.INSTANCE.winterfang.bountyFrostRadiusPerString.get()));
                case CHAOS -> t("tooltip.simplybows.rune_stat.ice.chaos",
                        SimplyBowsConfig.INSTANCE.winterfang.chaosWallWidth.get() + string * SimplyBowsConfig.INSTANCE.winterfang.chaosWallWidthPerString.get(),
                        seconds(SimplyBowsConfig.INSTANCE.winterfang.chaosWallDurationTicks.get() + frame * SimplyBowsConfig.INSTANCE.winterfang.chaosWallDurationPerFrameTicks.get()));
                default -> null;
            };
            case "bubble" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.rune_stat.bubble.pain", Math.max(1, string + 1));
                case GRACE -> t("tooltip.simplybows.rune_stat.bubble.grace", seconds(SimplyBowsConfig.INSTANCE.bubbleveil.graceResistanceDuration.get()));
                case BOUNTY -> t("tooltip.simplybows.rune_stat.bubble.bounty", num(SimplyBowsConfig.INSTANCE.bubbleveil.bountyBaseDamage.get() * (1.0 + frame * 0.06) * 0.075));
                case CHAOS -> t("tooltip.simplybows.rune_stat.bubble.chaos",
                        num(SimplyBowsConfig.INSTANCE.bubbleveil.chaosBaseDamage.get() + frame * SimplyBowsConfig.INSTANCE.bubbleveil.chaosDamagePerFrame.get() * 0.5),
                        num(net.sweenus.simplybows.world.BubbleChaosWaveManager.waveTravelDistance(upgrades)));
                default -> null;
            };
            case "bee" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.rune_stat.bee.pain", Math.max(1, string + 1));
                case GRACE -> t("tooltip.simplybows.rune_stat.bee.grace",
                        Math.max(1, 1 + string),
                        Math.max(1, 1 + frame));
                case BOUNTY -> t("tooltip.simplybows.rune_stat.bee.bounty",
                        BeeHiveSwarmManager.beeCountFor(upgrades),
                        num(SimplyBowsConfig.INSTANCE.buzzkill.baseDamage.get() * (1.35 + frame * 0.35) * 0.5),
                        Math.min(3, 1 + Math.min(2, frame)));
                case CHAOS -> t("tooltip.simplybows.rune_stat.bee.chaos",
                        seconds(Math.max(100, SimplyBowsConfig.INSTANCE.buzzkill.chaosBaseDurationTicks.get())),
                        num(SimplyBowsConfig.INSTANCE.buzzkill.chaosBaseRadius.get() + string * SimplyBowsConfig.INSTANCE.buzzkill.chaosRadiusPerString.get()),
                        num(SimplyBowsConfig.INSTANCE.buzzkill.chaosDiveDamage.get()));
                default -> null;
            };
            case "blossom" -> switch (rune) {
                case PAIN -> t("tooltip.simplybows.rune_stat.blossom.pain", num(Math.min(4.5, Math.max(3.0, SimplyBowsConfig.INSTANCE.petalwind.painAreaRadius.get() * 0.55)) + string * Math.min(0.45, SimplyBowsConfig.INSTANCE.petalwind.painAreaRadiusPerString.get())));
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
                case PAIN -> t("tooltip.simplybows.rune_stat.earth.pain");
                case GRACE -> t("tooltip.simplybows.rune_stat.earth.grace",
                        num(2.75 + string * 0.28),
                        seconds(120 + frame * 20));
                case BOUNTY -> t("tooltip.simplybows.rune_stat.earth.bounty");
                case CHAOS -> t("tooltip.simplybows.rune_stat.earth.chaos",
                        seconds(SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderDurationTicks.get() + string * SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderDurationPerStringTicks.get()));
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
        return java.math.BigDecimal.valueOf(value).setScale(1, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
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
