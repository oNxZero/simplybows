package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.BeeGraceVisualEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.util.GraceProjectile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Grace: bees sting allies for Resistance, then hop to the next ally in range.
 * String = bee count. Frame = stings/hops per bee.
 */
public final class BeeGraceShieldManager {

    public static final String GRACE_VISUAL_TAG = "simplybows_bee_grace_visual";
    private static final double HOP_SEARCH_RADIUS = 10.0;
    private static final double STING_REACH = 1.15;
    private static final double FLY_SPEED = 0.55;
    private static final int RESISTANCE_TICKS = 60; // 3s
    private static final int RESISTANCE_AMPLIFIER = 1; // Res II
    private static final int BEE_LIFETIME_TICKS = 200;

    private static final Map<ServerWorld, List<ActiveGraceBee>> ACTIVE_BEES = new HashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> GRACE_COOLDOWNS_BY_SERVER = CooldownStorage.newServerScopedStore();

    private BeeGraceShieldManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveGraceBee> bees = ACTIVE_BEES.get(world);
        return (bees != null && !bees.isEmpty()) || (world.getTime() % 20L == 0L);
    }

    public static void tryApplyFromImpact(ServerWorld world, Vec3d impactPos, LivingEntity owner, BowUpgradeData upgrades, LivingEntity struck) {
        if (world == null || impactPos == null || owner == null || !isGraceReady(world, owner.getUuid())) {
            return;
        }

        int beeCount = Math.max(1, 1 + upgrades.stringLevel());
        int hopsPerBee = Math.max(1, 1 + upgrades.frameLevel());
        List<LivingEntity> allies = findAlliesNear(world, impactPos, HOP_SEARCH_RADIUS + 2.0);
        if (struck != null && struck.isAlive() && GraceProjectile.isSupportTarget(struck) && !allies.contains(struck)) {
            allies.addFirst(struck);
        }
        if (allies.isEmpty()) {
            return;
        }

        // Prefer struck ally as first sting target for bee 0.
        if (struck != null && GraceProjectile.isSupportTarget(struck)) {
            allies.remove(struck);
            allies.addFirst(struck);
        }

        long now = world.getTime();
        List<ActiveGraceBee> bees = ACTIVE_BEES.computeIfAbsent(world, w -> new ArrayList<>());
        int spawned = 0;
        for (int i = 0; i < beeCount; i++) {
            LivingEntity startTarget = allies.get(i % allies.size());
            Vec3d start = impactPos.add(
                    (world.random.nextDouble() - 0.5) * 0.6,
                    0.4 + world.random.nextDouble() * 0.3,
                    (world.random.nextDouble() - 0.5) * 0.6
            );
            BeeGraceVisualEntity visual = new BeeGraceVisualEntity(world, start.x, start.y, start.z);
            visual.addCommandTag(GRACE_VISUAL_TAG);
            if (!world.spawnEntity(visual)) {
                continue;
            }
            Set<UUID> stung = new HashSet<>();
            bees.add(new ActiveGraceBee(
                    visual.getUuid(),
                    startTarget.getUuid(),
                    hopsPerBee,
                    stung,
                    now + BEE_LIFETIME_TICKS
            ));
            spawned++;
        }

        if (spawned <= 0) {
            return;
        }

        int duration = SimplyBowsConfig.INSTANCE.buzzkill.graceBaseDuration.get()
                + Math.max(0, upgrades.stringLevel()) * SimplyBowsConfig.INSTANCE.buzzkill.graceStringDurationBonus.get();
        startGraceCooldown(world, owner, Math.max(40, duration));
        world.playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.ENTITY_BEE_POLLINATE, SoundCategory.PLAYERS, 0.55F, 1.25F);
        world.spawnParticles(ParticleTypes.WAX_ON, impactPos.x, impactPos.y + 0.3, impactPos.z, 10, 0.25, 0.2, 0.25, 0.01);
    }

    /** Legacy no-op — Grace no longer absorbs hits; bees apply Resistance by stinging. */
    public static boolean consumeShield(ServerWorld world, LivingEntity target) {
        return false;
    }

    public static void tick(ServerWorld world) {
        List<ActiveGraceBee> bees = ACTIVE_BEES.get(world);
        if (bees == null || bees.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanVisuals(world);
            }
            return;
        }

        Iterator<ActiveGraceBee> it = bees.iterator();
        while (it.hasNext()) {
            ActiveGraceBee bee = it.next();
            Entity visualEntity = world.getEntity(bee.visualId);
            if (!(visualEntity instanceof BeeGraceVisualEntity visual) || world.getTime() > bee.expiryTick || bee.hopsLeft <= 0) {
                if (visualEntity != null) {
                    BowEffectSounds.end(world,visualEntity.getPos(),BowEffectSounds.Theme.SUPPORT_BEE);
                    visualEntity.discard();
                }
                it.remove();
                continue;
            }

            LivingEntity target = bee.targetId != null && world.getEntity(bee.targetId) instanceof LivingEntity living && living.isAlive()
                    ? living : null;
            if (target != null && !isStingable(target, bee.stung)) {
                target = null;
                bee.targetId = null;
            }
            if (target == null) {
                LivingEntity next = findNextAlly(world, visual.getPos(), bee.stung);
                if (next == null) {
                    wanderGraceBee(world, visual, bee);
                    continue;
                }
                bee.targetId = next.getUuid();
                target = next;
            }

            Vec3d aim = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.55, 0.0);
            Vec3d delta = aim.subtract(visual.getPos());
            double distSq = delta.lengthSquared();
            if (distSq <= STING_REACH * STING_REACH) {
                sting(world, visual, target, bee);
                if (bee.hopsLeft <= 0) {
                    visual.discard();
                    it.remove();
                    continue;
                }
                LivingEntity next = findNextAlly(world, target.getPos(), bee.stung);
                if (next == null) {
                    bee.targetId = null;
                    wanderGraceBee(world, visual, bee);
                    continue;
                }
                bee.targetId = next.getUuid();
                continue;
            }

            Vec3d step = delta.normalize().multiply(FLY_SPEED);
            visual.setPos(visual.getX() + step.x, visual.getY() + step.y, visual.getZ() + step.z);
            faceVelocity(visual, step);
            visual.setHeightScale(1.0F);
        }

        if (bees.isEmpty()) {
            ACTIVE_BEES.remove(world);
        }
    }

    private static void sting(ServerWorld world, BeeGraceVisualEntity visual, LivingEntity target, ActiveGraceBee bee) {
        bee.stung.add(target.getUuid());
        bee.hopsLeft--;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, RESISTANCE_TICKS, RESISTANCE_AMPLIFIER), null);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_BEE_STING, SoundCategory.PLAYERS, 0.7F, 1.25F);
        world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_BEE_POLLINATE, SoundCategory.PLAYERS, 0.45F, 1.35F);
        world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.55), target.getZ(), 8, 0.15, 0.2, 0.15, 0.02);
        world.spawnParticles(ParticleTypes.WAX_ON, target.getX(), target.getBodyY(0.55), target.getZ(), 10, 0.2, 0.2, 0.2, 0.01);
        Vec3d bounce = visual.getPos().subtract(target.getPos()).normalize().multiply(0.65);
        if (bounce.lengthSquared() < 1.0E-4) {
            bounce = new Vec3d(0.4, 0.2, 0.0);
        }
        visual.setPos(target.getX() + bounce.x, target.getBodyY(0.7) + 0.2, target.getZ() + bounce.z);
    }

    private static void wanderGraceBee(ServerWorld world, BeeGraceVisualEntity visual, ActiveGraceBee bee) {
        if (bee.wanderDir == null || world.getTime() % 12L == 0L) {
            double ang = world.random.nextDouble() * Math.PI * 2.0;
            bee.wanderDir = new Vec3d(Math.cos(ang), (world.random.nextDouble() - 0.5) * 0.25, Math.sin(ang)).normalize();
        }
        Vec3d step = bee.wanderDir.multiply(FLY_SPEED * 0.75);
        visual.setPos(visual.getX() + step.x, visual.getY() + step.y, visual.getZ() + step.z);
        faceVelocity(visual, step);
        visual.setHeightScale(1.0F);
        // Keep scanning while idle-swarming.
        LivingEntity next = findNextAlly(world, visual.getPos(), bee.stung);
        if (next != null) {
            bee.targetId = next.getUuid();
        }
    }

    private static void faceVelocity(BeeGraceVisualEntity visual, Vec3d step) {
        if (step.horizontalLengthSquared() > 1.0E-6) {
            visual.setYaw((float) (Math.atan2(-step.x, step.z) * (180.0 / Math.PI)));
        }
        visual.setPitch((float) MathHelper.clamp(-Math.atan2(step.y, step.horizontalLength()) * (180.0 / Math.PI), -40.0, 40.0));
    }

    private static boolean isStingable(LivingEntity ally, Set<UUID> stung) {
        if (stung.contains(ally.getUuid())) {
            return false;
        }
        // Don't re-sting someone who already has Resistance.
        return !ally.hasStatusEffect(StatusEffects.RESISTANCE);
    }

    private static List<LivingEntity> findAlliesNear(ServerWorld world, Vec3d pos, double radius) {
        Box box = Box.of(pos, radius * 2.0, radius * 2.0, radius * 2.0);
        List<LivingEntity> allies = world.getEntitiesByClass(LivingEntity.class, box,
                e -> e.isAlive() && GraceProjectile.isSupportTarget(e) && e.squaredDistanceTo(pos) <= radius * radius);
        allies.sort((a, b) -> Double.compare(a.squaredDistanceTo(pos), b.squaredDistanceTo(pos)));
        return allies;
    }

    private static LivingEntity findNextAlly(ServerWorld world, Vec3d from, Set<UUID> stung) {
        List<LivingEntity> allies = findAlliesNear(world, from, HOP_SEARCH_RADIUS);
        for (LivingEntity ally : allies) {
            if (isStingable(ally, stung)) {
                return ally;
            }
        }
        return null;
    }

    private static boolean isGraceReady(ServerWorld world, UUID ownerId) {
        if (!RuneUseCooldown.isPlayerReady(world, ownerId)) {
            return false;
        }
        long now = CooldownStorage.currentTick(world);
        Long cooldownEnd = CooldownStorage.forWorld(GRACE_COOLDOWNS_BY_SERVER, world).get(ownerId);
        return cooldownEnd == null || cooldownEnd <= now;
    }

    private static void startGraceCooldown(ServerWorld world, LivingEntity owner, int durationTicks) {
        int cooldownTicks = RuneUseCooldown.fromEffectDuration(durationTicks);
        CooldownStorage.forWorld(GRACE_COOLDOWNS_BY_SERVER, world)
                .put(owner.getUuid(), CooldownStorage.currentTick(world) + cooldownTicks);
        RuneUseCooldown.start(world, owner.getUuid(), "bee-grace", "bee", cooldownTicks);
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof BeeGraceVisualEntity && entity.getCommandTags().contains(GRACE_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static final class ActiveGraceBee {
        private final UUID visualId;
        private UUID targetId;
        private int hopsLeft;
        private final Set<UUID> stung;
        private final long expiryTick;
        private Vec3d wanderDir;

        private ActiveGraceBee(UUID visualId, UUID targetId, int hopsLeft, Set<UUID> stung, long expiryTick) {
            this.visualId = visualId;
            this.targetId = targetId;
            this.hopsLeft = hopsLeft;
            this.stung = stung;
            this.expiryTick = expiryTick;
        }
    }
}
