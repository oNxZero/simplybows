package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
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
    private static final int PAIN_STAR_RAYS = 8;
    private static final int PAIN_WAVE_STEP_TICKS = 1;
    private static final double PAIN_WAVE_DAMAGE_RADIUS = 0.8;
    private static final double BOUNTY_CENTER_KNOCKBACK_BASE_MULTIPLIER = 1.15;
    private static final double BOUNTY_CENTER_KNOCKBACK_PROXIMITY_MULTIPLIER = 1.9;
    private static final String SPIKE_VISUAL_TAG = "simplybows_earth_spike_visual";

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
        createOrReplaceField(world, center, owner, BowUpgradeData.none());
    }

    public static void createOrReplaceField(ServerWorld world, Vec3d center, Entity owner, BowUpgradeData upgrades) {
        List<ActiveSpikeField> fields = ACTIVE_FIELDS.computeIfAbsent(world, w -> new ArrayList<>());
        UUID ownerId = owner != null ? owner.getUuid() : null;
        if (ownerId != null && !isFieldReady(world, ownerId, fields)) {
            return;
        }

        long now = world.getTime();
        FieldTuning tuning = buildTuning(upgrades);
        List<SpikePoint> points = buildPatchPoints(world, center, tuning);
        int painTravelTicks = tuning.outwardPainWaves() ? getPainWaveMaxSteps(tuning) * PAIN_WAVE_STEP_TICKS : 0;
        ActiveSpikeField field = new ActiveSpikeField(center, now, now + FIELD_DURATION_TICKS + painTravelTicks, ownerId, tuning);
        spawnSpikeVisuals(world, field, points);
        if (tuning.bountyCenterSpike()) {
            spawnBountyCenterSpikeVisuals(world, field);
        }
        fields.add(field);
        if (ownerId != null) {
            int lockoutTicks = Math.max(20, fieldLockoutTicks());
            CooldownStorage.forWorld(FIELD_LOCKOUTS_BY_SERVER, world)
                    .put(ownerId, now + lockoutTicks);
            if (owner instanceof ServerPlayerEntity player) {
                SimplyBowItem.simplybows$sendCooldownPacket(player, "earth",
                        System.currentTimeMillis() + (long) lockoutTicks * 50L, lockoutTicks);
            }
        }

        LivingEntity ownerEntity = getOwnerEntity(world, ownerId);
        double initialKnock = tuning.outwardPainWaves() ? Math.min(0.22, tuning.upwardKnockback()) : tuning.upwardKnockback();
        applySpikeDamage(world, ownerEntity, center, tuning.radius(), tuning.damage(), initialKnock);
        if (tuning.bountyCenterSpike()) {
            applyBountyCenterImpact(world, ownerEntity, center, tuning);
        }
        if (tuning.graceSupport()) {
            applyGraceAllySupport(world, ownerEntity, center, tuning);
        }
        if (tuning.outwardPainWaves()) {
            initializePainWaves(field, now);
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
            tickPainWaves(world, field);
            animateField(world, field);
        }
    }

    private static boolean isFieldReady(ServerWorld world, UUID ownerId, List<ActiveSpikeField> fields) {
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
            float scaledDamage = tuning.damage() * (bountyCenterDamageBaseMultiplier() + (float) (proximity * bountyCenterDamageProximityMultiplier()));
            boolean damaged = CombatTargeting.applyDamage(world, owner, candidate, scaledDamage, true, false);
            if (damaged) {
                double scaledKnockup = tuning.upwardKnockback() * (BOUNTY_CENTER_KNOCKBACK_BASE_MULTIPLIER + (proximity * BOUNTY_CENTER_KNOCKBACK_PROXIMITY_MULTIPLIER));
                applyUpwardKnockback(candidate, scaledKnockup);
            }
        }
    }

    private static void applyGraceAllySupport(ServerWorld world, LivingEntity owner, Vec3d center, FieldTuning tuning) {
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
        boolean painWaves = upgrades.runeEtching() == RuneEtching.PAIN;
        boolean graceSupport = upgrades.runeEtching() == RuneEtching.GRACE;
        boolean bountyCenterSpike = upgrades.runeEtching() == RuneEtching.BOUNTY;
        double radius = fieldRadius() * sizeMultiplier + upgrades.stringLevel() * stringRadiusBonusPerLevel();
        double visualRadius = PATCH_VISUAL_RADIUS * sizeMultiplier + upgrades.stringLevel() * (stringRadiusBonusPerLevel() * 0.45);
        double painWaveDistance = painWaveMaxDistance() + upgrades.stringLevel() * stringWaveDistanceBonusPerLevel();
        double upwardKnockback = baseUpwardKnockback() + upgrades.frameLevel() * frameUpwardKnockbackPerLevel();
        int centerSpikeHeightSegments = bountyCenterBaseHeightSegments() + upgrades.frameLevel() * bountyCenterExtraHeightPerFrame();
        return new FieldTuning(
                radius,
                visualRadius,
                PATCH_VISUAL_POINTS + upgrades.stringLevel() * 5,
                damage,
                painWaves,
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

    private static void initializePainWaves(ActiveSpikeField field, long now) {
        for (int i = 0; i < PAIN_STAR_RAYS; i++) {
            double angle = ((Math.PI * 2.0) / PAIN_STAR_RAYS) * i;
            Vec3d direction = new Vec3d(Math.cos(angle), 0.0, Math.sin(angle));
            field.painWaves.add(new PainWaveState(direction, 1, now));
        }
    }

    private static void tickPainWaves(ServerWorld world, ActiveSpikeField field) {
        if (!field.tuning().outwardPainWaves() || field.painWaves.isEmpty()) {
            return;
        }

        int maxSteps = getPainWaveMaxSteps(field.tuning());
        field.painWaves.removeIf(wave -> {
            if (world.getTime() < wave.nextSpawnTick()) {
                return false;
            }
            if (wave.nextStep() > maxSteps) {
                return true;
            }

            double distance = wave.nextStep() * painWaveStepDistance();
            Vec3d pos = field.center().add(wave.direction().multiply(distance));
            double y = findGroundTopY(world, pos.x, pos.z, field.center().y) + BASE_GROUND_OFFSET;
            int heightSegments = 2 + (wave.nextStep() % 4);
            spawnSpikeVisual(world, field, pos.x, y, pos.z, heightSegments, world.getTime());
            damageAtWaveStep(world, field, getOwnerEntity(world, field.ownerId()), pos.x, y, pos.z, field.tuning().damage() * painWaveDamageMultiplier());
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.POINTED_DRIPSTONE.getDefaultState()), pos.x, y + 0.2, pos.z, 3, 0.1, 0.08, 0.1, 0.005);
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.DRIPSTONE_BLOCK.getDefaultState()), pos.x, y + 0.15, pos.z, 5, 0.16, 0.08, 0.16, 0.01);
            world.playSound(null, pos.x, y, pos.z, SoundEvents.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.PLAYERS, 0.45F, 1.05F + world.random.nextFloat() * 0.15F);

            wave.advance(PAIN_WAVE_STEP_TICKS);
            return wave.nextStep() > maxSteps;
        });
    }

    private static void spawnSpikeVisual(ServerWorld world, ActiveSpikeField field, double x, double y, double z, int heightSegments, long spawnTick) {
        float targetHeight = (float) (heightSegments * SPIKE_SEGMENT_HEIGHT);
        EarthSpikeVisualEntity visual = new EarthSpikeVisualEntity(world, x, y - START_DEPTH, z, targetHeight);
        visual.addCommandTag(SPIKE_VISUAL_TAG);
        world.spawnEntity(visual);
        field.visuals.add(new SpikeVisual(visual.getUuid(), x, y, z, spawnTick));
    }

    private static void damageAtWaveStep(ServerWorld world, ActiveSpikeField field, LivingEntity owner, double x, double y, double z, float damage) {
        Box hitBox = Box.of(new Vec3d(x, y + 0.3, z), PAIN_WAVE_DAMAGE_RADIUS * 2.0, 2.0, PAIN_WAVE_DAMAGE_RADIUS * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                hitBox,
                CombatTargeting::isOffensiveTargetCandidate
        )) {
            boolean damaged = CombatTargeting.applyDamage(world, owner, candidate, damage, true, false);
            if (damaged && field.painLaunched.add(candidate.getUuid())) {
                applyUpwardKnockback(candidate, 0.22);
            }
        }
    }

    private static LivingEntity getOwnerEntity(ServerWorld world, UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        Entity entity = world.getEntity(ownerId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static int getPainWaveMaxSteps(FieldTuning tuning) {
        return Math.max(1, (int) Math.floor(tuning.painWaveDistance() / painWaveStepDistance()));
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
        }
    }

    private static final class ActiveSpikeField {
        private final Vec3d center;
        private final long spawnTick;
        private final long expiryTick;
        private final UUID ownerId;
        private final FieldTuning tuning;
        private final List<SpikeVisual> visuals = new ArrayList<>();
        private final List<PainWaveState> painWaves = new ArrayList<>();
        private final java.util.Set<UUID> painLaunched = new java.util.HashSet<>();

        private ActiveSpikeField(Vec3d center, long spawnTick, long expiryTick, UUID ownerId, FieldTuning tuning) {
            this.center = center;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
            this.ownerId = ownerId;
            this.tuning = tuning;
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

    private static final class PainWaveState {
        private final Vec3d direction;
        private int nextStep;
        private long nextSpawnTick;

        private PainWaveState(Vec3d direction, int nextStep, long nextSpawnTick) {
            this.direction = direction;
            this.nextStep = nextStep;
            this.nextSpawnTick = nextSpawnTick;
        }

        private Vec3d direction() {
            return this.direction;
        }

        private int nextStep() {
            return this.nextStep;
        }

        private long nextSpawnTick() {
            return this.nextSpawnTick;
        }

        private void advance(int spawnIntervalTicks) {
            this.nextStep++;
            this.nextSpawnTick += spawnIntervalTicks;
        }
    }

    private record FieldTuning(
            double radius,
            double visualRadius,
            int points,
            float damage,
            boolean outwardPainWaves,
            double painWaveDistance,
            double upwardKnockback,
            boolean graceSupport,
            boolean bountyCenterSpike,
            int centerSpikeHeightSegments
    ) {
    }
}
