package net.sweenus.simplybows.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class CelestialSwiftnessTracker {

    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();

    private CelestialSwiftnessTracker() {
    }

    public static void set(UUID playerId, int stacks, long expiryTick) {
        if (playerId == null || stacks <= 0) {
            return;
        }
        ACTIVE.put(playerId, new Entry(stacks, expiryTick));
    }

    public static int getStacks(UUID playerId, long now) {
        if (playerId == null) {
            return 0;
        }
        Entry entry = ACTIVE.get(playerId);
        if (entry == null) {
            return 0;
        }
        if (entry.expiryTick <= now) {
            ACTIVE.remove(playerId);
            return 0;
        }
        return entry.stacks;
    }

    public static void clearAll() {
        ACTIVE.clear();
    }


    private record Entry(int stacks, long expiryTick) {
    }
}
