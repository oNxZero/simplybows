package net.sweenus.simplybows.loot;

/** Uniform per-thousand config scale: 1 = 0.1%, 1000 = 100%. */
public final class LootChance {
    private LootChance() {}

    public static float toProbability(float configuredValue) {
        if (!Float.isFinite(configuredValue)) return 0.0F;
        return Math.max(0.0F, Math.min(1.0F, configuredValue / 1000.0F));
    }
}
