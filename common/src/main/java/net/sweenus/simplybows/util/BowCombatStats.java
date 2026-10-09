package net.sweenus.simplybows.util;

import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.world.BeeHiveSwarmManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Combat numbers for a full draw, with the bow's current upgrades and rune. */
public final class BowCombatStats {
    private BowCombatStats() {}

    public static List<String> lines(String bow, BowUpgradeData u, double projectileBonus, double abilityBonus) {
        List<String> out = new ArrayList<>();
        var cfg = SimplyBowsConfig.INSTANCE;
        RuneEtching r = u.runeEtching();
        int s = u.stringLevel(), f = u.frameLevel();
        double power, speed;
        int count = 1;
        switch (bow) {
            case "vine" -> {
                power = cfg.everbloom.baseDamage.get() * u.damageMultiplier() * (r == RuneEtching.BOUNTY ? 0.5 : 1);
                speed = 3 * cfg.everbloom.arrowSpeedMultiplier.get() * (1 + s * 0.05);
            }
            case "ice" -> {
                power = cfg.winterfang.baseDamage.get() * (1 + f * 0.18) * (r == RuneEtching.GRACE ? 0 : 1);
                speed = 3 * cfg.winterfang.arrowSpeed.get();
                count = r == RuneEtching.NONE ? cfg.winterfang.baseQuantity.get() + s : r == RuneEtching.PAIN ? 1 + s : 1;
                if (r == RuneEtching.BOUNTY) speed *= 1 + s * 0.05;
            }
            case "bubble" -> {
                power = cfg.bubbleveil.baseDamage.get() * (1 + f * cfg.upgrades.damageMultiplierPerFrame.get() * 0.5);
                speed = 3 * cfg.bubbleveil.arrowSpeedMultiplier.get();
                if (r == RuneEtching.PAIN) {
                    count = Math.max(1, s + 1);
                    power *= BowAbilityBalance.bubblePainVolleyScale(s) / count;
                    // Pain uses different speeds on land and in water; show both below.
                    speed = 3 * cfg.bubbleveil.painShotSpeedLand.get();
                }
            }
            case "bee" -> {
                power = cfg.buzzkill.baseDamage.get() * u.damageMultiplier() * (r == RuneEtching.BOUNTY ? 0.5 : 1);
                speed = 3 * cfg.buzzkill.arrowSpeedMultiplier.get();
                if (r == RuneEtching.PAIN) {
                    count = Math.max(1, s + 1);
                    power *= Math.max(0.05, 0.20 / count);
                    speed = Math.min(speed, 0.8);
                }
            }
            case "blossom" -> { power = cfg.petalwind.baseDamage.get(); speed = 3 * cfg.petalwind.arrowSpeedMultiplier.get(); }
            case "earth" -> { power = cfg.tremorstrike.baseDamage.get() * u.damageMultiplier(); speed = 3 * cfg.tremorstrike.arrowSpeedMultiplier.get() * (1 + s * 0.05); }
            default -> { return List.of(); }
        }
        out.add("Fully drawn shot • 2 damage = 1 heart");
        if (bow.equals("ice") && r == RuneEtching.GRACE) {
            out.add("Direct hit damage: 0. No harmful arrow effects.");
            out.add("Homing: none. Fully draw to shoot.");
        } else if (bow.equals("bubble") && r == RuneEtching.CHAOS) {
            out.add("Direct hit: water wave replaces the arrow.");
            out.add("Critical hits: none on the wave.");
        } else {
            int hit = (int) Math.ceil(speed * (power + projectileBonus));
            out.add("Arrow damage (about): " + hit + (count > 1 ? " per projectile" : ""));
            if (count > 1) out.add("Projectiles per shot: " + count);
            if (bow.equals("bubble") && r == RuneEtching.PAIN) {
                out.add("Underwater hit damage: about " + (int) Math.ceil(3 * cfg.bubbleveil.painShotSpeedWater.get() * (power + projectileBonus)) + " per axolotl");
            }
            boolean crit = !bow.equals("ice") && !(bow.equals("bee") && r == RuneEtching.BOUNTY);
            out.add(crit ? "Critical hit: adds 0 to " + (hit / 2 + 1) + " damage"
                    : "Critical hits: none for this bow" + (r == RuneEtching.BOUNTY ? " and rune." : "."));
            out.add("Damage shown before armor and enchantments.");
        }
        out.add("On ally hit:");
        if (bow.equals("vine")) {
            out.add("Heals " + num((2 + f) / 2.0) + " hearts. Deals no direct damage to players, animals, or golems.");
        } else if (r == RuneEtching.GRACE) {
            out.add(switch (bow) {
                case "ice" -> "No direct damage. Creates a sanctuary with Resistance I, Speed I, and Regeneration I.";
                case "bubble" -> "No direct damage. Creates a protective column that grants Resistance II.";
                case "bee" -> "No direct damage. Sends protection bees; each sting grants Resistance II for 3s.";
                case "blossom" -> "No direct damage. Creates a puddle that grants Strength II for " + seconds(100 + f * 20) + ".";
                case "earth" -> "No direct damage. Grants Absorption III: 6 extra hearts for 6s, and creates the protective wall.";
                default -> "Supports allies without direct damage.";
            });
        } else {
            out.add("Damages players when friendly-fire rules allow. Does not heal or grant shields.");
        }
        out.add("Ability:");
        switch (bow) {
            case "vine" -> vine(out, u, abilityBonus);
            case "ice" -> ice(out, u, power + projectileBonus, abilityBonus);
            case "bubble" -> bubble(out, u, abilityBonus);
            case "bee" -> bee(out, u, abilityBonus);
            case "blossom" -> blossom(out, u, abilityBonus);
            case "earth" -> earth(out, u, abilityBonus);
        }
        return out;
    }

