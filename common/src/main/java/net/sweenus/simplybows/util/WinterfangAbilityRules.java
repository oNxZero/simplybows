package net.sweenus.simplybows.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.UUID;

public final class WinterfangAbilityRules {
    public static final int FREEZE_TICKS = 60;
    public static final int FROST_TICKS = 100;
    private WinterfangAbilityRules() {}
    public static int painArrowCount(int stringLevel) { return 1 + Math.max(0, stringLevel); }
    public static final class Targets {
        private final Queue<UUID> queue;
        public Targets(Collection<UUID> orderedTargets) { queue = new ArrayDeque<>(new LinkedHashSet<>(orderedTargets)); }
        public UUID next() { return queue.poll(); }
    }
}
