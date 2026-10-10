package net.sweenus.simplybows.util;

/** Timing and scaling shared by gameplay, visuals and tooltips. Damage is in health points. */
public final class RuneEffectRules {
    public static final int STONE = 0, STAR = 1, SWARM = 2, VORTEX = 3, LOTUS = 4, WATER_DROP = 5, PETAL_BLADE = 6;
    private RuneEffectRules() {}
    public static int kind(String bow, net.sweenus.simplybows.upgrade.RuneEtching rune) {
        if (bow.equals("earth")) return rune == net.sweenus.simplybows.upgrade.RuneEtching.CHAOS ? STONE : rune == net.sweenus.simplybows.upgrade.RuneEtching.BOUNTY ? STAR : -1;
        if (bow.equals("bee") && rune == net.sweenus.simplybows.upgrade.RuneEtching.PAIN) return SWARM;
        if (bow.equals("bubble") && rune == net.sweenus.simplybows.upgrade.RuneEtching.PAIN) return VORTEX;
        if (bow.equals("blossom") && rune == net.sweenus.simplybows.upgrade.RuneEtching.BOUNTY) return LOTUS;
        return -1;
    }
    public static int level(int value) { return Math.max(0, Math.min(5, value)); }
    public static int duration(int kind, int strings) {
        return switch (kind) { case STONE -> 80; case STAR -> 90; case SWARM -> 60 + 12 * level(strings);
            case VORTEX -> 80 + 8 * level(strings); case LOTUS -> 70; default -> 30; };
    }
    public static double radius(int kind, int strings) {
        return (kind == STONE ? 3.0 : kind == VORTEX ? 5.0 : 4.0) + 0.5 * level(strings);
    }
    public static float damage(int kind, int frames) {
        int f = level(frames);
        return switch (kind) { case STONE -> 8 + 2 * f; case STAR -> 3 + f;
            case SWARM -> 2.5F + 0.5F * f; case VORTEX -> 4 + 0.4F * f; default -> 12 + 3 * f; };
    }
    public static float finisher(int frames) { return 6 + level(frames); }
    public static int cooldown(int kind, int strings) {
        return switch (kind) { case SWARM -> 360; case VORTEX -> duration(kind, strings) * 3;
            case STAR -> 270; default -> 150; };
    }
    public static final int STONE_PAIRS = 5;
    public static int stoneStrikeTick(int pair) { return 28 + 8 * pair; }
    public static double stonePairZ(int pair, double radius) { return (pair - 2) * radius * .45; }
    public static float stoneClosing(float elapsed, int pair) {
        return smooth((elapsed - (stoneStrikeTick(pair) - 10)) / 10);
    }
    public static boolean pulseTick(int kind, int elapsed, int strings) {
        int interval = kind == SWARM ? 12 : kind == VORTEX ? 20 : 0;
        return interval > 0 && elapsed > 0 && elapsed <= duration(kind, strings) && elapsed % interval == 0;
    }
    public static int starWaveAge(int elapsed) { return Math.floorMod(elapsed - 1, 30); }
    public static final int FINISH_TICKS = 12;
    public static float animationScale(float elapsed, int finishAt) {
        float growTime = finishAt < 0 ? elapsed : Math.min(elapsed, finishAt - FINISH_TICKS);
        float grow = smooth(growTime / 6);
        float finish = finishAt < 0 ? 1 : smooth((finishAt - elapsed) / FINISH_TICKS);
        return grow * finish;
    }
    public static float smooth(float value) {
        float v = Math.max(0, Math.min(1, value));
        return v*v*(3-2*v);
    }
    /** One full outward/inward breath; three breaths retain the existing three-hit budget. */
    public static double starProgress(double phase) {
        double p = Math.max(0, Math.min(30, phase));
        return smooth((float)(p <= 15 ? p / 15 : (30-p) / 15));
    }
    /** Radial boundary of a filled 8-point polygon with alternating long/short vertices. */
    public static double starBoundary(double angle, double radius) {
        if (radius <= 0) return 0;
        double step = Math.PI / 8;
        double normalized = (angle % (Math.PI*2) + Math.PI*2) % (Math.PI*2);
        int sector = (int)Math.floor(normalized / step);
        double a = sector*step, b = (sector+1)*step;
        double r0 = sector%2 == 0 ? radius : radius*.52;
        double r1 = sector%2 == 0 ? radius*.52 : radius;
        double x0 = Math.cos(a)*r0, z0 = Math.sin(a)*r0;
        double ex = Math.cos(b)*r1-x0, ez = Math.sin(b)*r1-z0;
        double denominator = Math.cos(normalized)*ez-Math.sin(normalized)*ex;
        return (x0*ez-z0*ex) / denominator;
    }
    public static boolean insideStar(double x, double z, double radius, double padding) {
        return Math.hypot(x,z) <= starBoundary(Math.atan2(z,x),radius) + padding;
    }
    public record StarCell(double x, double z, double fraction, double height) {}
    private static final java.util.Map<Integer, java.util.List<StarCell>> STAR_CELLS = new java.util.HashMap<>();
    public static synchronized java.util.List<StarCell> starCells(int strings) {
        return STAR_CELLS.computeIfAbsent(level(strings), value -> {
            double radius = radius(STAR, value);
            int steps = (int)Math.ceil(radius/.55);
            double spacing = radius/steps;
            java.util.List<StarCell> cells = new java.util.ArrayList<>();
            for (int x=-steps;x<=steps;x++) for (int z=-steps;z<=steps;z++) {
                double px=x*spacing, pz=z*spacing;
                if (insideStar(px,pz,radius,1.0E-6)) cells.add(new StarCell(px,pz,
                        Math.hypot(px,pz)/starBoundary(Math.atan2(pz,px),radius), 1.1+.45*(.5+.5*Math.sin(x*2.3+z*1.7))));
            }
            return java.util.List.copyOf(cells);
        });
    }
}
