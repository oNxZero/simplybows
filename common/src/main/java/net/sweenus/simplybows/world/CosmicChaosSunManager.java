package net.sweenus.simplybows.world;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.CosmicOrbitVisualEntity;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.network.CelestialSwiftnessPayload;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.util.CelestialSwiftnessTracker;
import dev.architectury.networking.NetworkManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CosmicChaosSunManager {

    private static final String ORBITING_PROJECTILE_TAG = "simplybows_cosmic_chaos_orbit";
    private static final double ORPHAN_SUN_PROJECTILE_CLEANUP_RADIUS = 18.0;
    private static final double PROJECTILE_ORBIT_MIN_RADIUS = 1.45;
    private static final double PROJECTILE_ORBIT_MAX_RADIUS = 3.65;
    private static final int PROJECTILE_CAPTURE_SETTLE_TICKS = 10;
    private static final double PROJECTILE_SWEEP_CAPTURE_PADDING = 1.5;
    private static final List<ActiveSun> ACTIVE_SUNS = new ArrayList<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> SUN_COOLDOWNS_BY_SERVER = CooldownStorage.newServerScopedStore();

    private CosmicChaosSunManager() {
    }

    private static int durationTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.chaosSunDurationTicks.get(); }
    private static int maxDurationTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.chaosSunMaxDurationTicks.get(); }
    private static int cooldownTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.chaosSunCooldownTicks.get(); }
    private static double radius() { return SimplyBowsConfig.INSTANCE.cosmicBow.chaosSunRadius.get(); }
    private static int maxCapturedProjectiles() { return SimplyBowsConfig.INSTANCE.cosmicBow.chaosSunMaxCapturedProjectiles.get(); }
    private static int fireIntervalTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.chaosSunFireIntervalTicks.get(); }
    private static int durationBonusPerShotTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.chaosSunDurationBonusPerShotTicks.get(); }
    private static int swiftnessDurationTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.chaosCelestialSwiftnessDurationTicks.get(); }
    private static int maxSwiftnessStacks() { return SimplyBowsConfig.INSTANCE.cosmicBow.chaosCelestialSwiftnessMaxStacks.get(); }
    private static float bowPullBonusPerStack() { return SimplyBowsConfig.INSTANCE.cosmicBow.chaosCelestialBowPullBonusPerStack.get(); }

    public static boolean hasActive(ServerWorld world) {
        return ACTIVE_SUNS.stream().anyMatch(sun -> sun.world == world) || !getCooldowns(world).isEmpty() || world.getTime() % 20L == 0L;
    }

    public static boolean isManagedSunVisual(ServerWorld world, UUID visualId) {
        if (world == null || visualId == null) {
            return false;
        }
        return ACTIVE_SUNS.stream().anyMatch(sun -> sun.world == world && visualId.equals(sun.visualId));
    }

    public static void cleanupOrphanSunVisual(CosmicOrbitVisualEntity visual) {
        if (!(visual.getWorld() instanceof ServerWorld world) || isManagedSunVisual(world, visual.getUuid())) {
            return;
        }
        releaseNearbyOrphanProjectiles(world, visual.getPos());
        visual.discard();
    }

    public static boolean createSun(ServerWorld world, Entity owner, UUID snapshotOwnerId, Vec3d pos, BowUpgradeData upgrades) {
        if (world == null || pos == null) {
            return false;
        }
        UUID ownerId = snapshotOwnerId != null ? snapshotOwnerId : (owner == null ? null : owner.getUuid());
        if (ownerId == null || !isSunReady(world, ownerId)) {
            return false;
        }
        BowUpgradeData sunUpgrades = upgrades == null ? BowUpgradeData.none() : upgrades;
        int duration = Math.max(20, durationTicks());
        int maxDuration = Math.max(duration, maxDurationTicks());
        double sunRadius = Math.max(1.0, radius() + sunUpgrades.frameLevel());

        CosmicOrbitVisualEntity visual = new CosmicOrbitVisualEntity(world, pos.x, pos.y + 0.45, pos.z);
        visual.setSunMode(true);
        visual.setFieldRadius((float) Math.min(4.5, Math.max(1.4, sunRadius * 0.25)));
        visual.setLifetimeTicks(duration + 10);
        visual.setVisualScale(1.0F);
        if (!world.spawnEntity(visual)) {
            return false;
        }

        discardSunsOwnedBy(world, ownerId);
        ACTIVE_SUNS.add(new ActiveSun(
                world,
                visual.getUuid(),
                ownerId,
                pos.add(0.0, 0.55, 0.0),
                sunRadius,
                world.getTime(),
                world.getTime() + duration,
                world.getTime() + maxDuration,
                world.getTime() + Math.max(1, fireIntervalTicks())
        ));
        // Floor covering the sun's guaranteed life; finishSunCooldown extends it to cover however
        // long the sun actually ran, so the gap between suns never collapses to zero.
        int initialCooldown = duration + Math.max(0, cooldownTicks());
        startSunCooldown(world, ownerId, initialCooldown);
        if (owner instanceof ServerPlayerEntity player && initialCooldown > 0) {
            SimplyBowItem.simplybows$sendCooldownPacket(player, "cosmic", System.currentTimeMillis() + initialCooldown * 50L, initialCooldown);
        }
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.75F, 0.85F);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.55F, 1.25F);
        world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.65, pos.z, 40, 0.7, 0.45, 0.7, 0.035);
        world.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y + 0.65, pos.z, 24, 0.45, 0.32, 0.45, 0.02);
        return true;
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 40L == 0L) {
            long now = CooldownStorage.currentTick(world);
            getCooldowns(world).entrySet().removeIf(entry -> entry.getValue() <= now);
        }
        if (ACTIVE_SUNS.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<ActiveSun> iterator = ACTIVE_SUNS.iterator();
        while (iterator.hasNext()) {
            ActiveSun sun = iterator.next();
            if (sun.world != world) {
                continue;
            }
            Entity visualEntity = world.getEntity(sun.visualId);
            if (visualEntity == null || visualEntity.isRemoved() || now >= sun.expiresAt || !hasLivingOwner(world, sun)) {
                releaseOrbitingProjectiles(world, sun);
                if (visualEntity != null) {
                    visualEntity.discard();
                }
                finishSunCooldown(world, sun, now);
                iterator.remove();
                continue;
            }
            if (visualEntity instanceof CosmicOrbitVisualEntity visual) {
                visual.setLifetimeTicks((int) Math.max(1, sun.expiresAt - sun.spawnTick + 10));
            }

            updateProjectileOrbit(world, sun);
            rescheduleProjectileFire(sun, now);
            if (now >= sun.nextFireAt) {
                fireOneProjectile(world, sun);
                sun.nextFireAt = now + getDynamicFireDelay(sun, now);
            }
            if (now % 5L == 0L) {
                spawnAmbientParticles(world, sun);
            }
        }
    }

    private static boolean hasLivingOwner(ServerWorld world, ActiveSun sun) {
        LivingEntity owner = getOwner(world, sun);
        return owner != null && owner.isAlive();
    }

    private static void discardSunsOwnedBy(ServerWorld world, UUID ownerId) {
        ACTIVE_SUNS.removeIf(existing -> {
            if (existing.world != world || !ownerId.equals(existing.ownerId)) {
                return false;
            }
            releaseOrbitingProjectiles(world, existing);
            Entity existingVisual = world.getEntity(existing.visualId);
            if (existingVisual != null) {
                existingVisual.discard();
            }
            return true;
        });
    }

    private static void releaseOrbitingProjectiles(ServerWorld world, ActiveSun sun) {
        Iterator<Map.Entry<UUID, OrbitingProjectile>> iterator = sun.orbiters.entrySet().iterator();
        while (iterator.hasNext()) {
            UUID projectileId = iterator.next().getKey();
            Entity entity = world.getEntity(projectileId);
            iterator.remove();
            if (!(entity instanceof ProjectileEntity projectile) || projectile.isRemoved() || !projectile.isAlive()) {
                continue;
            }
            releaseCapturedProjectile(projectile);
        }
    }

    private static void releaseNearbyOrphanProjectiles(ServerWorld world, Vec3d center) {
        double radius = ORPHAN_SUN_PROJECTILE_CLEANUP_RADIUS;
        Box box = Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0);
        for (ProjectileEntity projectile : world.getEntitiesByClass(ProjectileEntity.class, box, projectile ->
                projectile.isAlive()
                        && !projectile.isRemoved()
                        && (projectile.getCommandTags().contains(ORBITING_PROJECTILE_TAG) || projectile.hasNoGravity()))) {
            releaseCapturedProjectile(projectile);
        }
    }

    private static void releaseCapturedProjectile(ProjectileEntity projectile) {
        Vec3d velocity = projectile.getVelocity();
        Vec3d releasedVelocity = new Vec3d(velocity.x * 0.16, Math.min(velocity.y * 0.16, -0.04), velocity.z * 0.16);
        projectile.removeScoreboardTag(ORBITING_PROJECTILE_TAG);
        projectile.setNoGravity(false);
        projectile.setVelocity(releasedVelocity);
        projectile.velocityModified = true;
    }

    private static void rescheduleProjectileFire(ActiveSun sun, long now) {
        if (sun.orbiters.isEmpty()) {
            sun.nextFireAt = now + Math.max(1, fireIntervalTicks());
            return;
        }
        long desiredFireAt = now + getDynamicFireDelay(sun, now);
        if (sun.nextFireAt > desiredFireAt) {
            sun.nextFireAt = desiredFireAt;
        }
    }

    private static int getDynamicFireDelay(ActiveSun sun, long now) {
        int configuredInterval = Math.max(1, fireIntervalTicks());
        int orbitingProjectiles = Math.max(1, sun.orbiters.size());
        long remainingTicks = Math.max(1L, sun.expiresAt - now);
        int intervalNeededToSpendAll = Math.max(1, (int) Math.floor((double) remainingTicks / orbitingProjectiles));
        return Math.max(1, Math.min(configuredInterval, intervalNeededToSpendAll));
    }

    public static float getCelestialBowPullMultiplier(LivingEntity entity) {
        if (entity == null) {
            return 1.0F;
        }
        int stacks = CelestialSwiftnessTracker.getStacks(entity.getUuid(), entity.getWorld().getTime());
        if (stacks <= 0) {
            return 1.0F;
        }
        return 1.0F + stacks * bowPullBonusPerStack();
    }

    public static void tryCaptureProjectile(ServerWorld world, ProjectileEntity projectile) {
        if (world == null || projectile == null || projectile.isRemoved() || !projectile.isAlive()) {
            return;
        }
        if (ACTIVE_SUNS.isEmpty()) {
            if (projectile.getCommandTags().contains(ORBITING_PROJECTILE_TAG)) {
                releaseCapturedProjectile(projectile);
            }
            return;
        }

        long now = world.getTime();
        for (ActiveSun sun : ACTIVE_SUNS) {
            if (sun.world != world) {
                continue;
            }
            Entity visualEntity = world.getEntity(sun.visualId);
            if (visualEntity == null || visualEntity.isRemoved() || now >= sun.expiresAt) {
                continue;
            }
            double captureRadius = sun.radius + PROJECTILE_SWEEP_CAPTURE_PADDING;
            if (sun.recentlyFired.containsKey(projectile.getUuid()) || !shouldCaptureProjectile(projectile, sun, captureRadius)) {
                continue;
            }
            if (!addOrbiter(world, sun, projectile)) {
                // This sun is full; let another sun in range have it rather than stalling it here.
                continue;
            }
            projectile.setNoGravity(true);
            projectile.setVelocity(Vec3d.ZERO);
            projectile.velocityModified = true;
            playOrbitCaptureSound(world, sun, projectile);
            return;
        }
    }

    private static void updateProjectileOrbit(ServerWorld world, ActiveSun sun) {
        long now = world.getTime();
        sun.recentlyFired.entrySet().removeIf(entry -> entry.getValue() <= now);
        double captureRadius = sun.radius + PROJECTILE_SWEEP_CAPTURE_PADDING;
        Box box = Box.of(sun.center, captureRadius * 2.0, captureRadius * 2.0, captureRadius * 2.0);
        for (Entity entity : world.getEntitiesByClass(Entity.class, box, entity ->
                entity instanceof ProjectileEntity
                        && entity.isAlive()
                        && !entity.isRemoved()
                        && !sun.recentlyFired.containsKey(entity.getUuid()))) {
            ProjectileEntity projectile = (ProjectileEntity) entity;
            if (shouldCaptureProjectile(projectile, sun, captureRadius) || isInsideCaptureBox(projectile, sun, captureRadius)) {
                addOrbiter(world, sun, projectile);
            }
        }

        Iterator<Map.Entry<UUID, OrbitingProjectile>> iterator = sun.orbiters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, OrbitingProjectile> entry = iterator.next();
            Entity entity = world.getEntity(entry.getKey());
            if (!(entity instanceof ProjectileEntity projectile) || !projectile.isAlive() || projectile.isRemoved()) {
                iterator.remove();
                continue;
            }
            OrbitingProjectile orbiter = entry.getValue();
            orbiter.age++;
            orbiter.angle += orbiter.speed;
            double y = sun.center.y + orbiter.heightOffset + Math.sin(orbiter.angle * 1.7 + orbiter.verticalPhase) * 0.38;
            Vec3d desired = new Vec3d(
                    sun.center.x + Math.cos(orbiter.angle) * orbiter.radius,
                    y,
                    sun.center.z + Math.sin(orbiter.angle) * orbiter.radius
            );
            double settle = Math.min(1.0, orbiter.age / (double) PROJECTILE_CAPTURE_SETTLE_TICKS);
            Vec3d next = projectile.getPos().lerp(desired, 0.25 + settle * 0.65);
            Vec3d velocity = next.subtract(projectile.getPos());
            projectile.setNoGravity(true);
            projectile.setPosition(next.x, next.y, next.z);
            projectile.setVelocity(velocity.x, velocity.y, velocity.z);
            projectile.velocityModified = true;

        }
    }

    private static boolean addOrbiter(ServerWorld world, ActiveSun sun, ProjectileEntity projectile) {
        UUID projectileId = projectile.getUuid();
        if (!sun.orbiters.containsKey(projectileId) && sun.orbiters.size() >= Math.max(1, maxCapturedProjectiles())) {
            return false;
        }
        sun.orbiters.computeIfAbsent(projectileId, ignored -> createOrbiter(world, sun, projectile));
        projectile.addCommandTag(ORBITING_PROJECTILE_TAG);
        return true;
    }

    private static boolean shouldCaptureProjectile(ProjectileEntity projectile, ActiveSun sun, double captureRadius) {
        double captureRadiusSq = captureRadius * captureRadius;
        if (projectile.squaredDistanceTo(sun.center) <= captureRadiusSq) {
            return true;
        }

        Vec3d previous = new Vec3d(projectile.prevX, projectile.prevY, projectile.prevZ);
        Vec3d current = projectile.getPos();
        Vec3d segment = current.subtract(previous);
        double segmentLengthSq = segment.lengthSquared();
        if (segmentLengthSq <= 1.0E-7) {
            return false;
        }

        double t = sun.center.subtract(previous).dotProduct(segment) / segmentLengthSq;
        t = Math.max(0.0, Math.min(1.0, t));
        Vec3d closest = previous.add(segment.multiply(t));
        return closest.squaredDistanceTo(sun.center) <= captureRadiusSq;
    }

    private static boolean isInsideCaptureBox(ProjectileEntity projectile, ActiveSun sun, double captureRadius) {
        Vec3d pos = projectile.getPos();
        return Math.abs(pos.x - sun.center.x) <= captureRadius
                && Math.abs(pos.y - sun.center.y) <= captureRadius
                && Math.abs(pos.z - sun.center.z) <= captureRadius;
    }

    private static OrbitingProjectile createOrbiter(ServerWorld world, ActiveSun sun, ProjectileEntity projectile) {
        Vec3d offset = projectile.getPos().subtract(sun.center);
        double horizontalDistance = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        double orbitRadius = Math.max(PROJECTILE_ORBIT_MIN_RADIUS, Math.min(PROJECTILE_ORBIT_MAX_RADIUS, horizontalDistance));
        double angle = horizontalDistance > 0.001 ? Math.atan2(offset.z, offset.x) : world.random.nextDouble() * Math.PI * 2.0;
        double heightOffset = Math.max(-0.45, Math.min(0.85, offset.y));
        double speed = 0.145 + world.random.nextDouble() * 0.055;
        return new OrbitingProjectile(angle, orbitRadius, heightOffset, speed, world.random.nextDouble() * Math.PI * 2.0);
    }

    private static void fireOneProjectile(ServerWorld world, ActiveSun sun) {
        LivingEntity owner = getOwner(world, sun);
        LivingEntity target = findTarget(world, sun, owner);
        if (target == null) {
            return;
        }

        ProjectileEntity projectile = null;
        Iterator<Map.Entry<UUID, OrbitingProjectile>> iterator = sun.orbiters.entrySet().iterator();
        while (iterator.hasNext()) {
            UUID projectileId = iterator.next().getKey();
            Entity entity = world.getEntity(projectileId);
            if (!(entity instanceof ProjectileEntity candidate) || !candidate.isAlive() || candidate.isRemoved()) {
                iterator.remove();
                continue;
            }
            projectile = candidate;
            iterator.remove();
            break;
        }
        if (projectile == null) {
            return;
        }

        Vec3d targetPos = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.58, 0.0);
        Vec3d direction = targetPos.subtract(projectile.getPos()).normalize();
        projectile.removeScoreboardTag(ORBITING_PROJECTILE_TAG);
        projectile.setNoGravity(false);
        projectile.setVelocity(direction.x, direction.y, direction.z, 2.25F, 0.08F);
        projectile.velocityModified = true;
        sun.recentlyFired.put(projectile.getUuid(), world.getTime() + 30L);

        sun.expiresAt = Math.min(sun.maxExpiresAt, sun.expiresAt + Math.max(0, durationBonusPerShotTicks()));
        grantCelestialSwiftness(world, sun);
        world.playSound(null, sun.center.x, sun.center.y, sun.center.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.48F, 1.62F);
        world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.78F, 1.92F);
        world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.32F, 1.44F);
        world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.22F, 1.95F);
        world.spawnParticles(ParticleTypes.END_ROD, projectile.getX(), projectile.getY(), projectile.getZ(), 6, 0.10, 0.10, 0.10, 0.035);
    }

    private static LivingEntity findTarget(ServerWorld world, ActiveSun sun, LivingEntity owner) {
        double targetRadius = sun.radius + 8.0;
        Box box = Box.of(sun.center, targetRadius * 2.0, targetRadius * 1.5, targetRadius * 2.0);
        LivingEntity best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, box, candidate ->
                CombatTargeting.isOffensiveTargetCandidate(candidate, owner)
                        && candidate.squaredDistanceTo(sun.center) <= targetRadius * targetRadius)) {
            if (!hasLineOfSight(world, sun.center, candidate)) {
                continue;
            }
            double distanceSq = candidate.squaredDistanceTo(sun.center);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean hasLineOfSight(ServerWorld world, Vec3d from, LivingEntity target) {
        Vec3d to = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.55, 0.0);
        HitResult result = world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, target));
        return result.getType() == HitResult.Type.MISS || result.getPos().squaredDistanceTo(to) < 0.35;
    }

    private static void grantCelestialSwiftness(ServerWorld world, ActiveSun sun) {
        LivingEntity owner = getOwner(world, sun);
        if (owner == null || !owner.isAlive()) {
            return;
        }
        int stacks = CelestialSwiftnessTracker.getStacks(owner.getUuid(), world.getTime()) + 1;
        stacks = Math.min(Math.max(1, maxSwiftnessStacks()), stacks);
        int duration = Math.max(20, swiftnessDurationTicks());
        CelestialSwiftnessTracker.set(owner.getUuid(), stacks, world.getTime() + duration);
        if (owner instanceof ServerPlayerEntity player) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            CelestialSwiftnessPayload.encode(new CelestialSwiftnessPayload(player.getUuid(), stacks, duration), buf);
            NetworkManager.sendToPlayer(player, CelestialSwiftnessPayload.CHANNEL_ID, buf);
            player.sendMessage(Text.translatable("message.simplybows.cosmic.celestial_swiftness", stacks), true);
        }
    }

    private static LivingEntity getOwner(ServerWorld world, ActiveSun sun) {
        if (sun.ownerId == null) {
            return null;
        }
        Entity owner = world.getEntity(sun.ownerId);
        if (owner instanceof LivingEntity living) {
            return living;
        }
        return world.getServer().getPlayerManager().getPlayer(sun.ownerId);
    }

    private static void spawnAmbientParticles(ServerWorld world, ActiveSun sun) {
        world.spawnParticles(ParticleTypes.FLAME, sun.center.x, sun.center.y, sun.center.z, 5, 0.42, 0.28, 0.42, 0.015);
        world.spawnParticles(ParticleTypes.END_ROD, sun.center.x, sun.center.y, sun.center.z, 3, 0.28, 0.20, 0.28, 0.01);
    }

    private static void playOrbitCaptureSound(ServerWorld world, ActiveSun sun, ProjectileEntity projectile) {
        if (sun.playedOrbitCaptureSound) {
            return;
        }
        sun.playedOrbitCaptureSound = true;
        world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.28F, 1.72F);
        world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 0.16F, 1.28F);
    }

    private static boolean isSunReady(ServerWorld world, UUID ownerId) {
        if (world == null || ownerId == null) {
            return false;
        }
        long now = CooldownStorage.currentTick(world);
        Long cooldownEnd = getCooldowns(world).get(ownerId);
        return cooldownEnd == null || cooldownEnd <= now;
    }

    private static void startSunCooldown(ServerWorld world, UUID ownerId, int ticks) {
        if (ticks <= 0) {
            return;
        }
        getCooldowns(world).put(ownerId, CooldownStorage.currentTick(world) + ticks);
    }

    private static void finishSunCooldown(ServerWorld world, ActiveSun sun, long now) {
        if (sun.ownerId == null) {
            return;
        }
        int ticks = Math.max(0, cooldownTicks());
        if (ticks <= 0) {
            return;
        }
        long readyAt = now + ticks;
        if (getCooldowns(world).merge(sun.ownerId, readyAt, Math::max) != readyAt) {
            return;
        }
        MinecraftServer server = world.getServer();
        ServerPlayerEntity player = server == null ? null : server.getPlayerManager().getPlayer(sun.ownerId);
        if (player != null) {
            SimplyBowItem.simplybows$sendCooldownPacket(player, "cosmic", System.currentTimeMillis() + ticks * 50L, ticks);
        }
    }

    private static Map<UUID, Long> getCooldowns(ServerWorld world) {
        return CooldownStorage.forWorld(SUN_COOLDOWNS_BY_SERVER, world);
    }

    private static class ActiveSun {
        private final ServerWorld world;
        private final UUID visualId;
        private final UUID ownerId;
        private final Vec3d center;
        private final double radius;
        private final long spawnTick;
        private final long maxExpiresAt;
        private long expiresAt;
        private long nextFireAt;
        private boolean playedOrbitCaptureSound;
        private final Map<UUID, OrbitingProjectile> orbiters = new HashMap<>();
        private final Map<UUID, Long> recentlyFired = new HashMap<>();

        private ActiveSun(ServerWorld world, UUID visualId, UUID ownerId, Vec3d center, double radius, long spawnTick, long expiresAt, long maxExpiresAt, long nextFireAt) {
            this.world = world;
            this.visualId = visualId;
            this.ownerId = ownerId;
            this.center = center;
            this.radius = radius;
            this.spawnTick = spawnTick;
            this.expiresAt = expiresAt;
            this.maxExpiresAt = maxExpiresAt;
            this.nextFireAt = nextFireAt;
        }
    }

    private static class OrbitingProjectile {
        private double angle;
        private final double radius;
        private final double heightOffset;
        private final double speed;
        private final double verticalPhase;
        private int age;

        private OrbitingProjectile(double angle, double radius, double heightOffset, double speed, double verticalPhase) {
            this.angle = angle;
            this.radius = radius;
            this.heightOffset = heightOffset;
            this.speed = speed;
            this.verticalPhase = verticalPhase;
        }
    }
}
