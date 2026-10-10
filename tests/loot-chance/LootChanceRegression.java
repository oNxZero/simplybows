import net.sweenus.simplybows.loot.LootChance;

public class LootChanceRegression {
    public static void main(String[] args) {
        float[][] cases = {{-1,0},{0,0},{0.05F,0.00005F},{0.5F,0.0005F},
                {1,0.001F},{1.01F,0.00101F},{3,0.003F},{20,0.02F},
                {30,0.03F},{50,0.05F},{1000,1},{2000,1},{Float.NaN,0},{Float.POSITIVE_INFINITY,0}};
        for (float[] c : cases) {
            float actual = LootChance.toProbability(c[0]);
            if (!Float.isFinite(actual) || Math.abs(actual - c[1]) > 0.0000001F)
                throw new AssertionError(c[0] + " became " + actual + ", expected " + c[1]);
        }
        System.out.println("Passed: uniform loot scale, fractional chances, old boundary at 1, defaults, clamps, and non-finite inputs.");
    }
}
