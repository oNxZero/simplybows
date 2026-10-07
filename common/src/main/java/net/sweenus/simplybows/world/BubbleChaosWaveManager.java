package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.BubbleChaosWaveVisualEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.util.NetworkCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BubbleChaosWaveManager {

    private static final int WAVE_PARTICLE_STRIPS = 9;
    private static final int WAVE_VISUAL_LANES = 3;
    private static final int VISUAL_RISE_TICKS = 3;
    private static final int VISUAL_HOLD_TICKS = 2;
    private static final int VISUAL_SINK_TICKS = 7;
    private static final double VISUAL_START_DEPTH = 1.15;
    private static final double MIN_VISUAL_LANE_SPACING = 0.5;
    private static final double VISUAL_LANE_OVERLAP = 0.02;
    private static final int GROUND_SCAN_UP = 4;
    private static final int GROUND_SCAN_DOWN = 12;
    private static final String CHAOS_WAVE_VISUAL_TAG = "simplybows_bubble_chaos_wave_visual";
    private static final Map<ServerWorld, List<ActiveWave>> ACTIVE_WAVES = new HashMap<>();

    private BubbleChaosWaveManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveWave> waves = ACTIVE_WAVES.get(world);
        return (waves != null && !waves.isEmpty()) || (world.getTime() % 20L == 0L);
    }

    public static void cast(ServerWorld world, LivingEntity caster, BowUpgradeData upgrades) {
        if (world == null || caster == null || !caster.isAlive()) {
            return;
        }

        Vec3d look = caster.getRotationVec(1.0F);
        Vec3d horizontalForward = new Vec3d(look.x, 0.0, look.z);
        if (horizontalForward.lengthSquared() <= 1.0E-6) {
            horizontalForward = Vec3d.fromPolar(0.0F, caster.getYaw());
        } else {
            horizontalForward = horizontalForward.normalize();
        }

        Vec3d right = new Vec3d(-horizontalForward.z, 0.0, horizontalForward.x).normalize();
        Vec3d start = caster.getPos().add(horizontalForward.multiply(waveForwardStartOffset()));
        int maxSteps = baseLengthSteps() + Math.max(0, upgrades.stringLevel()) * lengthStepsPerString();
        float damage = chaosBaseDamage() + Math.max(0, upgrades.frameLevel()) * chaosDamagePerFrame();
        double knockback = chaosBaseKnockback() + Math.max(0, upgrades.frameLevel()) * chaosKnockbackPerFrame();
        UUID ownerId = caster.getUuid();

        ActiveWave wave = new ActiveWave(start, horizontalForward, right, ownerId, world.getTime(), maxSteps, damage, knockback, Math.max(0, upgrades.frameLevel()));
        ACTIVE_WAVES.computeIfAbsent(world, w -> new ArrayList<>()).add(wave);

        world.playSound(null, start.x, start.y, start.z, SoundEvents.ENTITY_DOLPHIN_SPLASH, SoundCategory.PLAYERS, 0.85F, 0.9F + world.random.nextFloat() * 0.15F);
        world.playSound(null, start.x, start.y, start.z, SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, SoundCategory.PLAYERS, 0.8F, 0.9F + world.random.nextFloat() * 0.1F);
    }

    public static void tick(ServerWorld world) {
        List<ActiveWave> waves = ACTIVE_WAVES.get(world);
        if (waves == null || waves.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanVisuals(world);
            }
            return;
        }

        waves.removeIf(wave -> tickWave(world, wave) && wave.visuals.isEmpty());
        animateVisuals(world, waves);
        if (waves.isEmpty()) {
            ACTIVE_WAVES.remove(world);
        }
    }

    private static boolean tickWave(ServerWorld world, ActiveWave wave) {
        long age = world.getTime() - wave.spawnTick;
        if (age < 0 || age % waveStepIntervalTicks() != 0L) {
            return false;
        }

        int step = wave.currentStep++;
        if (step > wave.maxSteps) {
            return true;
        }

        double travel = step * waveStepDistance();
        Vec3d center = wave.start.add(wave.forward.multiply(travel));
        spawnWaveParticles(world, center, wave, step);
        spawnWaveVisualSegments(world, center, wave, step);
        applyWaveDamage(world, center, wave);

        if (step % 2 == 0) {
            world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 0.35F, 1.15F + world.random.nextFloat() * 0.15F);
        }
        return step > wave.maxSteps;
    }

    private static void applyWaveDamage(ServerWorld world, Vec3d center, ActiveWave wave) {
        Entity ownerEntity = world.getEntity(wave.ownerId);
        LivingEntity owner = ownerEntity instanceof LivingEntity living ? living : null;
        Box hitBox = Box.of(center.add(0.0, 0.5, 0.0), waveSegmentThickness() * 2.0, 2.2, waveWidthBlocks());

        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                hitBox,
                CombatTargeting::isOffensiveTargetCandidate
        )) {
            if (owner != null && !CombatTargeting.checkFriendlyFire(candidate, owner)) {
                continue;
            }
            if (!wave.hitEntities.add(candidate.getUuid())) {
                continue;
            }

            boolean damaged = CombatTargeting.applyDamage(world, owner, candidate, wave.damage, true, false);
            if (!damaged) {
                continue;
            }
            Vec3d push = wave.forward.multiply(wave.knockback);
            candidate.addVelocity(push.x, chaosKnockUp(), push.z);
            candidate.velocityDirty = true;
            if (candidate instanceof ServerPlayerEntity serverPlayer) {
                NetworkCompat.sendVelocityUpdate(serverPlayer);
            }
        }
    }

    private static void spawnWaveVisualSegments(ServerWorld world, Vec3d center, ActiveWave wave, int step) {
        float crestHeight = 2.0F + wave.frameLevel * 0.1F;
        double laneSpacing = visualLaneSpacing();
        for (int lane = 0; lane < WAVE_VISUAL_LANES; lane++) {
            float laneCenter = (WAVE_VISUAL_LANES - 1) * 0.5F;
            float laneOffsetUnits = lane - laneCenter;
            Vec3d lanePos = center.add(wave.right.multiply(laneOffsetUnits * laneSpacing));
            double groundY = findGroundTopY(world, lanePos.x, lanePos.z, lanePos.y);
            float laneFalloff = 1.0F - Math.abs(laneOffsetUnits) * 0.18F;
            float targetHeight = Math.max(0.35F, crestHeight * laneFalloff);
            spawnVisualSegment(world, wave, lanePos.x, groundY, lanePos.z, targetHeight, step);
        }
    }

    private static void spawnVisualSegment(ServerWorld world, ActiveWave wave, double x, double y, double z, float targetHeight, int spawnStep) {
        BubbleChaosWaveVisualEntity visual = new BubbleChaosWaveVisualEntity(world, x, y - VISUAL_START_DEPTH, z, targetHeight);
        visual.addCommandTag(CHAOS_WAVE_VISUAL_TAG);
        if (world.spawnEntity(visual)) {
            wave.visuals.add(new WaveVisual(visual.getUuid(), x, y, z, world.getTime(), spawnStep));
        }
    }

    private static void animateVisuals(ServerWorld world, List<ActiveWave> waves) {
        for (ActiveWave wave : waves) {
            wave.visuals.removeIf(visual -> {
                Entity entity = world.getEntity(visual.id);
                if (!(entity instanceof BubbleChaosWaveVisualEntity waveVisual)) {
                    return true;
                }

                long age = world.getTime() - visual.spawnTick;
                if (age >= visualLifetimeTicks()) {
                    waveVisual.discard();
                    return true;
                }

                float baseScale = getHeightScale(age);
                int behindSteps = Math.max(0, wave.currentStep - visual.spawnStep);
                float trailFactor = Math.max(0.26F, 1.0F - behindSteps * 0.16F);
                float finalScale = Math.max(0.0F, baseScale * trailFactor);
                waveVisual.setHeightScale(finalScale);
                double yOffset = getVerticalOffset(age);
                waveVisual.setPos(visual.baseX, visual.baseY + yOffset, visual.baseZ);
                return false;
            });
        }
    }

    private static void spawnWaveParticles(ServerWorld world, Vec3d center, ActiveWave wave, int step) {
        double halfWidth = waveWidthBlocks() * 0.5;
        int strips = WAVE_PARTICLE_STRIPS;
        for (int i = 0; i < strips; i++) {
            double t = strips == 1 ? 0.0 : (double) i / (strips - 1);
            double lateral = (t - 0.5) * waveWidthBlocks();
            Vec3d edgePoint = center.add(wave.right.multiply(lateral));
            double crest = 0.22 + Math.sin((step * 0.55) + (t * Math.PI)) * 0.08;
            world.spawnParticles(ParticleTypes.SPLASH, edgePoint.x, edgePoint.y + crest, edgePoint.z, 1, 0.02, 0.03, 0.02, 0.12);
            world.spawnParticles(ParticleTypes.BUBBLE, edgePoint.x, edgePoint.y + crest - 0.1, edgePoint.z, 1, 0.02, 0.03, 0.02, 0.0);
            world.spawnParticles(ParticleTypes.BUBBLE_POP, edgePoint.x, edgePoint.y + crest + 0.02, edgePoint.z, 1, 0.02, 0.02, 0.02, 0.0);
            if (i % 2 == 0) {
                world.spawnParticles(ParticleTypes.FALLING_WATER, edgePoint.x, edgePoint.y + crest + 0.06, edgePoint.z, 1, 0.01, 0.02, 0.01, 0.0);
            }
            if (i % 3 != 1) {
                world.spawnParticles(ParticleTypes.WHITE_ASH, edgePoint.x, edgePoint.y + crest + 0.08, edgePoint.z, 1, 0.03, 0.02, 0.03, 0.0);
            }
        }

        world.spawnParticles(ParticleTypes.BUBBLE_POP, center.x, center.y + 0.28, center.z, 4, halfWidth * 0.33, 0.09, 0.18, 0.0);
        world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + 0.24, center.z, 2, halfWidth * 0.28, 0.08, 0.16, 0.0);
        if (step % 2 == 0) {
            world.spawnParticles(ParticleTypes.FISHING, center.x, center.y + 0.32, center.z, 2, halfWidth * 0.35, 0.08, 0.2, 0.0);
        }
        if (step % 3 == 0) {
            world.spawnParticles(ParticleTypes.WAX_OFF, center.x, center.y + 0.26, center.z, 2, halfWidth * 0.25, 0.06, 0.12, 0.0);
        }
    }

    private static float getHeightScale(long age) {
        if (age < 0L) {
            return 0.0F;
        }
        int riseTicks = VISUAL_RISE_TICKS;
        int holdTicks = VISUAL_HOLD_TICKS;
        int sinkTicks = VISUAL_SINK_TICKS;
        if (age < riseTicks) {
            float t = MathHelper.clamp((float) age / (float) riseTicks, 0.0F, 1.0F);
            return easeOutBack(t);
        }
        if (age < riseTicks + holdTicks) {
            return 1.0F;
        }
        long sinkAge = age - riseTicks - holdTicks;
        float t = MathHelper.clamp((float) sinkAge / (float) sinkTicks, 0.0F, 1.0F);
        return 1.0F - (t * t * t);
    }

    private static double getVerticalOffset(long age) {
        double startDepth = VISUAL_START_DEPTH;
        int riseTicks = VISUAL_RISE_TICKS;
        int holdTicks = VISUAL_HOLD_TICKS;
        int sinkTicks = VISUAL_SINK_TICKS;
        if (age < riseTicks) {
            float t = MathHelper.clamp((float) age / (float) riseTicks, 0.0F, 1.0F);
            return -startDepth + (startDepth * easeOutBack(t));
        }
        if (age < riseTicks + holdTicks) {
            return 0.0;
        }
        long sinkAge = age - riseTicks - holdTicks;
        float t = MathHelper.clamp((float) sinkAge / (float) sinkTicks, 0.0F, 1.0F);
        return -(startDepth * t * t * t);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float p = t - 1.0F;
        return 1.0F + c3 * p * p * p + c1 * p * p;
    }

    private static double waveWidthBlocks() {
        return SimplyBowsConfig.INSTANCE.bubbleveil.chaosWaveWidthBlocks.get();
    }

    private static double visualLaneSpacing() {
        if (WAVE_VISUAL_LANES <= 1) {
            return waveWidthBlocks();
        }
        double laneWidth = waveWidthBlocks() / WAVE_VISUAL_LANES;
        // Slight overlap avoids visible seams between adjacent rendered wave blocks.
        return Math.max(MIN_VISUAL_LANE_SPACING, laneWidth - VISUAL_LANE_OVERLAP);
    }

    private static double waveSegmentThickness() {
        return SimplyBowsConfig.INSTANCE.bubbleveil.chaosWaveSegmentThickness.get();
    }

    private static double waveStepDistance() {
        return SimplyBowsConfig.INSTANCE.bubbleveil.chaosWaveStepDistance.get();
    }

    private static int waveStepIntervalTicks() {
        return Math.max(1, SimplyBowsConfig.INSTANCE.bubbleveil.chaosWaveStepIntervalTicks.get());
    }

    private static double waveForwardStartOffset() {
        return SimplyBowsConfig.INSTANCE.bubbleveil.chaosWaveForwardStartOffset.get();
    }

    private static int baseLengthSteps() {
        return SimplyBowsConfig.INSTANCE.bubbleveil.chaosBaseLengthSteps.get();
    }

    private static int lengthStepsPerString() {
        return SimplyBowsConfig.INSTANCE.bubbleveil.chaosLengthStepsPerString.get();
    }

    private static float chaosBaseDamage() {
        return SimplyBowsConfig.INSTANCE.bubbleveil.chaosBaseDamage.get();
    }

    private static float chaosDamagePerFrame() {
        return SimplyBowsConfig.INSTANCE.bubbleveil.chaosDamagePerFrame.get();
    }

    private static double chaosBaseKnockback() {
        return SimplyBowsConfig.INSTANCE.bubbleveil.chaosBaseKnockback.get();
    }

    private static double chaosKnockbackPerFrame() {
        return SimplyBowsConfig.INSTANCE.bubbleveil.chaosKnockbackPerFrame.get();
    }

    private static double chaosKnockUp() {
        return SimplyBowsConfig.INSTANCE.bubbleveil.chaosKnockUp.get();
    }

    private static int visualLifetimeTicks() {
        return VISUAL_RISE_TICKS + VISUAL_HOLD_TICKS + VISUAL_SINK_TICKS;
    }

    private static double findGroundTopY(ServerWorld world, double x, double z, double centerY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = (int) Math.floor(centerY) + GROUND_SCAN_UP;
        int minY = Math.max(world.getBottomY(), (int) Math.floor(centerY) - GROUND_SCAN_DOWN);
        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return y + 1.0;
            }
        }
        return centerY;
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof BubbleChaosWaveVisualEntity && entity.getCommandTags().contains(CHAOS_WAVE_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static final class ActiveWave {
        private final Vec3d start;
        private final Vec3d forward;
        private final Vec3d right;
        private final UUID ownerId;
        private final long spawnTick;
        private final int maxSteps;
        private final float damage;
        private final double knockback;
        private final int frameLevel;
        private final Set<UUID> hitEntities = new HashSet<>();
        private final List<WaveVisual> visuals = new ArrayList<>();
        private int currentStep;

        private ActiveWave(Vec3d start, Vec3d forward, Vec3d right, UUID ownerId, long spawnTick, int maxSteps, float damage, double knockback, int frameLevel) {
            this.start = start;
            this.forward = forward;
            this.right = right;
            this.ownerId = ownerId;
            this.spawnTick = spawnTick;
            this.maxSteps = maxSteps;
            this.damage = damage;
            this.knockback = knockback;
            this.frameLevel = frameLevel;
            this.currentStep = 0;
        }
    }

    private static final class WaveVisual {
        private final UUID id;
        private final double baseX;
        private final double baseY;
        private final double baseZ;
        private final long spawnTick;
        private final int spawnStep;

        private WaveVisual(UUID id, double baseX, double baseY, double baseZ, long spawnTick, int spawnStep) {
            this.id = id;
            this.baseX = baseX;
            this.baseY = baseY;
            this.baseZ = baseZ;
            this.spawnTick = spawnTick;
            this.spawnStep = spawnStep;
        }
    }
}