    private static void vine(List<String> out, BowUpgradeData u, double bonus) {
        var c = SimplyBowsConfig.INSTANCE.everbloom;
        RuneEtching r = u.runeEtching();
        int f = u.frameLevel();
        if (r == RuneEtching.CHAOS) {
            out.add("Drain damage: " + num(Math.min(c.chaosMaxDrainDamage.get(), c.chaosBaseDrainDamage.get()) + bonus)
                    + " at first; increases as the field gains energy.");
            out.add("Drain damage cap: " + num(c.chaosMaxDrainDamage.get() + bonus));
            interval(out, c.chaosDrainIntervalTicks.get());
            duration(out, Math.max(60, c.chaosBaseDurationTicks.get() + f * c.chaosDurationPerFrameTicks.get()));
            out.add("Draining enemies can extend the field's lifetime.");
            out.add("Players in the field: Speed I, refreshed for 3s.");
            return;
        }
        if (r == RuneEtching.NONE || r == RuneEtching.GRACE) {
            double heal = c.friendlyHeal.get() * (1 + f * 0.6) * 0.5;
            out.add("Garden healing: " + num(heal / 2) + " hearts per second.");
            out.add(r == RuneEtching.GRACE ? "Heals every ally in the patch and removes harmful effects."
                    : "Heals the ally with the lowest health in the patch.");
        }
        if (r != RuneEtching.GRACE) {
            boolean bounty = r == RuneEtching.BOUNTY;
            double damage = bounty ? (1 + f * 0.425) * 0.125 : c.hostileDamage.get() * u.damageMultiplier() * (r == RuneEtching.PAIN ? BowAbilityBalance.EVERBLOOM_PAIN_DAMAGE_SCALE : 1);
            double undead = bounty ? (0.3 + f * 0.1) * 0.125 : c.undeadBonusDamage.get() * u.damageMultiplier() * (r == RuneEtching.PAIN ? BowAbilityBalance.EVERBLOOM_PAIN_DAMAGE_SCALE : 1);
            double scaledBonus = bonus * (bounty ? 0.5 : r == RuneEtching.PAIN ? BowAbilityBalance.EVERBLOOM_PAIN_BONUS_SCALE : 1);
            out.add((bounty ? "Damage per tree bolt: " : "Damage per pulse: ") + num(damage + scaledBonus));
            out.add("Against undead: " + num(damage + undead + scaledBonus * (bounty ? 2 : 1)) + " damage");
            interval(out, Math.max(5, r == RuneEtching.PAIN ? c.painAuraInterval.get() : bounty ? c.bountyAuraInterval.get() : c.auraIntervalTicks.get()));
            if (bounty) out.add("Trees: 3, each attacks a different nearby enemy.");
            if (r == RuneEtching.PAIN) out.add("Rose damage bypasses armor. No lingering Wither after leaving.");
        }
        duration(out, c.fieldDurationTicks.get());
    }

