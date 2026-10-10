import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.BowAbilityBalance;

public class BalanceRegression {
    public static void main(String[] args) {
        for (double base : new double[]{0.1, 1.5, 5.625}) {
            for (double bonus : new double[]{0, 1, 10}) {
                close(base * BowAbilityBalance.EVERBLOOM_PAIN_DAMAGE_SCALE
                        + bonus * BowAbilityBalance.EVERBLOOM_PAIN_BONUS_SCALE, (base * 0.3 + bonus) * 0.5);
                close(base * BowAbilityBalance.petalDamageScale(RuneEtching.NONE, 5)
                        + bonus * BowAbilityBalance.petalBonusScale(RuneEtching.NONE, 5), (base + bonus) * 0.5);
                close(base * BowAbilityBalance.petalDamageScale(RuneEtching.BOUNTY, 5)
                        + bonus * BowAbilityBalance.petalBonusScale(RuneEtching.BOUNTY, 5), (base * 0.7 + bonus) * 0.7);
                close(base * BowAbilityBalance.petalDamageScale(RuneEtching.BOUNTY, 0)
                        + bonus * BowAbilityBalance.petalBonusScale(RuneEtching.BOUNTY, 0), base * 0.7 + bonus);
            }
        }
        close(BowAbilityBalance.petalDamageScale(RuneEtching.PAIN, 5), 0.35*0.4);
        close(BowAbilityBalance.petalBonusScale(RuneEtching.PAIN, 5), 0.4);
        for(int frame=0;frame<=5;frame++) for(double ordinary:new double[]{1,3,6})
            close(BowAbilityBalance.petalFrameMultiplier(RuneEtching.PAIN,frame,ordinary),1+frame*.1);
        close(BowAbilityBalance.waveKnockbackScale(0), 1);
        close(BowAbilityBalance.waveKnockbackScale(5), 0.8);
        // Configured step distances may not divide four evenly. The endpoint must still add exactly four blocks.
        for (double distance : new double[]{0.1, 0.3, 0.8, 1.1, 2.0}) {
            for (int string = 0; string <= 5; string++) {
                double oldRange = (7 + string * 2) * distance;
                double newRange = BowAbilityBalance.waveTravelDistance(7, 2, string, distance);
                close(newRange, oldRange + 4);
                int steps = BowAbilityBalance.waveStepCount(7, 2, string, distance);
                close(Math.min(steps * distance, newRange), oldRange + 4);
                if ((steps - 1) * distance >= newRange) throw new AssertionError("unnecessary extra wave step");
            }
        }
        close(BowAbilityBalance.bubblePainVolleyScale(0) / 0.28, 1.25);
        close(BowAbilityBalance.bubblePainVolleyScale(5), 0.4375);
        close(BowAbilityBalance.bubblePainVolleyScale(20), 0.4375);
        close(BowAbilityBalance.bubblePainVolleyScale(-1), 0.35);
        for (int string = 0; string <= 5; string++) {
            int frame = 5 - string;
            double budget = BowAbilityBalance.bubblePainVolleyScale(string) * (1 + frame * 0.55 * 0.5);
            if (budget > 0.831251) throw new AssertionError("Pain exceeds bounded five-slot volley budget");
        }
        System.out.println("Passed: requested damage ratios including external bonuses, unchanged Bounty Frame 0, reduced Petalwind Pain and +10% per Frame, Frame 5 knockback, and exact +4-block wave endpoints across configs.");
    }
    private static void close(double actual, double expected) {
        if (Math.abs(actual - expected) > 0.000001) throw new AssertionError(actual + " != " + expected);
    }
}
