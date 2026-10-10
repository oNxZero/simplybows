import java.util.List;
import java.util.UUID;
import net.sweenus.simplybows.util.WinterfangAbilityRules;

public class WinterfangRegression {
    public static void main(String[] args) {
        if (WinterfangAbilityRules.FREEZE_TICKS != 3 * 20 || WinterfangAbilityRules.FROST_TICKS != 5 * 20)
            throw new AssertionError("Rune control durations changed");
        for (int level = 0; level <= 20; level++)
            if (WinterfangAbilityRules.painArrowCount(level) != level + 1) throw new AssertionError("Incorrect Pain ammo");
        for (int string = 0; string <= 5; string++) {
            for (int frame = 0; frame <= 5 - string; frame++) {
                if (WinterfangAbilityRules.bountyCooldownTicks(frame) - WinterfangAbilityRules.bountyFreezeTicks(string) < 30)
                    throw new AssertionError("Bounty lost its recovery window");
                if (WinterfangAbilityRules.arrowDamageMultiplier(net.sweenus.simplybows.upgrade.RuneEtching.BOUNTY, frame) != 3.0)
                    throw new AssertionError("Bounty Frame changed damage");
            }
        }
        if (WinterfangAbilityRules.bountyFreezeTicks(-1) != 60 || WinterfangAbilityRules.bountyFreezeTicks(50) != 110
                || WinterfangAbilityRules.bountyCooldownTicks(-1) != 360 || WinterfangAbilityRules.bountyCooldownTicks(50) != 360)
            throw new AssertionError("Unsafe Bounty upgrade bounds");
        if (WinterfangAbilityRules.BOUNTY_FROST_TICKS != 200 || WinterfangAbilityRules.bountyDamagePerPulse(0) != 1
                || WinterfangAbilityRules.bountyDamagePerPulse(5) != 3.5F)
            throw new AssertionError("Bounty Frost or prison damage changed");
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        var targets = new WinterfangAbilityRules.Targets(List.of(first, first, second, first));
        if (!first.equals(targets.next()) || !second.equals(targets.next()) || targets.next() != null || targets.next() != null)
            throw new AssertionError("Volley reused a target or lost target order");
        var empty = new WinterfangAbilityRules.Targets(List.of());
        if (empty.next() != null) throw new AssertionError("Empty volley invented a target");
        System.out.println("Passed: Bounty duration/cooldown bounds and recovery window, fixed Bounty damage, Frost duration, Pain ammo and distinct targets.");
    }
}
