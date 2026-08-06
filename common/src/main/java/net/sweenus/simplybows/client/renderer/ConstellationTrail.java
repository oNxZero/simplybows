package net.sweenus.simplybows.client.renderer;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ConstellationTrail {

    private static final int DEFAULT_MAX_AGE_TICKS = 25;
    private static final int DEFAULT_LINE_MAX_AGE_TICKS = 12;
    private static final float DEFAULT_MAX_CONNECTION_DIST = 2.5F;
    private static final float DEFAULT_CONNECTION_PROBABILITY = 0.35F;
    private static final int DEFAULT_SAMPLE_INTERVAL = 4;
    private static final double MIN_POINT_SPACING_SQ = 0.55 * 0.55;
    private static final double TARGET_POINT_SPACING = 1.10;
    private static final double JITTER_RADIUS = 0.55;

    private final List<TrailPoint> points = new ArrayList<>();
    private final Long2ObjectOpenHashMap<Boolean> connectionCache = new Long2ObjectOpenHashMap<>();
    private final int maxAgeTicks;
    private final int lineMaxAgeTicks;
    private final float maxConnectionDist;
    private final float connectionProbability;
    private final int sampleInterval;

    public ConstellationTrail() {
        this(DEFAULT_MAX_AGE_TICKS, DEFAULT_LINE_MAX_AGE_TICKS, DEFAULT_MAX_CONNECTION_DIST, DEFAULT_CONNECTION_PROBABILITY, DEFAULT_SAMPLE_INTERVAL);
    }

    public ConstellationTrail(int maxAgeTicks, float maxConnectionDist, float connectionProbability, int sampleInterval) {
        this(maxAgeTicks, Math.max(1, maxAgeTicks / 2), maxConnectionDist, connectionProbability, sampleInterval);
    }

    public ConstellationTrail(int maxAgeTicks, int lineMaxAgeTicks, float maxConnectionDist, float connectionProbability, int sampleInterval) {
        this.maxAgeTicks = maxAgeTicks;
        this.lineMaxAgeTicks = lineMaxAgeTicks;
        this.maxConnectionDist = maxConnectionDist;
        this.connectionProbability = connectionProbability;
        this.sampleInterval = sampleInterval;
    }

    public int getSampleInterval() {
        return sampleInterval;
    }

    public void recordPoint(Vec3d pos, long worldTick) {
        if (points.isEmpty()) {
            addPoint(pos, worldTick);
            return;
        }

        Vec3d previous = points.get(points.size() - 1).anchorPos;
        double distance = previous.distanceTo(pos);
        if (distance * distance < MIN_POINT_SPACING_SQ) {
            return;
        }

        int samples = Math.max(1, (int) Math.ceil(distance / TARGET_POINT_SPACING));
        for (int i = 1; i <= samples; i++) {
            double t = i / (double) samples;
            addPoint(new Vec3d(
                    previous.x + (pos.x - previous.x) * t,
                    previous.y + (pos.y - previous.y) * t,
                    previous.z + (pos.z - previous.z) * t
            ), worldTick);
        }
        connectionCache.clear();
    }

    private void addPoint(Vec3d pos, long worldTick) {
        points.add(new TrailPoint(pos, jitteredPos(pos, worldTick, points.size()), worldTick));
    }

    public void prune(long currentTick) {
        long cutoff = currentTick - maxAgeTicks;
        boolean removed = points.removeIf(p -> p.birthTick < cutoff);
        if (removed) {
            connectionCache.clear();
        }
    }

    public boolean shouldConnect(int indexA, int indexB) {
        if (indexA >= indexB) return false;
        int indexDelta = indexB - indexA;
        if (indexDelta == 1) return true;
        if (indexDelta > 5) return false;

        long key = ((long) indexA << 32) | (indexB & 0xFFFFFFFFL);
        Boolean cached = connectionCache.get(key);
        if (cached != null) return cached;

        TrailPoint a = points.get(indexA);
        TrailPoint b = points.get(indexB);

        double distSq = a.pos.squaredDistanceTo(b.pos);
        double maxDistSq = maxConnectionDist * maxConnectionDist;
        if (distSq > maxDistSq) {
            connectionCache.put((long) key, (Boolean) false);
            return false;
        }

        long seed = mix(a.seed ^ Long.rotateLeft(b.seed, 21));
        double baseChance = connectionProbability;
        double distanceFalloff = 1.0 - Math.min(1.0, distSq / maxDistSq);
        double chance = baseChance * (0.45 + distanceFalloff * 0.55);
        boolean connected = randomUnit(seed) < chance;

        connectionCache.put((long) key, (Boolean) connected);
        return connected;
    }

    private static Vec3d jitteredPos(Vec3d pos, long worldTick, int index) {
        long seed = mix(Double.doubleToLongBits(pos.x) ^ Long.rotateLeft(Double.doubleToLongBits(pos.y), 17)
                ^ Long.rotateLeft(Double.doubleToLongBits(pos.z), 31) ^ worldTick ^ (index * 0x9E3779B97F4A7C15L));
        double yaw = randomUnit(seed) * Math.PI * 2.0;
        double pitch = (randomUnit(seed >>> 11) - 0.5) * 1.15;
        double radius = JITTER_RADIUS * (0.35 + randomUnit(seed >>> 23) * 0.65);
        double horizontal = Math.cos(pitch) * radius;

        return pos.add(
                Math.cos(yaw) * horizontal,
                Math.sin(pitch) * radius + 0.08,
                Math.sin(yaw) * horizontal
        );
    }

    private static long mix(long seed) {
        seed = (seed ^ (seed >>> 16)) * 0x45d9f3bL;
        seed = (seed ^ (seed >>> 16)) * 0x45d9f3bL;
        return seed ^ (seed >>> 16);
    }

    private static double randomUnit(long seed) {
        return (mix(seed) & 0xFFFF) / 65536.0;
    }

    public List<TrailPoint> getPoints() {
        return points;
    }

    public float getAlpha(int index, long currentTick) {
        return getAlpha(index, currentTick, maxAgeTicks);
    }

    public float getLineAlpha(int index, long currentTick) {
        return getAlpha(index, currentTick, lineMaxAgeTicks);
    }

    private float getAlpha(int index, long currentTick, int maxAge) {
        if (index < 0 || index >= points.size()) return 0;
        long age = currentTick - points.get(index).birthTick;
        if (age >= maxAge) return 0;
        if (age <= 0) return 1.0F;
        return 1.0F - ((float) age / maxAge);
    }

    public void clear() {
        points.clear();
        connectionCache.clear();
    }

    public static class TrailPoint {
        public final Vec3d anchorPos;
        public final Vec3d pos;
        public final long birthTick;
        public final long seed;

        public TrailPoint(Vec3d anchorPos, Vec3d pos, long birthTick) {
            this.anchorPos = anchorPos;
            this.pos = pos;
            this.birthTick = birthTick;
            this.seed = mix(Double.doubleToLongBits(pos.x) ^ Long.rotateLeft(Double.doubleToLongBits(pos.y), 19)
                    ^ Long.rotateLeft(Double.doubleToLongBits(pos.z), 37) ^ birthTick);
        }
    }
}
