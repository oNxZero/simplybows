package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
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
import net.sweenus.simplybows.entity.EarthSpikeVisualEntity;
import net.sweenus.simplybows.entity.IceChaosWallVisualEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.util.GraceProjectile;
import net.sweenus.simplybows.util.NetworkCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EarthSpikeFieldManager {

    private static final int RAISE_TICKS = 9;
    private static final int HOLD_TICKS = 0;
    private static final int SINK_TICKS = 11;
    private static final int FIELD_DURATION_TICKS = RAISE_TICKS + HOLD_TICKS + SINK_TICKS;
    private static final double PATCH_VISUAL_RADIUS = 2.1;
    private static final int PATCH_VISUAL_POINTS = 20;
    private static final int GROUND_SCAN_UP = 4;
    private static final int GROUND_SCAN_DOWN = 16;
    private static final double SPIKE_SEGMENT_HEIGHT = 0.34;
    private static final double START_DEPTH = 3.2;
    private static final double BASE_GROUND_OFFSET = -0.18;
    private static final int PAIN_FISSURE_STEPS = 9; // ~+2 blocks travel
    private static final double PAIN_FISSURE_STEP = 0.95;
    private static final double PAIN_FISSURE_HIT_RADIUS = 3.15; // +2 blocks wider
    private static final double BOUNTY_CENTER_KNOCKBACK_BASE_MULTIPLIER = 0.55;
    private static final double BOUNTY_CENTER_KNOCKBACK_PROXIMITY_MULTIPLIER = 0.35;
    private static final double BOUNTY_MAX_KNOCKUP = 0.55;
    private static final int BOUNTY_PULSE_COUNT = 3;
    private static final int BOUNTY_PULSE_GAP_TICKS = 10; // 0.5s between pulses
    private static final int GRACE_WALL_BASE_DURATION_TICKS = 120; // 6s base (+3s vs old)
    private static final int GRACE_WALL_DURATION_PER_FRAME = 20; // Frame = duration
    private static final int GRACE_WALL_COOLDOWN_MULTIPLIER = 3; // longer CD for balance
    private static final int GRACE_RISE_TICKS = 6;
    private static final int GRACE_SINK_TICKS = 10;
    private static final double GRACE_WALL_THICKNESS = 1.45;
    private static final float GRACE_WALL_HEIGHT = 2.85F;
    private static final double GRACE_BASE_RADIUS = 2.75;
    private static final double GRACE_RADIUS_PER_STRING = 0.28; // String5 ≈ 4.15, not 9+
    private static final double GRACE_FORWARD_SCALE = 1.15; // oval depth
    private static final double GRACE_SIDE_SCALE = 1.08; // wider oval
    private static final double GRACE_ARC_HALF = Math.PI * 0.62; // ~112° wrap
    private static final double GRACE_SEGMENT_HALF_WIDTH = 0.55;
    private static final int GRACE_ABSORPTION_AMPLIFIER = 2; // Absorption III
    private static final int GRACE_ABSORPTION_TICKS = 120; // 6s
    private static final String SPIKE_VISUAL_TAG = "simplybows_earth_spike_visual";
    private static final String GRACE_WALL_VISUAL_TAG = "simplybows_earth_grace_wall_visual";

    private static double fieldRadius() { return SimplyBowsConfig.INSTANCE.tremorstrike.fieldRadius.get(); }
    private static float spikeDamage() { return SimplyBowsConfig.INSTANCE.tremorstrike.spikeDamage.get(); }
    private static double baseUpwardKnockback() { return SimplyBowsConfig.INSTANCE.tremorstrike.baseUpwardKnockback.get(); }
    private static double frameUpwardKnockbackPerLevel() { return SimplyBowsConfig.INSTANCE.tremorstrike.frameUpwardKnockbackPerLevel.get(); }
    private static double stringRadiusBonusPerLevel() { return SimplyBowsConfig.INSTANCE.tremorstrike.stringRadiusBonusPerLevel.get(); }
    private static double painWaveMaxDistance() { return SimplyBowsConfig.INSTANCE.tremorstrike.painWaveMaxDistance.get(); }
    private static double painWaveStepDistance() { return SimplyBowsConfig.INSTANCE.tremorstrike.painWaveStepDistance.get(); }
    private static float painWaveDamageMultiplier() { return SimplyBowsConfig.INSTANCE.tremorstrike.painWaveDamageMultiplier.get(); }
    private static double stringWaveDistanceBonusPerLevel() { return SimplyBowsConfig.INSTANCE.tremorstrike.painStringWaveDistanceBonusPerLevel.get(); }
    private static int graceResistanceDurationTicks() { return SimplyBowsConfig.INSTANCE.tremorstrike.graceResistanceDuration.get(); }
    private static int graceSlowFallingDurationTicks() { return SimplyBowsConfig.INSTANCE.tremorstrike.graceSlowFallingDuration.get(); }
    private static int bountyCenterBaseHeightSegments() { return SimplyBowsConfig.INSTANCE.tremorstrike.bountyCenterBaseHeightSegments.get(); }
    private static int bountyCenterExtraHeightPerFrame() { return SimplyBowsConfig.INSTANCE.tremorstrike.bountyCenterExtraHeightPerFrame.get(); }
    private static int fieldLockoutTicks() { return SimplyBowsConfig.INSTANCE.tremorstrike.fieldLockoutTicks.get(); }
    private static float bountyCenterDamageBaseMultiplier() { return SimplyBowsConfig.INSTANCE.tremorstrike.bountyCenterDamageBaseMultiplier.get(); }
    private static float bountyCenterDamageProximityMultiplier() { return SimplyBowsConfig.INSTANCE.tremorstrike.bountyCenterDamageProximityMultiplier.get(); }
    private static final Map<ServerWorld, List<ActiveSpikeField>> ACTIVE_FIELDS = new HashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> FIELD_LOCKOUTS_BY_SERVER = CooldownStorage.newServerScopedStore();

    private EarthSpikeFieldManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveSpikeField> fields = ACTIVE_FIELDS.get(world);
        return (fields != null && !fields.isEmpty()) || (world.getTime() % 20L == 0L);
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center, Entity owner) {
        createOrReplaceField(world, center, owner, BowUpgradeData.none(), null);
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center, Entity owner, BowUpgradeData upgrades) {
        createOrReplaceField(world, center, owner, upgrades, null);
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center, Entity owner, BowUpgradeData upgrades, Vec3d impactDirection) {
        createOrReplaceField(world, center, owner, upgrades, impactDirection, null);
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center, Entity owner, BowUpgradeData upgrades, Vec3d impactDirection, LivingEntity hitTarget) {
        List<ActiveSpikeField> fields = ACTIVE_FIELDS.computeIfAbsent(world, w -> new ArrayList<>());
        UUID ownerId = owner != null ? owner.getUuid() : null;
        if (ownerId != null && !isFieldReady(world, ownerId, fields)) {
            return;
        }

        long now = world.getTime();
        FieldTuning tuning = buildTuning(upgrades);
        Vec3d fissureDir = resolveFissureDirection(impactDirection, owner);
        int bountyExtra = tuning.bountyCenterSpike()
                ? (BOUNTY_PULSE_COUNT - 1) * (FIELD_DURATION_TICKS + BOUNTY_PULSE_GAP_TICKS)
                : 0;
        int painExtra = tuning.directedFissure() ? PAIN_FISSURE_STEPS : 0;
        int graceWallTicks = GRACE_WALL_BASE_DURATION_TICKS + Math.max(0, upgrades.frameLevel()) * GRACE_WALL_DURATION_PER_FRAME;
        int lifetime = tuning.graceSupport() ? graceWallTicks : FIELD_DURATION_TICKS + bountyExtra + painExtra;
        ActiveSpikeField field = new ActiveSpikeField(center, now, now + lifetime, ownerId, tuning, fissureDir);
        if (tuning.directedFissure()) {
            spawnPainFissureVisuals(world, field);
        } else if (tuning.graceSupport()) {
            spawnGraceDripstoneRing(world, field);
        } else {
            List<SpikePoint> points = buildPatchPoints(world, center, tuning);
            spawnSpikeVisuals(world, field, points);
            if (tuning.bountyCenterSpike()) {
                spawnBountyCenterSpikeVisuals(world, field);
            }
        }
        if (tuning.bountyCenterSpike()) {
            field.bountyPulsesRemaining = BOUNTY_PULSE_COUNT - 1;
            field.nextBountyPulseTick = now + FIELD_DURATION_TICKS + BOUNTY_PULSE_GAP_TICKS;
        }
        fields.add(field);
        if (ownerId != null) {
            // Grace: ~2× wall duration CD (was 3× — too punishing after Absorption buff).
            int lockoutTicks = tuning.graceSupport()
                    ? Math.max(160, graceWallTicks * 2)
                    : Math.max(fieldLockoutTicks(), RuneUseCooldown.fromEffectDuration(lifetime));
            CooldownStorage.forWorld(FIELD_LOCKOUTS_BY_SERVER, world)
                    .put(ownerId, now + lockoutTicks);
            RuneUseCooldown.start(world, ownerId, "earth-field", "earth", lockoutTicks);
        }

        LivingEntity ownerEntity = getOwnerEntity(world, ownerId);
        if (tuning.graceSupport()) {
            applyGraceAllySupport(world, ownerEntity, center, tuning, hitTarget);
        } else if (tuning.bountyCenterSpike()) {
            applyBountyCenterImpact(world, ownerEntity, center, tuning);
        } else if (tuning.directedFissure()) {
            applyPainFissure(world, ownerEntity, field);
        } else {
            applySpikeDamage(world, ownerEntity, center, tuning.radius(), tuning.damage(), tuning.upwardKnockback());
        }
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.35F, 0.65F + world.random.nextFloat() * 0.1F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundCategory.PLAYERS, 1.0F, 0.75F + world.random.nextFloat() * 0.1F);
        spawnBurstParticles(world, center);
    }

    public static void tick(ServerWorld world) {
        List<ActiveSpikeField> fields = ACTIVE_FIELDS.get(world);
        if (fields == null || fields.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanSpikeVisuals(world);
            }
            return;
        }

        fields.removeIf(field -> {
            if (world.getTime() > field.expiryTick()) {
                removeField(world, field);
                return true;
            }
            return false;
        });
        if (fields.isEmpty()) {
            ACTIVE_FIELDS.remove(world);
            purgeOrphanSpikeVisuals(world);
            return;
        }

        for (ActiveSpikeField field : fields) {
            tickBountyPulses(world, field);
            if (field.tuning().graceSupport()) {
                animateGraceWall(world, field);
                blockGraceRing(world, field);
                spawnGraceGroundMarker(world, field);
            } else {
                animateField(world, field);
            }
        }
    }

    private static boolean isFieldReady(ServerWorld world, UUID ownerId, List<ActiveSpikeField> fields) {
        if (!RuneUseCooldown.isPlayerReady(world, ownerId)) {
            return false;
        }
        long now = CooldownStorage.currentTick(world);
        Long lockoutEnd = CooldownStorage.forWorld(FIELD_LOCKOUTS_BY_SERVER, world).get(ownerId);
        if (lockoutEnd != null && lockoutEnd > now) {
            return false;
        }
        for (ActiveSpikeField field : fields) {
            if (ownerId.equals(field.ownerId()) && now <= field.expiryTick()) {
                return false;
            }
        }
        return true;
    }

    private static void applySpikeDamage(ServerWorld world, LivingEntity owner, Vec3d center, double radius, float damage, double upwardKnockback) {
        Box box = Box.of(center, radius * 2.0, 3.5, radius * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                box,
                CombatTargeting::isOffensiveTargetCandidate
        )) {
            if (candidate.squaredDistanceTo(center) > radius * radius) {
                continue;
            }
            boolean damaged = CombatTargeting.applyDamage(world, owner, candidate, damage, true, false);
            if (damaged) {
                applyUpwardKnockback(candidate, upwardKnockback);
            }
        }
    }

    private static void applyBountyCenterImpact(ServerWorld world, LivingEntity owner, Vec3d center, FieldTuning tuning) {
        double centerRadius = Math.max(1.6, tuning.radius() * 0.55);
        Box centerBox = Box.of(center, centerRadius * 2.0, 4.0, centerRadius * 2.0);

        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                centerBox,
                CombatTargeting::isOffensiveTargetCandidate
        )) {
            double dist = candidate.getPos().distanceTo(center);
            if (dist > centerRadius) {
                continue;
            }

            double proximity = 1.0 - MathHelper.clamp(dist / centerRadius, 0.0, 1.0);
            // Per-pulse damage — three pulses make this stronger than a single base field.
            float scaledDamage = tuning.damage() * 0.55F
                    * (bountyCenterDamageBaseMultiplier() + (float) (proximity * bountyCenterDamageProximityMultiplier()));
            boolean damaged = CombatTargeting.applyDamage(world, owner, candidate, scaledDamage, false, false);
            if (damaged) {
                double scaledKnockup = Math.min(BOUNTY_MAX_KNOCKUP,
                        Math.min(0.42, tuning.upwardKnockback())
                                * (BOUNTY_CENTER_KNOCKBACK_BASE_MULTIPLIER + (proximity * BOUNTY_CENTER_KNOCKBACK_PROXIMITY_MULTIPLIER)));
                applyUpwardKnockback(candidate, scaledKnockup);
            }
        }
    }

    private static void applyGraceAllySupport(ServerWorld world, LivingEntity owner, Vec3d center, FieldTuning tuning, LivingEntity hitTarget) {
        if (owner == null) {
            return;
        }

        Box box = Box.of(center, tuning.radius() * 2.0, 3.5, tuning.radius() * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                box,
                entity -> entity.isAlive() && entity.squaredDistanceTo(center) <= tuning.radius() * tuning.radius()
        )) {
            if (!CombatTargeting.isFriendlyTo(candidate, owner)) {
                continue;
            }

            applyUpwardKnockback(candidate, tuning.upwardKnockback());
            candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, graceResistanceDurationTicks(), 0), owner);
            candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, graceSlowFallingDurationTicks(), 0), owner);
        }

        LivingEntity absorbTarget = hitTarget != null && hitTarget.isAlive() && GraceProjectile.isSupportTarget(hitTarget)
                ? hitTarget
                : owner;
        if (absorbTarget != null && absorbTarget.isAlive()) {
            absorbTarget.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.ABSORPTION, GRACE_ABSORPTION_TICKS, GRACE_ABSORPTION_AMPLIFIER),
                    owner
            );
        }
    }

    private static void spawnBurstParticles(ServerWorld world, Vec3d center) {
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.getDefaultState()), center.x, center.y + 0.12, center.z, 18, 1.1, 0.22, 1.1, 0.01);
        world.spawnParticles(ParticleTypes.POOF, center.x, center.y + 0.15, center.z, 8, 0.9, 0.12, 0.9, 0.01);
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.x, center.y + 0.05, center.z, 14, 1.0, 0.05, 1.0, 0.01);
    }

    private static List<SpikePoint> buildPatchPoints(ServerWorld world, Vec3d center, FieldTuning tuning) {
        List<SpikePoint> points = new ArrayList<>();
        for (int i = 0; i < tuning.points(); i++) {
            double angle = ((Math.PI * 2.0) / tuning.points()) * i;
            double ringScale = (0.45 + ((i * 13) % 10) * 0.06);
            double radius = tuning.visualRadius() * Math.min(1.0, ringScale);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = findGroundTopY(world, x, z, center.y) + BASE_GROUND_OFFSET;
            int heightSegments = 1 + ((i * 11) % 8);
            points.add(new SpikePoint(x, y, z, heightSegments));
        }
        return points;
    }

    private static FieldTuning buildTuning(BowUpgradeData upgrades) {
        double sizeMultiplier = upgrades.sizeMultiplier();
        float damage = (float) (spikeDamage() * upgrades.damageMultiplier());
        boolean directedFissure = upgrades.runeEtching() == RuneEtching.PAIN;
        boolean graceSupport = upgrades.runeEtching() == RuneEtching.GRACE;
        boolean bountyCenterSpike = upgrades.runeEtching() == RuneEtching.BOUNTY;
        double radius;
        if (graceSupport) {
            // String = wall radius only (no global sizeMultiplier stack).
            radius = GRACE_BASE_RADIUS + upgrades.stringLevel() * GRACE_RADIUS_PER_STRING;
        } else {
            radius = fieldRadius() * sizeMultiplier + upgrades.stringLevel() * stringRadiusBonusPerLevel();
        }
        double visualRadius = PATCH_VISUAL_RADIUS * sizeMultiplier + upgrades.stringLevel() * (stringRadiusBonusPerLevel() * 0.45);
        double painWaveDistance = Math.min(12.0, painWaveMaxDistance() * 0.85 + upgrades.stringLevel() * stringWaveDistanceBonusPerLevel() * 0.5);
        double upwardKnockback = Math.min(0.55, baseUpwardKnockback() * 0.7 + upgrades.frameLevel() * Math.min(0.06, frameUpwardKnockbackPerLevel()));
        int centerSpikeHeightSegments = bountyCenterBaseHeightSegments() + upgrades.frameLevel() * bountyCenterExtraHeightPerFrame();
        return new FieldTuning(
                radius,
                visualRadius,
                PATCH_VISUAL_POINTS + upgrades.stringLevel() * 5,
                damage,
                directedFissure,
                painWaveDistance,
                upwardKnockback,
                graceSupport,
                bountyCenterSpike,
                centerSpikeHeightSegments
        );
    }

    private static void spawnBountyCenterSpikeVisuals(ServerWorld world, ActiveSpikeField field) {
        double centerY = findGroundTopY(world, field.center().x, field.center().z, field.center().y) + BASE_GROUND_OFFSET;
        int heightSegments = field.tuning().centerSpikeHeightSegments();

        // Main center spike.
        spawnSpikeVisual(world, field, field.center().x, centerY, field.center().z, heightSegments, world.getTime());

        // Nearby ring spikes to make the center feel much wider.
        double ringRadius = 0.7;
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI * 2.0 / 6.0) * i;
            double x = field.center().x + Math.cos(angle) * ringRadius;
            double z = field.center().z + Math.sin(angle) * ringRadius;
            double y = findGroundTopY(world, x, z, field.center().y) + BASE_GROUND_OFFSET;
            spawnSpikeVisual(world, field, x, y, z, Math.max(8, heightSegments - 5), world.getTime());
        }
    }

    private static void spawnSpikeVisuals(ServerWorld world, ActiveSpikeField field, List<SpikePoint> points) {
        for (SpikePoint point : points) {
            spawnSpikeVisual(world, field, point.x(), point.y(), point.z(), point.heightSegments(), world.getTime());
        }
    }

    private static void animateField(ServerWorld world, ActiveSpikeField field) {
        field.visuals.removeIf(visual -> {
            Entity entity = world.getEntity(visual.id());
            if (!(entity instanceof EarthSpikeVisualEntity spikeVisual)) {
                return true;
            }

            long age = world.getTime() - visual.spawnTick();
            if (age >= FIELD_DURATION_TICKS) {
                spikeVisual.discard();
                return true;
            }

            float scale = getHeightScale(age);
            spikeVisual.setHeightScale(scale);
            double yOffset = getVerticalOffset(age);
            spikeVisual.setPos(visual.baseX(), visual.baseY() + yOffset, visual.baseZ());
            return false;
        });

        if ((world.getTime() - field.spawnTick()) % 2L == 0L) {
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.getDefaultState()), field.center().x, field.center().y + 0.08, field.center().z, 4, 1.4, 0.1, 1.4, 0.0);
        }
    }

    private static Vec3d resolveFissureDirection(Vec3d impactDirection, Entity owner) {
        if (impactDirection != null) {
            Vec3d horizontal = new Vec3d(impactDirection.x, 0.0, impactDirection.z);
            if (horizontal.lengthSquared() > 1.0E-6) {
                return horizontal.normalize();
            }
        }
        if (owner != null) {
            Vec3d look = owner.getRotationVec(1.0F);
            Vec3d horizontal = new Vec3d(look.x, 0.0, look.z);
            if (horizontal.lengthSquared() > 1.0E-6) {
                return horizontal.normalize();
            }
        }
        return new Vec3d(1.0, 0.0, 0.0);
    }

    private static void spawnPainFissureVisuals(ServerWorld world, ActiveSpikeField field) {
        Vec3d dir = field.fissureDirection;
        for (int step = 0; step <= PAIN_FISSURE_STEPS; step++) {
            Vec3d pos = field.center().add(dir.multiply(step * PAIN_FISSURE_STEP));
            double y = findGroundTopY(world, pos.x, pos.z, field.center().y) + BASE_GROUND_OFFSET;
            int heightSegments = 4 + (step % 3) * 2;
            spawnSpikeVisual(world, field, pos.x, y, pos.z, heightSegments, world.getTime() + step);
            // Wider trench (+~2 blocks) so Pain reads as a corridor, not a pole line.
            Vec3d side = new Vec3d(-dir.z, 0.0, dir.x).multiply(2.55);
            spawnSpikeVisual(world, field, pos.x + side.x, findGroundTopY(world, pos.x + side.x, pos.z + side.z, field.center().y) + BASE_GROUND_OFFSET, pos.z + side.z, Math.max(3, heightSegments - 2), world.getTime() + step);
            spawnSpikeVisual(world, field, pos.x - side.x, findGroundTopY(world, pos.x - side.x, pos.z - side.z, field.center().y) + BASE_GROUND_OFFSET, pos.z - side.z, Math.max(3, heightSegments - 2), world.getTime() + step);
            Vec3d mid = side.multiply(0.5);
            spawnSpikeVisual(world, field, pos.x + mid.x, findGroundTopY(world, pos.x + mid.x, pos.z + mid.z, field.center().y) + BASE_GROUND_OFFSET, pos.z + mid.z, Math.max(3, heightSegments - 1), world.getTime() + step);
            spawnSpikeVisual(world, field, pos.x - mid.x, findGroundTopY(world, pos.x - mid.x, pos.z - mid.z, field.center().y) + BASE_GROUND_OFFSET, pos.z - mid.z, Math.max(3, heightSegments - 1), world.getTime() + step);
        }
    }

    /** Half-oval dripstone wall — segment planes block like frost wall. */
    private static void spawnGraceDripstoneRing(ServerWorld world, ActiveSpikeField field) {
        double radius = Math.max(GRACE_BASE_RADIUS, field.tuning().radius());
        double forwardR = radius * GRACE_FORWARD_SCALE;
        double sideR = radius * GRACE_SIDE_SCALE;
        Vec3d facing = field.fissureDirection;
        double facingAngle = Math.atan2(facing.z, facing.x);
        double fx = Math.cos(facingAngle);
        double fz = Math.sin(facingAngle);
        double sx = -fz;
        double sz = fx;
        int segments = Math.max(16, (int) Math.round((forwardR + sideR) * 2.6));
        field.graceSegments.clear();
        for (int i = 0; i < segments; i++) {
            double t = segments <= 1 ? 0.5 : i / (double) (segments - 1);
            double ang = -GRACE_ARC_HALF + (GRACE_ARC_HALF * 2.0) * t;
            double localF = forwardR * Math.cos(ang);
            double localS = sideR * Math.sin(ang);
            double x = field.center().x + fx * localF + sx * localS;
            double z = field.center().z + fz * localF + sz * localS;
            double y = findGroundTopY(world, x, z, field.center().y);
            // Outward radial normal in world XZ (ice-wall style plane).
            double ox = x - field.center().x;
            double oz = z - field.center().z;
            double olen = Math.sqrt(ox * ox + oz * oz);
            Vec3d outward = olen > 1.0E-4 ? new Vec3d(ox / olen, 0.0, oz / olen) : new Vec3d(fx, 0.0, fz);
            Vec3d tangent = new Vec3d(-outward.z, 0.0, outward.x);
            field.graceSegments.add(new GraceSegment(new Vec3d(x, y, z), outward, tangent));

            IceChaosWallVisualEntity visual = new IceChaosWallVisualEntity(world, x, y, z, GRACE_WALL_HEIGHT);
            visual.setDripstoneStyle(true);
            float yaw = (float) (Math.atan2(outward.x, outward.z) * (180.0 / Math.PI));
            visual.setYaw(yaw);
            visual.prevYaw = yaw;
            visual.addCommandTag(GRACE_WALL_VISUAL_TAG);
            if (world.spawnEntity(visual)) {
                field.visuals.add(new SpikeVisual(visual.getUuid(), x, y, z, world.getTime()));
            }
        }
        world.playSound(null, field.center().x, field.center().y, field.center().z,
                SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.PLAYERS, 1.0F, 0.65F);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.getDefaultState()),
                field.center().x, field.center().y + 0.4, field.center().z, 28, sideR * 0.55, 0.6, forwardR * 0.35, 0.02);
    }

    private static void animateGraceWall(ServerWorld world, ActiveSpikeField field) {
        long age = world.getTime() - field.spawnTick();
        long remaining = field.expiryTick() - world.getTime();
        float rise = MathHelper.clamp((float) age / (float) GRACE_RISE_TICKS, 0.0F, 1.0F);
        float sink = remaining <= GRACE_SINK_TICKS
                ? MathHelper.clamp((float) remaining / (float) GRACE_SINK_TICKS, 0.0F, 1.0F)
                : 1.0F;
        float scale = Math.min(rise, sink);

        field.visuals.removeIf(visual -> {
            Entity entity = world.getEntity(visual.id());
            if (!(entity instanceof IceChaosWallVisualEntity wallVisual)) {
                return true;
            }
            wallVisual.setPos(visual.baseX(), visual.baseY(), visual.baseZ());
            wallVisual.setHeightScale(scale);
            return false;
        });
    }

    /** Block walking/shooting through each dripstone segment (same rules as frost wall). */
    private static void blockGraceRing(ServerWorld world, ActiveSpikeField field) {
        if (field.graceSegments.isEmpty()) {
            return;
        }
        double height = GRACE_WALL_HEIGHT;
        double search = Math.max(field.tuning().radius() * GRACE_FORWARD_SCALE, field.tuning().radius() * GRACE_SIDE_SCALE)
                + GRACE_WALL_THICKNESS + 2.5;
        Box box = Box.of(field.center().add(0.0, height * 0.5, 0.0), search * 2.0, height + 1.4, search * 2.0);
        LivingEntity owner = getOwnerEntity(world, field.ownerId());

        for (Entity entity : world.getEntitiesByClass(Entity.class, box,
                e -> e.isAlive() && !e.isRemoved()
                        && !(e instanceof EarthSpikeVisualEntity)
                        && !(e instanceof IceChaosWallVisualEntity)
                        && !(e instanceof ProjectileEntity))) {
            // Only owner-friendlies pass — enemy players must be blocked.
            if (owner != null && entity instanceof LivingEntity living && CombatTargeting.isFriendlyTo(living, owner)) {
                continue;
            }
            for (GraceSegment seg : field.graceSegments) {
                if (pushOffGraceSegment(world, entity, seg, height)) {
                    break;
                }
            }
        }

        for (ProjectileEntity projectile : world.getEntitiesByClass(ProjectileEntity.class, box, p -> p.isAlive() && !p.isRemoved())) {
            for (GraceSegment seg : field.graceSegments) {
                if (isInsideGraceSegment(projectile.getPos(), projectile.getBoundingBox(), seg, height)
                        || isInsideGraceSegment(new Vec3d(projectile.prevX, projectile.prevY, projectile.prevZ), projectile.getBoundingBox(), seg, height)) {
                    world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.getDefaultState()),
                            projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.12, 0.12, 0.12, 0.01);
                    projectile.discard();
                    break;
                }
            }
        }
    }

    private static boolean isInsideGraceSegment(Vec3d pos, Box box, GraceSegment seg, double height) {
        Vec3d rel = pos.subtract(seg.pos);
        double lateral = Math.abs(rel.dotProduct(seg.tangent));
        double halfExtentLateral = getHorizontalHalfExtent(box, seg.tangent) + 0.12;
        if (lateral > GRACE_SEGMENT_HALF_WIDTH + halfExtentLateral) {
            return false;
        }
        if (box.maxY < seg.pos.y - 0.15 || box.minY > seg.pos.y + height) {
            return false;
        }
        double normalPadding = getHorizontalHalfExtent(box, seg.outward) + 0.08;
        double distToPlane = Math.abs(rel.dotProduct(seg.outward));
        return distToPlane <= GRACE_WALL_THICKNESS + normalPadding;
    }

    private static boolean pushOffGraceSegment(ServerWorld world, Entity entity, GraceSegment seg, double height) {
        if (!isInsideGraceSegment(entity.getPos(), entity.getBoundingBox(), seg, height)) {
            return false;
        }
        double side = seg.outward.dotProduct(entity.getPos().subtract(seg.pos));
        double sideSign;
        if (side > 1.0E-4) {
            sideSign = 1.0;
        } else if (side < -1.0E-4) {
            sideSign = -1.0;
        } else {
            sideSign = entity.getVelocity().dotProduct(seg.outward) >= 0.0 ? 1.0 : -1.0;
        }
        double halfExtent = getHorizontalHalfExtent(entity.getBoundingBox(), seg.outward);
        double allowed = GRACE_WALL_THICKNESS + halfExtent;
        double penetration = allowed - Math.abs(side);
        if (penetration <= 0.0) {
            return false;
        }
        Vec3d corrected = entity.getPos().add(seg.outward.multiply(sideSign * (penetration + 0.04)));
        if (entity instanceof ServerPlayerEntity player) {
            player.networkHandler.requestTeleport(corrected.x, corrected.y, corrected.z, player.getYaw(), player.getPitch());
        } else {
            entity.requestTeleport(corrected.x, corrected.y, corrected.z);
        }
        Vec3d vel = entity.getVelocity();
        double normalComponent = vel.dotProduct(seg.outward);
        if (sideSign * normalComponent < 0.0) {
            entity.setVelocity(vel.subtract(seg.outward.multiply(normalComponent)));
            entity.velocityDirty = true;
            if (entity instanceof ServerPlayerEntity player) {
                NetworkCompat.sendVelocityUpdate(player);
            }
        }
        return true;
    }

    private static double getHorizontalHalfExtent(Box box, Vec3d axis) {
        double halfX = (box.maxX - box.minX) * 0.5;
        double halfZ = (box.maxZ - box.minZ) * 0.5;
        return Math.abs(axis.x) * halfX + Math.abs(axis.z) * halfZ;
    }

    private record GraceSegment(Vec3d pos, Vec3d outward, Vec3d tangent) {
    }

    private static void applyPainFissure(ServerWorld world, LivingEntity owner, ActiveSpikeField field) {
        Vec3d dir = field.fissureDirection;
        float damage = field.tuning().damage() * Math.max(0.75F, painWaveDamageMultiplier());
        for (int step = 0; step <= PAIN_FISSURE_STEPS; step++) {
            Vec3d pos = field.center().add(dir.multiply(step * PAIN_FISSURE_STEP));
            Box hitBox = Box.of(pos.add(0.0, 0.35, 0.0), PAIN_FISSURE_HIT_RADIUS * 2.0, 2.2, PAIN_FISSURE_HIT_RADIUS * 2.0);
            for (LivingEntity candidate : world.getEntitiesByClass(
                    LivingEntity.class,
                    hitBox,
                    entity -> CombatTargeting.isOffensiveTargetCandidate(entity, owner)
            )) {
                boolean damaged = CombatTargeting.applyDamage(world, owner, candidate, damage, false, false);
                if (damaged) {
                    candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), owner);
                    applyUpwardKnockback(candidate, Math.min(0.22, field.tuning().upwardKnockback() * 0.55));
                }
            }
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.POINTED_DRIPSTONE.getDefaultState()), pos.x, pos.y + 0.2, pos.z, 4, 0.18, 0.08, 0.18, 0.005);
        }
        world.playSound(null, field.center().x, field.center().y, field.center().z, SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.PLAYERS, 0.9F, 0.7F);
    }

    private static void tickBountyPulses(ServerWorld world, ActiveSpikeField field) {
        if (!field.tuning().bountyCenterSpike() || field.bountyPulsesRemaining <= 0) {
            return;
        }
        if (world.getTime() < field.nextBountyPulseTick) {
            return;
        }
        LivingEntity owner = getOwnerEntity(world, field.ownerId());
        // Reset visuals so spikes push up again.
        removeField(world, field);
        field.visuals.clear();
        List<SpikePoint> points = buildPatchPoints(world, field.center(), field.tuning());
        spawnSpikeVisuals(world, field, points);
        spawnBountyCenterSpikeVisuals(world, field);
        applyBountyCenterImpact(world, owner, field.center(), field.tuning());
        world.playSound(null, field.center().x, field.center().y, field.center().z, SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.PLAYERS, 0.85F, 0.8F + world.random.nextFloat() * 0.15F);
        spawnBurstParticles(world, field.center());
        field.bountyPulsesRemaining--;
        field.nextBountyPulseTick = world.getTime() + FIELD_DURATION_TICKS + BOUNTY_PULSE_GAP_TICKS;
    }

    private static void spawnGraceGroundMarker(ServerWorld world, ActiveSpikeField field) {
        if ((world.getTime() - field.spawnTick()) % 4L != 0L) {
            return;
        }
        double radius = field.tuning().radius();
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.getDefaultState()),
                field.center().x, field.center().y + 0.05, field.center().z, 6, radius * 0.45, 0.04, radius * 0.45, 0.0);
        world.spawnParticles(ParticleTypes.ENCHANT,
                field.center().x, field.center().y + 0.15, field.center().z, 3, radius * 0.4, 0.08, radius * 0.4, 0.0);
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                field.center().x, field.center().y + 0.02, field.center().z, 2, radius * 0.35, 0.02, radius * 0.35, 0.0);
    }

    private static void spawnSpikeVisual(ServerWorld world, ActiveSpikeField field, double x, double y, double z, int heightSegments, long spawnTick) {
        float targetHeight = (float) (heightSegments * SPIKE_SEGMENT_HEIGHT);
        EarthSpikeVisualEntity visual = new EarthSpikeVisualEntity(world, x, y - START_DEPTH, z, targetHeight);
        visual.addCommandTag(SPIKE_VISUAL_TAG);
        world.spawnEntity(visual);
        field.visuals.add(new SpikeVisual(visual.getUuid(), x, y, z, spawnTick));
    }

    private static LivingEntity getOwnerEntity(ServerWorld world, UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        Entity entity = world.getEntity(ownerId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void applyUpwardKnockback(LivingEntity target, double upwardKnockback) {
        if (upwardKnockback <= 0.0) {
            return;
        }
        target.addVelocity(0.0, upwardKnockback, 0.0);
        target.setOnGround(false);
        target.velocityDirty = true;
        if (target instanceof ServerPlayerEntity player) {
            NetworkCompat.sendVelocityUpdate(player);
        }
    }

    private static float getHeightScale(long age) {
        if (age < 0) {
            return 0.0F;
        }
        if (age < RAISE_TICKS) {
            float t = MathHelper.clamp((float) age / (float) RAISE_TICKS, 0.0F, 1.0F);
            return easeOutBack(t);
        }
        if (age < RAISE_TICKS + HOLD_TICKS) {
            return 1.0F;
        }
        long sinkAge = age - RAISE_TICKS - HOLD_TICKS;
        if (sinkAge < SINK_TICKS) {
            float t = MathHelper.clamp((float) sinkAge / (float) SINK_TICKS, 0.0F, 1.0F);
            return 1.0F - (t * t * t);
        }
        return 0.0F;
    }

    private static double getVerticalOffset(long age) {
        if (age < RAISE_TICKS) {
            float t = MathHelper.clamp((float) age / (float) RAISE_TICKS, 0.0F, 1.0F);
            return -START_DEPTH + (START_DEPTH * easeOutBack(t));
        }
        if (age < RAISE_TICKS + HOLD_TICKS) {
            return 0.0;
        }
        long sinkAge = age - RAISE_TICKS - HOLD_TICKS;
        if (sinkAge < SINK_TICKS) {
            float t = MathHelper.clamp((float) sinkAge / (float) SINK_TICKS, 0.0F, 1.0F);
            return -(START_DEPTH * t * t * t);
        }
        return -START_DEPTH;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float p = t - 1.0F;
        return 1.0F + c3 * p * p * p + c1 * p * p;
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

    private record SpikePoint(double x, double y, double z, int heightSegments) {
    }

    private static void removeField(ServerWorld world, ActiveSpikeField field) {
        for (SpikeVisual visual : field.visuals) {
            Entity entity = world.getEntity(visual.id());
            if (entity != null) {
                entity.discard();
            }
        }
    }

    private static void purgeOrphanSpikeVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof EarthSpikeVisualEntity && entity.getCommandTags().contains(SPIKE_VISUAL_TAG)) {
                entity.discard();
            }
            if (entity instanceof IceChaosWallVisualEntity && entity.getCommandTags().contains(GRACE_WALL_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static final class ActiveSpikeField {
        private final Vec3d center;
        private final long spawnTick;
        private final long expiryTick;
        private final UUID ownerId;
        private final FieldTuning tuning;
        private final Vec3d fissureDirection;
        private final List<SpikeVisual> visuals = new ArrayList<>();
        private final List<GraceSegment> graceSegments = new ArrayList<>();
        private int bountyPulsesRemaining;
        private long nextBountyPulseTick;

        private ActiveSpikeField(Vec3d center, long spawnTick, long expiryTick, UUID ownerId, FieldTuning tuning, Vec3d fissureDirection) {
            this.center = center;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
            this.ownerId = ownerId;
            this.tuning = tuning;
            this.fissureDirection = fissureDirection == null ? new Vec3d(1.0, 0.0, 0.0) : fissureDirection;
        }

        private Vec3d center() {
            return this.center;
        }

        private long spawnTick() {
            return this.spawnTick;
        }

        private long expiryTick() {
            return this.expiryTick;
        }

        private UUID ownerId() {
            return this.ownerId;
        }

        private FieldTuning tuning() {
            return this.tuning;
        }
    }

    private record SpikeVisual(UUID id, double baseX, double baseY, double baseZ, long spawnTick) {
    }

    private record FieldTuning(
            double radius,
            double visualRadius,
            int points,
            float damage,
            boolean directedFissure,
            double painWaveDistance,
            double upwardKnockback,
            boolean graceSupport,
            boolean bountyCenterSpike,
            int centerSpikeHeightSegments
    ) {
    }
}
