package net.sweenus.simplybows.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.UUID;
import net.sweenus.simplybows.upgrade.RuneEtching;

public final class WinterfangAbilityRules {
    public static final int FREEZE_TICKS = 60;
    public static int bountyFreezeTicks(int stringLevel) { return FREEZE_TICKS + 10 * Math.max(0, Math.min(5, stringLevel)); }
    public static int bountyCooldownTicks(int frameLevel) { return 360; }
    public static double arrowDamageMultiplier(RuneEtching rune, int frameLevel) {
        return rune == RuneEtching.GRACE ? 0 : rune == RuneEtching.BOUNTY ? 3.0 : 1 + Math.max(0, frameLevel) * 0.18;
    }
    public static float bountyDamagePerPulse(int frameLevel) { return 1.0F + 0.5F * Math.max(0, Math.min(5, frameLevel)); }
    public static final int BOUNTY_FROST_TICKS = 200;
    public static final int FROST_TICKS = 100;
    private WinterfangAbilityRules() {}
    public static int painArrowCount(int stringLevel) { return 1 + Math.max(0, stringLevel); }
    public static final class Targets {
        private final Queue<UUID> queue;
        public Targets(Collection<UUID> orderedTargets) { queue = new ArrayDeque<>(new LinkedHashSet<>(orderedTargets)); }
        public UUID next() { return queue.poll(); }
    }
}
