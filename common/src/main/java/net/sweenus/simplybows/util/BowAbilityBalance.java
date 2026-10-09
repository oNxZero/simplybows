package net.sweenus.simplybows.util;

import net.sweenus.simplybows.upgrade.RuneEtching;

/** Shared ability tuning for combat and tooltip calculations. */
public final class BowAbilityBalance {
    public static final float EVERBLOOM_PAIN_DAMAGE_SCALE = 0.15F;
    public static final float EVERBLOOM_PAIN_BONUS_SCALE = 0.5F;
    private BowAbilityBalance() {}

    /** Modest volley buff; String damage gain is capped at five levels. */
    public static double bubblePainVolleyScale(int string) {
        return 0.35 * (1.0 + 0.05 * Math.max(0, Math.min(5, string)));
    }

    public static float petalDamageScale(RuneEtching rune, int frame) {
        return switch (rune) {
            case NONE -> 0.5F;
            case PAIN -> 0.35F;
            case BOUNTY -> 0.7F * petalBonusScale(rune, frame);
            case GRACE -> 0.0F;
            case CHAOS -> 1.0F;
        };
    }

    public static float petalBonusScale(RuneEtching rune, int frame) {
        return switch (rune) {
            case NONE -> 0.5F;
            case BOUNTY -> 1.0F - 0.06F * Math.max(0, Math.min(5, frame));
            case GRACE -> 0.0F;
            default -> 1.0F;
        };
    }

    public static double waveKnockbackScale(int frame) {
        return 1.0 - 0.04 * Math.max(0, Math.min(5, frame));
    }

    public static int waveStepCount(int baseSteps, int stepsPerString, int string, double stepDistance) {
        // Ignore floating-point noise at an exact step boundary to avoid a duplicate endpoint.
        return (int) Math.ceil(waveTravelDistance(baseSteps, stepsPerString, string, stepDistance) / stepDistance - 1.0E-9);
    }

    public static double waveTravelDistance(int baseSteps, int stepsPerString, int string, double stepDistance) {
        return (baseSteps + Math.max(0, string) * stepsPerString) * stepDistance + 4.0;
    }
}
