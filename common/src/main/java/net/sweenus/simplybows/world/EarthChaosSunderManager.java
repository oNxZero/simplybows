package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.BlockStateParticleEffect;
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
import net.sweenus.simplybows.entity.EarthSpikeVisualEntity;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EarthChaosSunderManager {

    private static final Map<ServerWorld, List<ActiveSunderField>> ACTIVE_FIELDS = new HashMap<>();
    private static final Map<ServerWorld, Long> NEXT_ORPHAN_VISUAL_CLEANUP_TICK = new HashMap<>();
    private static final double SUNDER_STEP_DISTANCE = 0.25;
    private static final double SUNDER_HIT_RADIUS = 0.9;
    private static final long TARGET_DAMAGE_COOLDOWN_TICKS = 20L;
    private static final long TARGET_REACQUIRE_COOLDOWN_TICKS = 60L;
    private static final long IDLE_TARGET_CHECK_INTERVAL_TICKS = 15L;
    private static final long MOVEMENT_SOUND_INTERVAL_TICKS = 3L;
    private static final long VISUAL_SPAWN_INTERVAL_TICKS = 2L;
    private static final String SUNDER_VISUAL_TAG = "simplybows_earth_chaos_sunder_visual";
    private static final int GROUND_SCAN_UP = 4;
    private static final int GROUND_SCAN_DOWN = 14;
    private static final double VISUAL_BASE_GROUND_OFFSET = -0.18;
    private static final double VISUAL_START_DEPTH = 2.4;
    private static final int VISUAL_RISE_TICKS = 5;
    private static final int VISUAL_HOLD_TICKS = 2;
    private static final int VISUAL_SINK_TICKS = 5;
    private static final int VISUAL_LIFETIME_TICKS = VISUAL_RISE_TICKS + VISUAL_HOLD_TICKS + VISUAL_SINK_TICKS;
    private static final long ORPHAN_VISUAL_CLEANUP_INTERVAL_TICKS = 100L;
    private static final double ORPHAN_VISUAL_CLEANUP_PLAYER_RADIUS = 192.0;

    private EarthChaosSunderManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveSunderField> fields = ACTIVE_FIELDS.get(world);
        return (fields != null && !fields.isEmpty()) || (world.getTime() % 20L == 0L);
    }

    public static boolean isSunderReady(ServerWorld world, UUID ownerId) {
        if (world == null || ownerId == null) {
            return false;
        }
        List<ActiveSunderField> fields = ACTIVE_FIELDS.get(world);
        if (fields == null || fields.isEmpty()) {
            return true;
        }
        long now = world.getTime();
        for (ActiveSunderField field : fields) {
            if (ownerId.equals(field.ownerId) && now < field.expiryTick) {
                return false;
            }
        }
        return true;
    }

    public static void spawnAtImpact(ServerWorld world, Vec3d startPos, UUID ownerId, int stringLevel, int frameLevel, Vec3d initialVelocity) {
        if (world == null || startPos == null) {
            return;
        }

        Vec3d direction = resolveInitialDirection(world, initialVelocity);
        int durationTicks = Math.max(20,
                SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderDurationTicks.get()
                        + Math.max(0, stringLevel) * SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderDurationPerStringTicks.get());
        double acquisitionRange = Math.max(1.5,
                SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderAcquisitionRange.get()
                        + Math.max(0, frameLevel) * SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderAcquisitionRangePerFrame.get());

        if (ownerId != null) {
            List<ActiveSunderField> existing = ACTIVE_FIELDS.get(world);
            if (existing != null && !existing.isEmpty()) {
                existing.removeIf(field -> {
                    if (ownerId.equals(field.ownerId)) {
                        removeFieldVisuals(world, field);
                        return true;
                    }
                    return false;
                });
            }
        }

        ActiveSunderField field = new ActiveSunderField(
                startPos,
                direction,
                ownerId,
                world.getTime() + durationTicks,
                acquisitionRange,
                frameLevel
        );
        ACTIVE_FIELDS.computeIfAbsent(world, w -> new ArrayList<>()).add(field);

        world.playSound(null, startPos.x, startPos.y, startPos.z, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1.0F, 0.85F);
    }

    public static void tick(ServerWorld world) {
        List<ActiveSunderField> fields = ACTIVE_FIELDS.get(world);
        if (fields == null || fields.isEmpty()) {
            cleanupOrphanedSunderVisuals(world);
            return;
        }

        Iterator<ActiveSunderField> iterator = fields.iterator();
        while (iterator.hasNext()) {
            ActiveSunderField field = iterator.next();
            if (world.getTime() >= field.expiryTick) {
                removeFieldVisuals(world, field);
                iterator.remove();
                continue;
            }

            tickField(world, field);
        }

        if (fields.isEmpty()) {
            ACTIVE_FIELDS.remove(world);
        }
    }

    private static void cleanupOrphanedSunderVisuals(ServerWorld world) {
        long now = world.getTime();
        long nextCleanupTick = NEXT_ORPHAN_VISUAL_CLEANUP_TICK.getOrDefault(world, 0L);
        if (now < nextCleanupTick) {
            return;
        }
        NEXT_ORPHAN_VISUAL_CLEANUP_TICK.put(world, now + ORPHAN_VISUAL_CLEANUP_INTERVAL_TICKS);

        Set<UUID> cleaned = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            Box searchBox = player.getBoundingBox().expand(ORPHAN_VISUAL_CLEANUP_PLAYER_RADIUS);
            for (EarthSpikeVisualEntity visual : world.getEntitiesByClass(
                    EarthSpikeVisualEntity.class,
                    searchBox,
                    entity -> entity.getCommandTags().contains(SUNDER_VISUAL_TAG)
            )) {
                if (cleaned.add(visual.getUuid())) {
                    visual.discard();
                }
            }
        }
    }

    public static void clearWorld(ServerWorld world) {
        ACTIVE_FIELDS.remove(world);
        NEXT_ORPHAN_VISUAL_CLEANUP_TICK.remove(world);
        for (EarthSpikeVisualEntity visual : world.getEntitiesByClass(
                EarthSpikeVisualEntity.class,
                new Box(
                        world.getWorldBorder().getBoundWest(), world.getBottomY(), world.getWorldBorder().getBoundNorth(),
                        world.getWorldBorder().getBoundEast(), world.getTopY(), world.getWorldBorder().getBoundSouth()
                ),
                entity -> entity.getCommandTags().contains(SUNDER_VISUAL_TAG)
        )) {
            if (visual.isAlive()) {
                visual.discard();
            }
        }
    }

    private static void tickField(ServerWorld world, ActiveSunderField field) {
        Vec3d previous = field.position;
        field.position = field.position.add(field.direction.multiply(SUNDER_STEP_DISTANCE));
        LivingEntity currentTarget = getValidCurrentTarget(world, field);
        if (currentTarget != null) {
            steerTowardTarget(field, currentTarget);
        }

        if (world.getTime() >= field.nextMovementSoundTick) {
            world.playSound(null, field.position.x, field.position.y, field.position.z, SoundEvents.BLOCK_DEEPSLATE_BRICKS_BREAK, SoundCategory.PLAYERS, 0.7F, 0.75F + world.random.nextFloat() * 0.15F);
            field.nextMovementSoundTick = world.getTime() + MOVEMENT_SOUND_INTERVAL_TICKS;
        }

        if (world.getTime() >= field.nextVisualSpawnTick) {
            spawnSunderSpikeVisual(world, field, world.getTime());
            field.nextVisualSpawnTick = world.getTime() + VISUAL_SPAWN_INTERVAL_TICKS;
        }
        animateSunderVisuals(world, field);

        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.DRIPSTONE_BLOCK.getDefaultState()), field.position.x, field.position.y + 0.08, field.position.z, 5, 0.22, 0.08, 0.22, 0.01);
        world.spawnParticles(ParticleTypes.POOF, field.position.x, field.position.y + 0.1, field.position.z, 2, 0.12, 0.05, 0.12, 0.0);

        LivingEntity owner = getOwnerEntity(world, field.ownerId);
        float damage = (float) (SimplyBowsConfig.INSTANCE.tremorstrike.spikeDamage.get() * (1.0 + field.frameLevel * SimplyBowsConfig.INSTANCE.upgrades.damageMultiplierPerFrame.get()));

        Box damageBox = Box.of(field.position, SUNDER_HIT_RADIUS * 2.0, 2.0, SUNDER_HIT_RADIUS * 2.0)
                .union(Box.of(previous, SUNDER_HIT_RADIUS * 2.0, 2.0, SUNDER_HIT_RADIUS * 2.0));

        LivingEntity redirectSource = null;
        boolean damagedCurrentTarget = false;
        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                damageBox,
                entity -> CombatTargeting.isOffensiveTargetCandidate(entity, owner)
        )) {
            UUID candidateId = candidate.getUuid();
            Long lastDamageTick = field.recentDamageTicks.get(candidateId);
            if (lastDamageTick != null && world.getTime() - lastDamageTick < TARGET_DAMAGE_COOLDOWN_TICKS) {
                continue;
            }
            if (candidate.squaredDistanceTo(field.position) > (SUNDER_HIT_RADIUS * SUNDER_HIT_RADIUS)
                    && candidate.squaredDistanceTo(previous) > (SUNDER_HIT_RADIUS * SUNDER_HIT_RADIUS)) {
                continue;
            }

            boolean damaged = CombatTargeting.applyDamage(world, owner, candidate, damage, true, false);
            if (!damaged) {
                continue;
            }

            field.recentDamageTicks.put(candidateId, world.getTime());
            if (field.currentTargetId != null && field.currentTargetId.equals(candidateId)) {
                damagedCurrentTarget = true;
            }

            if (redirectSource == null) {
                redirectSource = candidate;
            }
        }

        if (redirectSource != null) {
            if (currentTarget != null && !damagedCurrentTarget) {
                return;
            }
            retargetOrBranch(world, field, redirectSource);
        } else if (world.getTime() >= field.nextIdleTargetCheckTick) {
            if (currentTarget != null) {
                return;
            }
            attemptIdleRetarget(world, field);
            field.nextIdleTargetCheckTick = world.getTime() + IDLE_TARGET_CHECK_INTERVAL_TICKS;
        }
    }

    private static void attemptIdleRetarget(ServerWorld world, ActiveSunderField field) {
        LivingEntity nextTarget = findNextTargetNear(world, field, field.position, null);
        if (nextTarget == null) {
            return;
        }

        Vec3d desired = nextTarget.getPos().add(0.0, nextTarget.getStandingEyeHeight() * 0.35, 0.0).subtract(field.position);
        if (desired.lengthSquared() <= 1.0E-6) {
            return;
        }

        field.direction = desired.normalize();
        field.currentTargetId = nextTarget.getUuid();
        field.recentTargetTicks.put(nextTarget.getUuid(), world.getTime());
        world.playSound(null, field.position.x, field.position.y, field.position.z, SoundEvents.BLOCK_DEEPSLATE_BRICKS_BREAK, SoundCategory.PLAYERS, 0.45F, 1.25F + world.random.nextFloat() * 0.1F);
    }

    private static void retargetOrBranch(ServerWorld world, ActiveSunderField field, LivingEntity source) {
        LivingEntity nextTarget = findNextTarget(world, field, source);
        if (nextTarget != null) {
            Vec3d desired = nextTarget.getPos().add(0.0, nextTarget.getStandingEyeHeight() * 0.35, 0.0).subtract(field.position);
            if (desired.lengthSquared() > 1.0E-6) {
                field.direction = desired.normalize();
                field.currentTargetId = nextTarget.getUuid();
                field.recentTargetTicks.put(nextTarget.getUuid(), world.getTime());
                world.playSound(null, field.position.x, field.position.y, field.position.z, SoundEvents.BLOCK_DEEPSLATE_BRICKS_BREAK, SoundCategory.PLAYERS, 0.6F, 1.15F + world.random.nextFloat() * 0.15F);
                return;
            }
        }

        field.currentTargetId = null;
        field.direction = randomBranchDirection(world, field.direction);
        world.playSound(null, field.position.x, field.position.y, field.position.z, SoundEvents.BLOCK_DRIPSTONE_BLOCK_STEP, SoundCategory.PLAYERS, 0.5F, 0.8F + world.random.nextFloat() * 0.2F);
    }

    private static LivingEntity getValidCurrentTarget(ServerWorld world, ActiveSunderField field) {
        if (field.currentTargetId == null) {
            return null;
        }
        Entity entity = world.getEntity(field.currentTargetId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            field.currentTargetId = null;
            return null;
        }
        LivingEntity owner = getOwnerEntity(world, field.ownerId);
        if (!CombatTargeting.isOffensiveTargetCandidate(living, owner)) {
            field.currentTargetId = null;
            return null;
        }
        if (living.squaredDistanceTo(field.position) > field.acquisitionRange * field.acquisitionRange) {
            field.currentTargetId = null;
            return null;
        }
        return living;
    }

    private static void steerTowardTarget(ActiveSunderField field, LivingEntity target) {
        Vec3d desired = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.35, 0.0).subtract(field.position);
        if (desired.lengthSquared() <= 1.0E-6) {
            return;
        }
        field.direction = desired.normalize();
    }

    private static LivingEntity findNextTarget(ServerWorld world, ActiveSunderField field, LivingEntity source) {
        return findNextTargetNear(world, field, source.getPos(), source.getUuid());
    }

    private static LivingEntity findNextTargetNear(ServerWorld world, ActiveSunderField field, Vec3d centerPos, UUID excludedEntityId) {
        double range = field.acquisitionRange;
        Box box = Box.of(centerPos, range * 2.0, 4.0, range * 2.0);
        long now = world.getTime();
        LivingEntity owner = getOwnerEntity(world, field.ownerId);
        field.recentTargetTicks.entrySet().removeIf(entry -> now - entry.getValue() >= TARGET_REACQUIRE_COOLDOWN_TICKS);

        LivingEntity nearest = null;
        double bestSq = Double.MAX_VALUE;

        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                box,
                entity -> CombatTargeting.isOffensiveTargetCandidate(entity, owner)
        )) {
            UUID id = candidate.getUuid();
            if (excludedEntityId != null && id.equals(excludedEntityId)) {
                continue;
            }
            Long lastTargetTick = field.recentTargetTicks.get(id);
            if (lastTargetTick != null && now - lastTargetTick < TARGET_REACQUIRE_COOLDOWN_TICKS) {
                continue;
            }
            double sq = candidate.getPos().squaredDistanceTo(centerPos);
            if (sq <= range * range && sq < bestSq) {
                bestSq = sq;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static Vec3d resolveInitialDirection(ServerWorld world, Vec3d initialVelocity) {
        if (initialVelocity != null && initialVelocity.lengthSquared() > 1.0E-6) {
            return initialVelocity.normalize();
        }
        float yaw = world.random.nextFloat() * 360.0F;
        return Vec3d.fromPolar(0.0F, yaw).normalize();
    }

    private static Vec3d randomBranchDirection(ServerWorld world, Vec3d current) {
        Vec3d base = (current == null || current.lengthSquared() <= 1.0E-6) ? new Vec3d(1.0, 0.0, 0.0) : current.normalize();
        double yaw = Math.atan2(base.z, base.x);
        double randomTurn = MathHelper.lerp(world.random.nextDouble(), Math.toRadians(35.0), Math.toRadians(120.0));
        if (world.random.nextBoolean()) {
            randomTurn = -randomTurn;
        }
        double newYaw = yaw + randomTurn;
        return new Vec3d(Math.cos(newYaw), 0.0, Math.sin(newYaw)).normalize();
    }

    private static LivingEntity getOwnerEntity(ServerWorld world, UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        Entity entity = world.getEntity(ownerId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void spawnSunderSpikeVisual(ServerWorld world, ActiveSunderField field, long now) {
        Vec3d position = field.position;
        double groundY = findGroundTopY(world, position.x, position.z, position.y) + VISUAL_BASE_GROUND_OFFSET;
        float targetHeight = 1.6F + world.random.nextFloat() * 0.9F;
        EarthSpikeVisualEntity visual = new EarthSpikeVisualEntity(world, position.x, groundY - VISUAL_START_DEPTH, position.z, targetHeight);
        visual.addCommandTag(SUNDER_VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            return;
        }
        field.visuals.add(new SunderVisual(visual.getUuid(), position.x, groundY, position.z, now));
    }

    private static void animateSunderVisuals(ServerWorld world, ActiveSunderField field) {
        field.visuals.removeIf(visual -> {
            Entity entity = world.getEntity(visual.id());
            if (!(entity instanceof EarthSpikeVisualEntity spikeVisual)) {
                return true;
            }

            long age = world.getTime() - visual.spawnTick();
            if (age >= VISUAL_LIFETIME_TICKS) {
                spikeVisual.discard();
                return true;
            }

            float scale = getVisualHeightScale(age);
            spikeVisual.setHeightScale(scale);
            spikeVisual.setPos(visual.baseX(), visual.baseY() + getVisualVerticalOffset(age), visual.baseZ());
            return false;
        });
    }

    private static void removeFieldVisuals(ServerWorld world, ActiveSunderField field) {
        for (SunderVisual visual : field.visuals) {
            Entity entity = world.getEntity(visual.id());
            if (entity != null) {
                entity.discard();
            }
        }
        field.visuals.clear();
    }

    private static float getVisualHeightScale(long age) {
        if (age < VISUAL_RISE_TICKS) {
            float t = MathHelper.clamp((float) age / (float) VISUAL_RISE_TICKS, 0.0F, 1.0F);
            return easeOutBack(t);
        }
        if (age < VISUAL_RISE_TICKS + VISUAL_HOLD_TICKS) {
            return 1.0F;
        }
        long sinkAge = age - VISUAL_RISE_TICKS - VISUAL_HOLD_TICKS;
        if (sinkAge < VISUAL_SINK_TICKS) {
            float t = MathHelper.clamp((float) sinkAge / (float) VISUAL_SINK_TICKS, 0.0F, 1.0F);
            return 1.0F - (t * t * t);
        }
        return 0.0F;
    }

    private static double getVisualVerticalOffset(long age) {
        if (age < VISUAL_RISE_TICKS) {
            float t = MathHelper.clamp((float) age / (float) VISUAL_RISE_TICKS, 0.0F, 1.0F);
            return -VISUAL_START_DEPTH + (VISUAL_START_DEPTH * easeOutBack(t));
        }
        if (age < VISUAL_RISE_TICKS + VISUAL_HOLD_TICKS) {
            return 0.0;
        }
        long sinkAge = age - VISUAL_RISE_TICKS - VISUAL_HOLD_TICKS;
        if (sinkAge < VISUAL_SINK_TICKS) {
            float t = MathHelper.clamp((float) sinkAge / (float) VISUAL_SINK_TICKS, 0.0F, 1.0F);
            return -(VISUAL_START_DEPTH * t * t * t);
        }
        return -VISUAL_START_DEPTH;
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

    private static final class ActiveSunderField {
        private Vec3d position;
        private Vec3d direction;
        private final UUID ownerId;
        private final long expiryTick;
        private final double acquisitionRange;
        private final int frameLevel;
        private final Map<UUID, Long> recentDamageTicks = new HashMap<>();
        private final Map<UUID, Long> recentTargetTicks = new HashMap<>();
        private final List<SunderVisual> visuals = new ArrayList<>();
        private UUID currentTargetId;
        private long nextIdleTargetCheckTick;
        private long nextMovementSoundTick;
        private long nextVisualSpawnTick;

        private ActiveSunderField(Vec3d position, Vec3d direction, UUID ownerId, long expiryTick, double acquisitionRange, int frameLevel) {
            this.position = position;
            this.direction = direction;
            this.ownerId = ownerId;
            this.expiryTick = expiryTick;
            this.acquisitionRange = acquisitionRange;
            this.frameLevel = frameLevel;
            this.currentTargetId = null;
            this.nextIdleTargetCheckTick = 0L;
            this.nextMovementSoundTick = 0L;
            this.nextVisualSpawnTick = 0L;
        }
    }

    private record SunderVisual(UUID id, double baseX, double baseY, double baseZ, long spawnTick) {
    }
}