    private static void ice(List<String> out, BowUpgradeData u, double power, double bonus) {
        var c = SimplyBowsConfig.INSTANCE.winterfang;
        switch (u.runeEtching()) {
            case PAIN -> {
                out.add("Different targets: up to " + (1 + u.stringLevel()));
                out.add("Splash damage: " + num(power * c.painFrostDamageMultiplier.get() + bonus));
                out.add("Splash radius: " + num(Math.max(0.5, Math.min(3, c.painFrostRadius.get()))) + " blocks.");
                out.add("Each enemy takes at most one splash per volley.");
                out.add("Frost: Slowness III for 5s. No lasting damage field.");
            }
            case BOUNTY -> {
                out.add("Ice prison: 3s. No homing, no repeated damage.");
                out.add("Struck enemy cannot move, attack, or use items while frozen.");
            }
            case GRACE -> {
                out.add("Sanctuary lasts: 7s");
                out.add("Resistance I, Speed I, Regeneration I: refreshed for " + seconds(100 + u.frameLevel() * 20) + " while inside.");
                out.add("Regeneration I heals half a heart every 2.5s.");
                out.add("No damage or harmful effects against enemies.");
            }
            case CHAOS -> {
                out.add("Wall damage: none");
                duration(out, Math.max(20, c.chaosWallDurationTicks.get() + u.frameLevel() * c.chaosWallDurationPerFrameTicks.get()));
                out.add("Blocks movement and projectiles.");
            }
            default -> out.add("Homing frost arrows. No separate damage field.");
        }
    }

    private static void bubble(List<String> out, BowUpgradeData u, double bonus) {
        var c = SimplyBowsConfig.INSTANCE.bubbleveil;
        int duration = c.columnDurationTicks.get() + u.stringLevel() * c.columnDurationBonusPerString.get();
        switch (u.runeEtching()) {
            case BOUNTY -> {
                damage(out, c.bountyBaseDamage.get() * (1 + u.frameLevel() * 0.06) * 0.075 + bonus * 0.3);
                interval(out, c.bountyDamageIntervalTicks.get()); duration(out, duration);
            }
            case GRACE -> {
                duration(out, duration);
                out.add("Resistance II: refreshed for " + seconds(c.graceResistanceDuration.get()) + ".");
                out.add("Slows enemies and blocks incoming shots.");
            }
            case CHAOS -> {
                out.add("Wave hit damage: " + num(c.chaosBaseDamage.get() + u.frameLevel() * c.chaosDamagePerFrame.get() * 0.5 + bonus));
                double travel = net.sweenus.simplybows.world.BubbleChaosWaveManager.waveTravelDistance(u);
                out.add("Wave reach: " + num(c.chaosWaveForwardStartOffset.get() + travel) + " blocks in front of you.");
                out.add("Each enemy can be hit once per wave. Strong knockback.");
            }
            case PAIN -> out.add("Axolotl impacts only. No damage over time or bubble column.");
            default -> { out.add("Column damage: none. Lifts entities in the bubbles."); duration(out, duration); }
        }
    }

    private static void bee(List<String> out, BowUpgradeData u, double bonus) {
        var c = SimplyBowsConfig.INSTANCE.buzzkill;
        int f = u.frameLevel(), s = u.stringLevel();
        switch (u.runeEtching()) {
            case GRACE -> {
                out.add("Protection bees: " + Math.max(1, 1 + s));
                out.add("Allies protected per bee: up to " + Math.max(1, 1 + f));
                out.add("Each sting: Resistance II for 3s.");
            }
            case BOUNTY -> {
                out.add("Hive bees: " + BeeHiveSwarmManager.beeCountFor(u));
                out.add("Damage per explosion: " + num(Math.max(1, c.baseDamage.get() * (1.35 + f * 0.35) * 0.5) + bonus));
                out.add("Starting poison level: " + roman(Math.min(3, 1 + f)) + "; repeated hits strengthen it, up to V.");
                out.add("Poison lasts: " + seconds(c.basePoisonDuration.get() + 20 + f * 15));
                out.add("Hive releases a bee every 0.8s.");
                duration(out, BeeHiveSwarmManager.hiveDurationTicks(u));
            }
            case CHAOS -> {
                out.add("Damage per dive explosion: " + num(c.chaosDiveDamage.get() + bonus));
                interval(out, Math.max(c.chaosMinDiveIntervalTicks.get(), c.chaosBaseDiveIntervalTicks.get() - f * c.chaosDiveIntervalReductionPerFrameTicks.get()));
                duration(out, Math.max(100, Math.min(160, c.chaosBaseDurationTicks.get() + s * Math.min(8, c.chaosDurationPerStringTicks.get()))));
                out.add("Enemies in the honey cloud are slowed.");
            }
            default -> out.add("No separate damage field.");
        }
        if (u.runeEtching() != RuneEtching.GRACE) {
            out.add("Direct hit poison: starts at " + roman(Math.min(5, 1 + s / 2)) + ", lasts " + seconds(c.basePoisonDuration.get() + s * c.stringPoisonDurationBonus.get()));
            int poisonInterval = Math.max(1, 25 >> Math.min(4, s / 2));
            out.add("Starting poison: 1 damage every " + seconds(poisonInterval) + ".");
            out.add("Repeated hits strengthen poison, up to V. Stronger poison ticks faster and cannot kill.");
        }
    }

