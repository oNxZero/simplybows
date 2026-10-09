import java.util.List;
import java.util.UUID;
import net.sweenus.simplybows.util.WinterfangAbilityRules;

public class WinterfangRegression {
    public static void main(String[] args) {
        if (WinterfangAbilityRules.FREEZE_TICKS != 3 * 20 || WinterfangAbilityRules.FROST_TICKS != 5 * 20)
            throw new AssertionError("Rune control durations changed");
        for (int level = 0; level <= 20; level++)
            if (WinterfangAbilityRules.painArrowCount(level) != level + 1) throw new AssertionError("Incorrect Pain ammo");
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        var targets = new WinterfangAbilityRules.Targets(List.of(first, first, second, first));
        if (!first.equals(targets.next()) || !second.equals(targets.next()) || targets.next() != null || targets.next() != null)
            throw new AssertionError("Volley reused a target or lost target order");
        var empty = new WinterfangAbilityRules.Targets(List.of());
        if (empty.next() != null) throw new AssertionError("Empty volley invented a target");
        System.out.println("Passed: three-second freeze, five-second Frost, String/ammo counts, distinct targets and target exhaustion.");
    }
}
