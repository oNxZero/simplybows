package net.sweenus.simplybows.util;

import net.minecraft.text.Text;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import java.util.List;

/** Short player-facing descriptions shared by modern and vanilla tooltips. */
public final class BowTooltipPages {
    public static final List<String> BOWS = List.of("vine", "ice", "bubble", "bee", "blossom", "earth");
    private BowTooltipPages() {}

    public static String ability(String bow) {
        return Text.translatable("tooltip.simplybows.bow." + bow + ".ability").getString();
    }

    public static String rune(String bow, RuneEtching rune) {
        return Text.translatable("tooltip.simplybows.bow." + bow + ".rune." + rune.id()).getString();
    }

    public static String runeIntro(RuneEtching rune) {
        return switch (rune) {
            case GRACE -> "Support your allies. Each bow gains its own healing, protection, or strengthening ability.";
            case PAIN -> "Turn your bow's special ability toward damage and weakening enemies.";
            case BOUNTY -> "Overwhelm enemies with swarms, extra attacks, or repeated strikes.";
            case CHAOS -> "Give your bow an unusual ability, from protective walls to sweeping waves.";
            default -> "Changes a bow's special ability.";
        };
    }

    public static String stringEffect(String bow, BowUpgradeData data) {
        RuneEtching r = data.runeEtching();
        return switch (bow) {
            case "vine" -> r == RuneEtching.NONE ? "Makes the flower patch larger and arrows faster." : "Makes the ability cover a larger area.";
            case "ice" -> switch (r) {
                case NONE -> "Adds another frost arrow to each shot.";
                case CHAOS -> "Makes the frost wall wider.";
                default -> "Makes the frost area larger.";
            };
            case "bubble" -> switch (r) {
                case PAIN -> "Adds another axolotl and +5% of base volley damage, up to +25%.";
                case CHAOS -> "Makes the water wave travel farther.";
                default -> "Makes the bubble column last longer.";
            };
            case "bee" -> switch (r) {
                case PAIN -> "Adds more bees and makes their poison last longer.";
                case GRACE -> "Adds another bee to protect more allies.";
                case BOUNTY -> "Adds more exploding bees to the hive.";
                case CHAOS -> "Makes the honey storm larger.";
                default -> "Makes poison stronger and last longer.";
            };
            case "blossom" -> switch (r) {
                case PAIN -> "Makes the damaging petal ring larger.";
                case GRACE -> "Makes the support puddle larger and last longer.";
                case CHAOS -> "Makes the koi cover a larger area and circle faster.";
                default -> "Makes the petal storms last longer.";
            };
            case "earth" -> switch (r) {
                case PAIN -> "Widens the spike field. The fissure's length stays the same.";
                case GRACE -> "Makes the protective wall cover a larger area.";
                case CHAOS -> "Makes the sweeping spikes wider and last longer.";
                default -> "Makes the spike field larger.";
            };
            default -> "Improves the reach of the ability.";
        };
    }

    public static String frameEffect(String bow, BowUpgradeData data) {
        RuneEtching r = data.runeEtching();
        return switch (bow) {
            case "vine" -> switch (r) {
                case NONE -> "Increases healing and damage to monsters.";
                case GRACE -> "Increases healing.";
                case PAIN -> "Increases the wither roses' damage.";
                case BOUNTY -> "Increases damage from the trees' petal bolts.";
                case CHAOS -> "Makes the spore field last longer.";
            };
            case "ice" -> switch (r) {
                case BOUNTY -> "Adds another frost pulse.";
                case CHAOS -> "Makes the frost wall last longer.";
                case GRACE -> "Increases arrow damage against enemies. Sanctuary buffs stay the same.";
                default -> "Increases arrow damage.";
            };
            case "bubble" -> switch (r) {
                case PAIN -> "Increases axolotl shot damage.";
                case CHAOS -> "Increases the water wave's damage.";
                case BOUNTY -> "Makes the column wider and taller, and slightly increases swarm damage.";
                default -> "Makes the bubble column wider and taller.";
            };
            case "bee" -> switch (r) {
                case GRACE -> "Lets each bee protect another ally before disappearing.";
                case BOUNTY -> "Increases explosion damage and poison strength.";
                default -> "Increases bee damage.";
            };
            case "blossom" -> switch (r) {
                case GRACE -> "Makes the Strength buff last longer.";
                case CHAOS -> "Adds more koi and makes them last longer.";
                default -> "Increases petal damage.";
            };
            case "earth" -> r == RuneEtching.GRACE ? "Makes the protective wall last longer." : "Increases spike damage and knockback.";
            default -> "Improves the ability's strength.";
        };
    }
}