    private static void blossom(List<String> out, BowUpgradeData u, double bonus) {
        var c = SimplyBowsConfig.INSTANCE.petalwind;
        RuneEtching r = u.runeEtching();
        int f = u.frameLevel(), s = u.stringLevel();
        if (r == RuneEtching.GRACE) {
            duration(out, 60 + s * 10);
            out.add("All allies in the puddle: Strength II for " + seconds(100 + f * 20));
            out.add("Strength II adds 6 melee damage. Puddle damage: none.");
        } else if (r == RuneEtching.CHAOS) {
            out.add("Koi: " + Math.max(1, c.chaosBaseFishCount.get() + f * c.chaosFishPerFrame.get()));
            out.add("Damage per contact: " + num(Math.max(0, c.chaosContactDamage.get()) + bonus));
            out.add("Can damage the same enemy every " + seconds(Math.max(1, c.chaosContactCooldownTicks.get())));
            duration(out, Math.max(40, c.chaosDurationTicks.get() + f * c.chaosDurationPerFrameTicks.get()));
            out.add("Reflects incoming projectiles.");
        } else {
            damage(out, c.stormDamage.get() * u.damageMultiplier() * BowAbilityBalance.petalDamageScale(r, f) + bonus * BowAbilityBalance.petalBonusScale(r, f));
            interval(out, r == RuneEtching.PAIN ? Math.max(8, c.damageIntervalTicks.get() - 2) : c.damageIntervalTicks.get());
            duration(out, c.stormDurationTicks.get() + s * c.stormDurationBonusPerString.get() + (r == RuneEtching.PAIN ? 20 : 0));
            if (r == RuneEtching.BOUNTY) out.add("Separate storms: up to " + c.bountyMaxStorms.get());
        }
    }

    private static void earth(List<String> out, BowUpgradeData u, double bonus) {
        var c = SimplyBowsConfig.INSTANCE.tremorstrike;
        int f = u.frameLevel(), s = u.stringLevel();
        double damage = c.spikeDamage.get() * u.damageMultiplier();
        switch (u.runeEtching()) {
            case GRACE -> {
                out.add("Wall damage: none"); duration(out, 120 + f * 20);
                out.add("Nearby allies: Resistance I for " + seconds(c.graceResistanceDuration.get()) + ", Slow Falling for " + seconds(c.graceSlowFallingDuration.get()));
                out.add("Absorption goes to the struck ally, or to you when no ally is struck.");
            }
            case BOUNTY -> {
                out.add("Spike pulses: 3, every 1.5s.");
                out.add("Damage per pulse: " + num(damage * 0.55 * c.bountyCenterDamageBaseMultiplier.get() + bonus) + " to "
                        + num(damage * 0.55 * (c.bountyCenterDamageBaseMultiplier.get() + c.bountyCenterDamageProximityMultiplier.get()) + bonus));
                out.add("Enemies nearer the center take more damage and knockback.");
            }
            case PAIN -> {
                out.add("Damage per fissure hit: " + num(damage * Math.max(0.75, c.painWaveDamageMultiplier.get()) + bonus));
                out.add("Successful hits knock enemies up and apply Slowness II for 3s.");
            }
            case CHAOS -> {
                out.add("Damage per sweep hit: " + num(c.spikeDamage.get() * 0.52 * (1 + f * SimplyBowsConfig.INSTANCE.upgrades.damageMultiplierPerFrame.get() * 0.5) + bonus));
                out.add("Can damage the same enemy once per second.");
                duration(out, Math.max(200, Math.min(360, Math.max(200, c.chaosSunderDurationTicks.get()) + s * Math.max(20, c.chaosSunderDurationPerStringTicks.get()))));
            }
            default -> out.add("Spike burst damage: " + num(damage + bonus) + ". Knocks enemies upward.");
        }
    }

    private static void damage(List<String> out, double value) { out.add("Damage per pulse: " + num(value)); }
    private static void interval(List<String> out, int ticks) { out.add("Hits every " + seconds(ticks) + "."); }
    private static void duration(List<String> out, int ticks) { out.add("Lasts: " + seconds(ticks)); }
    private static String seconds(int ticks) { return num(ticks / 20.0) + "s"; }
    private static String roman(int level) { return switch (level) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> "V"; }; }
    private static String num(double value) { return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(); }
}
