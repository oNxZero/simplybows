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
    private static final double SUNDER_ORBIT_RADIUS_BASE = 5.0; // wide rotating disc
    private static final double SUNDER_ORBIT_RADIUS_PER_STRING = 0.45;
    private static final double SUNDER_BAND_HALF_WIDTH = 1.55; // dense band — spikes packed tight
    private static final double SUNDER_HIT_RADIUS = 1.85; // per-front hit along the band
    private static final int SUNDER_FULL_ROTATIONS = 2;
    private static final int SUNDER_MIN_DURATION_TICKS = 200; // 10s
    private static final int SUNDER_MAX_DURATION_TICKS = 360; // 18s
    private static final java.util.Map<net.minecraft.server.MinecraftServer, java.util.Map<UUID, Long>> SUNDER_COOLDOWNS = CooldownStorage.newServerScopedStore();
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
        if (!RuneUseCooldown.isPlayerReady(world, ownerId)) {
            return false;
        }
        Long cooldownEnd = CooldownStorage.forWorld(SUNDER_COOLDOWNS, world).get(ownerId);
        if (cooldownEnd != null && world.getTime() < cooldownEnd) {
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

        int durationTicks = Math.max(SUNDER_MIN_DURATION_TICKS, Math.min(SUNDER_MAX_DURATION_TICKS,
                Math.max(SUNDER_MIN_DURATION_TICKS, SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderDurationTicks.get())
                        + Math.max(0, stringLevel) * Math.max(20, SimplyBowsConfig.INSTANCE.tremorstrike.chaosSunderDurationPerStringTicks.get())));
        double orbitRadius = SUNDER_ORBIT_RADIUS_BASE + Math.max(0, stringLevel) * SUNDER_ORBIT_RADIUS_PER_STRING;
        double startAngle = resolveInitialAngle(world, initialVelocity);

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

        Vec3d orbitCenter = new Vec3d(startPos.x, startPos.y, startPos.z);
        Vec3d firstPos = orbitCenter.add(Math.cos(startAngle) * orbitRadius, 0.0, Math.sin(startAngle) * orbitRadius);
        double angularSpeed = (Math.PI * 2.0 * SUNDER_FULL_ROTATIONS) / Math.max(1, durationTicks);
        ActiveSunderField field = new ActiveSunderField(
                orbitCenter,
                firstPos,
                startAngle,
                orbitRadius,
                angularSpeed,
                ownerId,
                world.getTime() + durationTicks,
                frameLevel
        );
        ACTIVE_FIELDS.computeIfAbsent(world, w -> new ArrayList<>()).add(field);
        if (ownerId != null) {
            int cooldownTicks = RuneUseCooldown.fromEffectDuration(durationTicks);
            CooldownStorage.forWorld(SUNDER_COOLDOWNS, world).put(
                    ownerId, world.getTime() + cooldownTicks);
            RuneUseCooldown.start(world, ownerId, "earth-chaos", "earth", cooldownTicks);
        }

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
        field.orbitAngle += field.angularSpeed;
        if (field.orbitAngle > Math.PI * 2.0) {
            field.orbitAngle -= Math.PI * 2.0;
        }
        // Circle around the impact point — never runs off into the void.
        field.position = field.orbitCenter.add(
                Math.cos(field.orbitAngle) * field.orbitRadius,
                0.0,
                Math.sin(field.orbitAngle) * field.orbitRadius
        );
        field.direction = field.position.subtract(previous);
        if (field.direction.lengthSquared() > 1.0E-6) {
            field.direction = field.direction.normalize();
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

        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.DRIPSTONE_BLOCK.getDefaultState()), field.position.x, field.position.y + 0.08, field.position.z, 10, SUNDER_BAND_HALF_WIDTH * 0.55, 0.08, SUNDER_BAND_HALF_WIDTH * 0.55, 0.01);
        world.spawnParticles(ParticleTypes.POOF, field.position.x, field.position.y + 0.1, field.position.z, 4, SUNDER_BAND_HALF_WIDTH * 0.4, 0.05, SUNDER_BAND_HALF_WIDTH * 0.4, 0.0);

        LivingEntity owner = getOwnerEntity(world, field.ownerId);
        // Slightly softer per hit — bigger denser ring covers more ground.
        float damage = (float) (SimplyBowsConfig.INSTANCE.tremorstrike.spikeDamage.get() * 0.52
                * (1.0 + field.frameLevel * SimplyBowsConfig.INSTANCE.upgrades.damageMultiplierPerFrame.get() * 0.5));

        double outer = field.orbitRadius + SUNDER_BAND_HALF_WIDTH + SUNDER_HIT_RADIUS;
        Box damageBox = Box.of(field.orbitCenter, outer * 2.0, 2.4, outer * 2.0);

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
            double dx = candidate.getX() - field.orbitCenter.x;
            double dz = candidate.getZ() - field.orbitCenter.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double inner = Math.max(0.4, field.orbitRadius - SUNDER_BAND_HALF_WIDTH);
            double bandOuter = field.orbitRadius + SUNDER_BAND_HALF_WIDTH;
            // Thick ring: must be in the radial band, and near the moving front (or previous).
            if (dist < inner || dist > bandOuter) {
                continue;
            }
            if (candidate.squaredDistanceTo(field.position) > (SUNDER_HIT_RADIUS * SUNDER_HIT_RADIUS * 2.25)
                    && candidate.squaredDistanceTo(previous) > (SUNDER_HIT_RADIUS * SUNDER_HIT_RADIUS * 2.25)) {
                continue;
            }

            boolean damaged = CombatTargeting.applyDamage(world, owner, candidate, damage, false, false);
            if (damaged) {
                field.recentDamageTicks.put(candidateId, world.getTime());
            }
        }
    }

    private static double resolveInitialAngle(ServerWorld world, Vec3d initialVelocity) {
        if (initialVelocity != null && initialVelocity.horizontalLengthSquared() > 1.0E-6) {
            return Math.atan2(initialVelocity.z, initialVelocity.x);
        }
        return world.random.nextDouble() * Math.PI * 2.0;
    }

    private static LivingEntity getOwnerEntity(ServerWorld world, UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        Entity entity = world.getEntity(ownerId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void spawnSunderSpikeVisual(ServerWorld world, ActiveSunderField field, long now) {
        Vec3d side = new Vec3d(-field.direction.z, 0.0, field.direction.x);
        if (side.lengthSquared() < 1.0E-4) {
            side = new Vec3d(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }
        // Dense packed front — reads as a rotating disc segment, not sparse poles.
        Vec3d inward = field.orbitCenter.subtract(field.position);
        Vec3d radial = inward.horizontalLengthSquared() > 1.0E-4
                ? new Vec3d(inward.x, 0.0, inward.z).normalize()
                : new Vec3d(1.0, 0.0, 0.0);
        double[] sideOffsets = {-0.95, -0.45, 0.0, 0.45, 0.95};
        double[] radialOffsets = {-SUNDER_BAND_HALF_WIDTH * 0.75, 0.0, SUNDER_BAND_HALF_WIDTH * 0.75};
        for (double sideOff : sideOffsets) {
            for (double radialOff : radialOffsets) {
                float height = (Math.abs(sideOff) < 0.1 && Math.abs(radialOff) < 0.2) ? 1.55F : 1.25F;
                spawnSunderSpikeAt(world, field, field.position.add(side.multiply(sideOff)).add(radial.multiply(radialOff)), now, height);
            }
        }
    }

    private static void spawnSunderSpikeAt(ServerWorld world, ActiveSunderField field, Vec3d position, long now, float height) {
        double groundY = findGroundTopY(world, position.x, position.z, position.y) + VISUAL_BASE_GROUND_OFFSET;
        EarthSpikeVisualEntity visual = new EarthSpikeVisualEntity(world, position.x, groundY - VISUAL_START_DEPTH, position.z, height);
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
        private final Vec3d orbitCenter;
        private Vec3d position;
        private Vec3d direction;
        private double orbitAngle;
        private final double orbitRadius;
        private final double angularSpeed;
        private final UUID ownerId;
        private final long expiryTick;
        private final int frameLevel;
        private final Map<UUID, Long> recentDamageTicks = new HashMap<>();
        private final List<SunderVisual> visuals = new ArrayList<>();
        private long nextMovementSoundTick;
        private long nextVisualSpawnTick;

        private ActiveSunderField(Vec3d orbitCenter, Vec3d position, double orbitAngle, double orbitRadius,
                                  double angularSpeed, UUID ownerId, long expiryTick, int frameLevel) {
            this.orbitCenter = orbitCenter;
            this.position = position;
            this.orbitAngle = orbitAngle;
            this.orbitRadius = orbitRadius;
            this.angularSpeed = angularSpeed;
            this.direction = new Vec3d(-Math.sin(orbitAngle), 0.0, Math.cos(orbitAngle));
            this.ownerId = ownerId;
            this.expiryTick = expiryTick;
            this.frameLevel = frameLevel;
            this.nextMovementSoundTick = 0L;
            this.nextVisualSpawnTick = 0L;
        }
    }

    private record SunderVisual(UUID id, double baseX, double baseY, double baseZ, long spawnTick) {
    }
}
