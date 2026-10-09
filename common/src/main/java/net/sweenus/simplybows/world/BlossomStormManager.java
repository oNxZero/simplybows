package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.util.BowAbilityBalance;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.util.GraceProjectile;
import net.sweenus.simplybows.world.RuneUseCooldown;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BlossomStormManager {

    private static final int JUMP_INTERVAL_TICKS = 25;
    private static final int GRACE_STRENGTH_AMPLIFIER = 1; // Strength II
    private static final int GRACE_FIELD_BASE_TICKS = 60; // 3s puddle
    private static final int GRACE_BUFF_BASE_TICKS = 100; // 5s Strength
    private static final int GRACE_FIELD_PER_STRING_TICKS = 10;
    private static final int GRACE_BUFF_PER_FRAME_TICKS = 20;
    private static final double GRACE_BASE_RADIUS = 3.0;
    private static final double GRACE_RADIUS_PER_STRING = 0.45;
    private static final double BOUNTY_TRIGGER_BASE_KNOCKUP = 0.18;
    private static final double BOUNTY_TRIGGER_KNOCKUP_PER_FRAME = 0.04;
    private static final int PAIN_VISUAL_RING_MIN_POINTS = 24;
    private static final int PAIN_VISUAL_RING_MAX_POINTS = 72;
    private static final int VORTEX_POINTS = 4;
    private static final int STORM_FADE_TICKS = 12;

    private static int stormDurationTicks() { return SimplyBowsConfig.INSTANCE.petalwind.stormDurationTicks.get(); }
    private static int stormDurationBonusPerString() { return SimplyBowsConfig.INSTANCE.petalwind.stormDurationBonusPerString.get(); }
    private static int damageIntervalTicks() { return SimplyBowsConfig.INSTANCE.petalwind.damageIntervalTicks.get(); }
    private static float stormDamage() { return SimplyBowsConfig.INSTANCE.petalwind.stormDamage.get(); }
    private static double jumpRange() { return SimplyBowsConfig.INSTANCE.petalwind.jumpRange.get(); }
    private static double graceAuraDamageRadius() { return SimplyBowsConfig.INSTANCE.petalwind.graceAuraDamageRadius.get(); }
    private static double graceAuraRadiusPerString() { return SimplyBowsConfig.INSTANCE.petalwind.graceAuraRadiusPerString.get(); }
    private static int graceBuffDurationTicks() { return SimplyBowsConfig.INSTANCE.petalwind.graceBuffDuration.get(); }
    private static int bountyMaxStorms() {
        // Hard-cap at 3 even if someone edits config past the intended max.
        return Math.min(3, Math.max(1, SimplyBowsConfig.INSTANCE.petalwind.bountyMaxStorms.get()));
    }
    private static double bountyTriggerBaseRadius() { return SimplyBowsConfig.INSTANCE.petalwind.bountyTriggerBaseRadius.get(); }
    private static double bountyTriggerRadiusPerString() { return SimplyBowsConfig.INSTANCE.petalwind.bountyTriggerRadiusPerString.get(); }
    private static double painAreaDamageRadius() { return SimplyBowsConfig.INSTANCE.petalwind.painAreaRadius.get(); }
    private static double painAreaRadiusPerString() { return SimplyBowsConfig.INSTANCE.petalwind.painAreaRadiusPerString.get(); }
    private static final Map<ServerWorld, List<ActiveStorm>> ACTIVE_STORMS = new HashMap<>();

    private BlossomStormManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveStorm> storms = ACTIVE_STORMS.get(world);
        return storms != null && !storms.isEmpty();
    }

    public static void createStorm(ServerWorld world, Vec3d startPos, LivingEntity directTarget, Entity owner) {
        createStorm(world, startPos, directTarget, owner, BowUpgradeData.none());
    }

    public static void createStorm(ServerWorld world, Vec3d startPos, LivingEntity directTarget, Entity owner, BowUpgradeData upgrades) {
        List<ActiveStorm> existing = ACTIVE_STORMS.computeIfAbsent(world, w -> new ArrayList<>());
        UUID ownerId = owner != null ? owner.getUuid() : null;
        StormTuning tuning = buildTuning(upgrades);
        if (tuning.bountyTrapMode() && ownerId != null && !RuneUseCooldown.isReady(world, ownerId, "blossom-bounty")) {
            return;
        }
        if (tuning.painAreaMode() && ownerId != null && !RuneUseCooldown.isReady(world, ownerId, "blossom-pain")) {
            return;
        }
        if (tuning.graceSupportMode() && ownerId != null && !RuneUseCooldown.isReady(world, ownerId, "blossom-grace")) {
            return;
        }
        boolean baseStorm = !tuning.bountyTrapMode() && !tuning.painAreaMode() && !tuning.graceSupportMode();
        if (baseStorm && ownerId != null && !RuneUseCooldown.isReady(world, ownerId, "blossom-base")) {
            return;
        }
        if (ownerId != null && !tuning.bountyTrapMode()) {
            for (ActiveStorm storm : existing) {
                if (ownerId.equals(storm.ownerId)) {
                    return;
                }
            }
        }

        long now = world.getTime();
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;

        if (tuning.bountyTrapMode()) {
            // Up to 3 petal storms locked onto separate targets (no free hopping).
            while (ownerId != null && countOwnerBountyStorms(existing, ownerId) > 0) {
                int oldestIndex = findOldestOwnerBountyStormIndex(existing, ownerId);
                if (oldestIndex < 0) {
                    break;
                }
                existing.remove(oldestIndex);
            }
            List<LivingEntity> targets = findBountyInitialTargets(world, startPos, directTarget, ownerLiving, tuning.maxActiveBountyTraps());
            if (targets.isEmpty()) {
                // No hostiles nearby — one hunter storm at impact, same as base bow.
                existing.add(new ActiveStorm(
                        now + tuning.durationTicks(),
                        now + damageIntervalTicks(),
                        now + JUMP_INTERVAL_TICKS,
                        now,
                        startPos,
                        null,
                        ownerId,
                        tuning
                ));
            } else {
                for (LivingEntity target : targets) {
                    Vec3d center = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
                    existing.add(new ActiveStorm(
                            now + tuning.durationTicks(),
                            now + damageIntervalTicks(),
                            now + JUMP_INTERVAL_TICKS,
                            now,
                            center,
                            target.getUuid(),
                            ownerId,
                            tuning
                    ));
                }
            }
            RuneUseCooldown.startForEffect(world, ownerId, "blossom-bounty", "blossom", tuning.durationTicks());
            world.playSound(null, startPos.x, startPos.y, startPos.z, SoundEvents.BLOCK_CHERRY_LEAVES_PLACE, SoundCategory.PLAYERS, 0.8F, 0.95F + world.random.nextFloat() * 0.2F);
            return;
        }

        UUID initialTargetId = null;
        Vec3d initialCenter = startPos;
        if (tuning.graceSupportMode()) {
            // Fixed ground puddle — under struck feet, or at impact. Never follows.
            double groundY = directTarget != null ? directTarget.getY() : startPos.y;
            double x = directTarget != null ? directTarget.getX() : startPos.x;
            double z = directTarget != null ? directTarget.getZ() : startPos.z;
            initialCenter = new Vec3d(x, groundY, z);
            initialTargetId = null;
        } else if (directTarget != null && directTarget.isAlive() && ownerLiving != null && CombatTargeting.checkFriendlyFire(directTarget, ownerLiving)) {
            initialTargetId = directTarget.getUuid();
        }

        ActiveStorm storm = new ActiveStorm(
                now + tuning.durationTicks(),
                now + damageIntervalTicks(),
                now + JUMP_INTERVAL_TICKS,
                now,
                initialCenter,
                initialTargetId,
                ownerId,
                tuning
        );
        existing.add(storm);
        if (tuning.painAreaMode()) {
            RuneUseCooldown.startForEffect(world, ownerId, "blossom-pain", "blossom", tuning.durationTicks());
        } else if (tuning.graceSupportMode()) {
            RuneUseCooldown.startForEffect(world, ownerId, "blossom-grace", "blossom", tuning.durationTicks());
        } else {
            RuneUseCooldown.startForEffect(world, ownerId, "blossom-base", "blossom", tuning.durationTicks());
        }
        world.playSound(null, startPos.x, startPos.y, startPos.z, SoundEvents.BLOCK_CHERRY_LEAVES_PLACE, SoundCategory.PLAYERS, 0.8F, 0.95F + world.random.nextFloat() * 0.2F);
    }

    public static void tick(ServerWorld world) {
        List<ActiveStorm> storms = ACTIVE_STORMS.get(world);
        if (storms == null || storms.isEmpty()) {
            return;
        }

        long now = world.getTime();
        storms.removeIf(storm -> tickStorm(world, storm, now));
        if (storms.isEmpty()) {
            ACTIVE_STORMS.remove(world);
        }
    }

    private static boolean tickStorm(ServerWorld world, ActiveStorm storm, long now) {
        if (storm.fadeStartTick > 0L) {
            long age = now - storm.fadeStartTick;
            if (age >= STORM_FADE_TICKS) {
                return true;
            }
            spawnVortexParticles(world, storm, now, 1.0F - (float) age / (float) STORM_FADE_TICKS);
            return false;
        }
        if (now >= storm.expiryTick) {
            storm.fadeStartTick = now;
            spawnVortexParticles(world, storm, now, 1.0F);
            return false;
        }

        LivingEntity currentTarget = storm.currentTargetId == null ? null : getLivingEntity(world, storm.currentTargetId);
        // Pain / Grace stay planted; base + Bounty leaping storms follow their target.
        if (!storm.tuning.painAreaMode() && !storm.tuning.graceSupportMode()
                && currentTarget != null && currentTarget.isAlive()) {
            storm.center = currentTarget.getPos().add(0.0, currentTarget.getHeight() * 0.5, 0.0);
        }

        spawnVortexParticles(world, storm, now, 1.0F);

        if (now >= storm.nextDamageTick) {
            LivingEntity owner = getLivingEntityNullable(world, storm.ownerId);
            if (storm.tuning.painAreaMode()) {
                applyPainAreaDamage(world, storm, owner);
            } else if (storm.tuning.graceSupportMode()) {
                applyGraceSupportPulse(world, storm, owner);
            } else if (currentTarget != null && currentTarget.isAlive()) {
                CombatTargeting.applyAbilityDamage(world, owner, currentTarget, storm.tuning.damage(), true, false, storm.tuning.damageBonusScale(), false);
            }
            int interval = storm.tuning.painAreaMode()
                    ? Math.max(8, damageIntervalTicks() - 2)
                    : damageIntervalTicks();
            storm.nextDamageTick = now + interval;
        }

        if (!storm.tuning.painAreaMode() && !storm.tuning.graceSupportMode()
                && (currentTarget == null || !currentTarget.isAlive() || now >= storm.nextJumpTick)) {
            // Bounty: lock to the initial 3 targets — only hop when that target dies.
            boolean bountyLocked = storm.tuning.bountyTrapMode();
            boolean needsRetarget = currentTarget == null || !currentTarget.isAlive();
            if (!bountyLocked || needsRetarget) {
                LivingEntity next = findNextTarget(world, storm, bountyLocked ? null : currentTarget);
                if (next != null && (currentTarget == null || !next.getUuid().equals(currentTarget.getUuid()))) {
                    Vec3d from = storm.center;
                    Vec3d to = next.getPos().add(0.0, next.getHeight() * 0.5, 0.0);
                    spawnJumpTrail(world, from, to);
                    world.playSound(null, to.x, to.y, to.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.7F, 1.05F + world.random.nextFloat() * 0.2F);
                    storm.currentTargetId = next.getUuid();
                    storm.center = to;
                }
            }
            storm.nextJumpTick = now + JUMP_INTERVAL_TICKS;
        }

        return false;
    }

    private static void applyPainAreaDamage(ServerWorld world, ActiveStorm storm, LivingEntity owner) {
        double radius = storm.tuning.painAreaRadius();
        Box hitBox = Box.of(storm.center, radius * 2.0, 5.0, radius * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                hitBox,
                CombatTargeting::isOffensiveTargetCandidate
        )) {
            if (storm.ownerId != null && candidate.getUuid().equals(storm.ownerId)) {
                continue;
            }
            if (candidate.squaredDistanceTo(storm.center) > radius * radius) {
                continue;
            }
            if (owner != null && !CombatTargeting.checkFriendlyFire(candidate, owner)) {
                continue;
            }
            // Same DPS as the leaping storm, but AOE.
            CombatTargeting.applyAbilityDamage(world, owner, candidate, storm.tuning.damage(), true, false, storm.tuning.damageBonusScale(), false);
        }
    }

    private static void applyGraceSupportPulse(ServerWorld world, ActiveStorm storm, LivingEntity owner) {
        double radius = storm.tuning.graceAuraRadius();
        int buffTicks = storm.tuning.graceBuffTicks();
        Box hitBox = Box.of(storm.center, radius * 2.0, 3.5, radius * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                hitBox,
                entity -> entity.isAlive() && GraceProjectile.isSupportTarget(entity)
        )) {
            if (candidate.squaredDistanceTo(storm.center) > radius * radius) {
                continue;
            }
            candidate.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.STRENGTH, buffTicks, GRACE_STRENGTH_AMPLIFIER),
                    owner
            );
        }
    }

    private static LivingEntity findNextTarget(ServerWorld world, ActiveStorm storm, LivingEntity currentTarget) {
        if (storm.tuning.graceSupportMode()) {
            return findNextGraceTarget(world, storm, currentTarget);
        }

        Box search = Box.of(storm.center, jumpRange() * 2.0, 5.0, jumpRange() * 2.0);
        List<LivingEntity> candidates = world.getEntitiesByClass(
                LivingEntity.class,
                search,
                CombatTargeting::isOffensiveTargetCandidate
        );
        LivingEntity owner = getLivingEntityNullable(world, storm.ownerId);
        Set<UUID> claimedBySiblings = storm.tuning.bountyTrapMode()
                ? claimedBountyTargets(world, storm)
                : Set.of();

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            if (storm.ownerId != null && candidate.getUuid().equals(storm.ownerId)) {
                continue;
            }
            if (currentTarget != null && candidate.getUuid().equals(currentTarget.getUuid())) {
                continue;
            }
            if (claimedBySiblings.contains(candidate.getUuid())) {
                continue;
            }
            if (owner != null && !CombatTargeting.checkFriendlyFire(candidate, owner)) {
                continue;
            }
            double dist = candidate.squaredDistanceTo(storm.center);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    private static List<LivingEntity> findBountyInitialTargets(
            ServerWorld world, Vec3d impact, LivingEntity directTarget, LivingEntity owner, int maxTargets) {
        List<LivingEntity> chosen = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        if (directTarget != null && directTarget.isAlive()
                && CombatTargeting.isOffensiveTargetCandidate(directTarget, owner)
                && (owner == null || CombatTargeting.checkFriendlyFire(directTarget, owner))) {
            chosen.add(directTarget);
            seen.add(directTarget.getUuid());
        }
        Box search = Box.of(impact, jumpRange() * 2.0, 6.0, jumpRange() * 2.0);
        List<LivingEntity> nearby = new ArrayList<>(world.getEntitiesByClass(
                LivingEntity.class,
                search,
                entity -> CombatTargeting.isOffensiveTargetCandidate(entity, owner)
                        && (owner == null || CombatTargeting.checkFriendlyFire(entity, owner))
                        && !seen.contains(entity.getUuid())
        ));
        nearby.sort((a, b) -> Double.compare(a.squaredDistanceTo(impact), b.squaredDistanceTo(impact)));
        for (LivingEntity candidate : nearby) {
            if (chosen.size() >= maxTargets) {
                break;
            }
            chosen.add(candidate);
            seen.add(candidate.getUuid());
        }
        return chosen;
    }

    private static Set<UUID> claimedBountyTargets(ServerWorld world, ActiveStorm self) {
        Set<UUID> claimed = new HashSet<>();
        List<ActiveStorm> storms = ACTIVE_STORMS.get(world);
        if (storms == null || self.ownerId == null) {
            return claimed;
        }
        for (ActiveStorm other : storms) {
            if (other == self || !self.ownerId.equals(other.ownerId) || !other.tuning.bountyTrapMode()) {
                continue;
            }
            if (other.currentTargetId != null) {
                claimed.add(other.currentTargetId);
            }
        }
        return claimed;
    }

    private static LivingEntity findNextGraceTarget(ServerWorld world, ActiveStorm storm, LivingEntity currentTarget) {
        LivingEntity owner = getLivingEntityNullable(world, storm.ownerId);
        if (owner == null || !owner.isAlive()) {
            return null;
        }

        Box search = Box.of(storm.center, jumpRange() * 2.0, 5.0, jumpRange() * 2.0);
        List<LivingEntity> candidates = world.getEntitiesByClass(
                LivingEntity.class,
                search,
                entity -> entity.isAlive() && CombatTargeting.isFriendlyTo(entity, owner)
        );
        candidates.add(owner);

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            if (currentTarget != null && candidate.getUuid().equals(currentTarget.getUuid())) {
                continue;
            }
            if (candidate.squaredDistanceTo(storm.center) > jumpRange() * jumpRange()) {
                continue;
            }
            double dist = candidate.squaredDistanceTo(storm.center);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    private static LivingEntity resolveGraceInitialAnchor(ServerWorld world, Vec3d impact, LivingEntity owner, LivingEntity directTarget) {
        if (directTarget != null && directTarget.isAlive() && (owner == null || CombatTargeting.isFriendlyTo(directTarget, owner))) {
            return directTarget;
        }
        if (world == null || impact == null || owner == null) {
            return owner;
        }
        Box search = Box.of(impact, 8.0, 5.0, 8.0);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, search, entity ->
                entity.isAlive() && CombatTargeting.isFriendlyTo(entity, owner))) {
            double dist = candidate.squaredDistanceTo(impact);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        if (owner.isAlive()) {
            double ownerDist = owner.squaredDistanceTo(impact);
            if (best == null || ownerDist < bestDist) {
                return owner;
            }
        }
        return best;
    }

    private static void spawnVortexParticles(ServerWorld world, ActiveStorm storm, long now, float presence) {
        presence = Math.max(0.0F, Math.min(1.0F, presence));
        if (presence <= 0.02F) {
            return;
        }
        Vec3d center = storm.center;
        if (storm.tuning.graceSupportMode()) {
            double radius = Math.max(0.8, storm.tuning.graceAuraRadius()) * presence;
            spawnFilledPuddleParticles(world, center, radius, now, true);
            return;
        }
        double time = now * 0.25;
        for (int i = 0; i < VORTEX_POINTS; i++) {
            double angle = time + (Math.PI * 2.0 / VORTEX_POINTS) * i;
            double radius = (0.85 + 0.2 * Math.sin(time + i)) * presence;
            if (storm.tuning.painAreaMode()) {
                radius *= 1.45;
            }
            double y = center.y - 0.3 + ((i % 4) * 0.22);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.CHERRY_LEAVES, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
            if (i % 4 == 0) {
                world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y + 0.08, z, 1, 0.01, 0.01, 0.01, 0.0);
            }
        }

        if (!storm.tuning.painAreaMode()) {
            return;
        }

        double ringRadius = storm.tuning.painAreaRadius() * presence;
        int ringPoints = Math.max(PAIN_VISUAL_RING_MIN_POINTS, Math.min(PAIN_VISUAL_RING_MAX_POINTS, (int) Math.round(ringRadius * 10.0)));
        double spin = now * 0.06;
        int step = presence < 0.45F ? 4 : 2;
        int offset = (int) (now & 1L);
        for (int i = offset; i < ringPoints; i += step) {
            double angle = spin + (Math.PI * 2.0 / ringPoints) * i;
            double wobble = 0.12 * Math.sin((now * 0.12) + i * 0.7);
            double radius = ringRadius + wobble * presence;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y - 0.05 + 0.35 * Math.sin((now * 0.08) + i * 0.45);
            world.spawnParticles(ParticleTypes.CHERRY_LEAVES, x, y, z, 1, 0.015, 0.02, 0.015, 0.0);
            if ((i & 3) == 0) {
                world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y + 0.12, z, 1, 0.01, 0.01, 0.01, 0.0);
            }
            if ((i & 7) == 0) {
                world.spawnParticles(ParticleTypes.ENCHANT, x, y + 0.18, z, 1, 0.0, 0.02, 0.0, 0.0);
            }
        }
    }

    private static void spawnFilledPuddleParticles(ServerWorld world, Vec3d center, double radius, long now, boolean grace) {
        // Outer ring
        int ringPoints = Math.max(16, (int) Math.round(radius * 10.0));
        double spin = now * 0.04;
        for (int i = 0; i < ringPoints; i++) {
            double angle = spin + (Math.PI * 2.0 / ringPoints) * i;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.CHERRY_LEAVES, x, center.y + 0.1, z, 1, 0.02, 0.01, 0.02, 0.0);
        }
        // Filled disk — denser toward the center so it reads as a puddle, not a hoop.
        int fillCount = Math.max(10, (int) Math.round(radius * radius * 3.5));
        for (int i = 0; i < fillCount; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2.0;
            double dist = radius * Math.sqrt(world.random.nextDouble());
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, center.y + 0.06, z, 1, 0.04, 0.01, 0.04, 0.0);
            if ((i & 3) == 0) {
                world.spawnParticles(ParticleTypes.CHERRY_LEAVES, x, center.y + 0.08, z, 1, 0.03, 0.01, 0.03, 0.0);
            }
            if (grace && (i & 7) == 0) {
                world.spawnParticles(ParticleTypes.ENCHANT, x, center.y + 0.15, z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
        if (now % 4L == 0L) {
            world.spawnParticles(ParticleTypes.FALLING_SPORE_BLOSSOM, center.x, center.y + 0.2, center.z,
                    4, radius * 0.45, 0.05, radius * 0.45, 0.0);
        }
    }

    private static void spawnJumpTrail(ServerWorld world, Vec3d from, Vec3d to) {
        Vec3d delta = to.subtract(from);
        int steps = Math.max(8, (int) (delta.length() * 6.0));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3d point = from.add(delta.multiply(t));
            world.spawnParticles(ParticleTypes.CHERRY_LEAVES, point.x, point.y, point.z, 1, 0.01, 0.01, 0.01, 0.0);
            if (i % 3 == 0) {
                world.spawnParticles(ParticleTypes.ENCHANT, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static LivingEntity getLivingEntity(ServerWorld world, UUID id) {
        Entity entity = world.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static LivingEntity getLivingEntityNullable(ServerWorld world, UUID id) {
        if (id == null) {
            return null;
        }
        return getLivingEntity(world, id);
    }

    private static int countOwnerBountyStorms(List<ActiveStorm> storms, UUID ownerId) {
        int count = 0;
        for (ActiveStorm storm : storms) {
            if (ownerId.equals(storm.ownerId) && storm.tuning.bountyTrapMode()) {
                count++;
            }
        }
        return count;
    }

    private static int findOldestOwnerBountyStormIndex(List<ActiveStorm> storms, UUID ownerId) {
        int oldestIndex = -1;
        long oldestSpawn = Long.MAX_VALUE;
        for (int i = 0; i < storms.size(); i++) {
            ActiveStorm storm = storms.get(i);
            if (!ownerId.equals(storm.ownerId) || !storm.tuning.bountyTrapMode()) {
                continue;
            }
            if (storm.spawnTick < oldestSpawn) {
                oldestSpawn = storm.spawnTick;
                oldestIndex = i;
            }
        }
        return oldestIndex;
    }

    private static StormTuning buildTuning(BowUpgradeData upgrades) {
        RuneEtching rune = upgrades.runeEtching();
        float damage = (float) (stormDamage() * upgrades.damageMultiplier());
        boolean painAreaMode = rune == RuneEtching.PAIN;
        boolean graceSupportMode = rune == RuneEtching.GRACE;
        boolean bountyTrapMode = rune == RuneEtching.BOUNTY;
        damage *= BowAbilityBalance.petalDamageScale(rune, upgrades.frameLevel());
        float damageBonusScale = BowAbilityBalance.petalBonusScale(rune, upgrades.frameLevel());
        double painAreaRadius = Math.min(4.5, Math.max(3.0, painAreaDamageRadius() * 0.55))
                + upgrades.stringLevel() * Math.min(0.45, painAreaRadiusPerString());
        double graceAuraRadius = GRACE_BASE_RADIUS + upgrades.stringLevel() * GRACE_RADIUS_PER_STRING
                + (graceAuraDamageRadius() > 0 ? Math.min(1.5, graceAuraRadiusPerString() * upgrades.stringLevel() * 0.25) : 0.0);
        double bountyTriggerRadius = Math.max(1.4, bountyTriggerBaseRadius() + upgrades.stringLevel() * bountyTriggerRadiusPerString());
        int maxActiveBountyTraps = bountyMaxStorms();
        double bountyTriggerKnockup = 0.0;
        int durationTicks;
        int graceBuffTicks = GRACE_BUFF_BASE_TICKS + upgrades.frameLevel() * GRACE_BUFF_PER_FRAME_TICKS;
        if (graceSupportMode) {
            durationTicks = GRACE_FIELD_BASE_TICKS + upgrades.stringLevel() * GRACE_FIELD_PER_STRING_TICKS;
        } else if (painAreaMode) {
            durationTicks = stormDurationTicks() + upgrades.stringLevel() * stormDurationBonusPerString() + 20;
        } else {
            durationTicks = stormDurationTicks() + upgrades.stringLevel() * stormDurationBonusPerString();
        }
        return new StormTuning(damage, damageBonusScale, painAreaMode, graceSupportMode, bountyTrapMode, painAreaRadius, graceAuraRadius,
                bountyTriggerRadius, maxActiveBountyTraps, bountyTriggerKnockup, durationTicks, graceBuffTicks);
    }

    private static final class ActiveStorm {
        private final long expiryTick;
        private long nextDamageTick;
        private long nextJumpTick;
        private final long spawnTick;
        private Vec3d center;
        private UUID currentTargetId;
        private final UUID ownerId;
        private final StormTuning tuning;
        private final Set<UUID> triggeredBountyVictims = new HashSet<>();
        private long fadeStartTick;

        private ActiveStorm(long expiryTick, long nextDamageTick, long nextJumpTick, long spawnTick, Vec3d center, UUID currentTargetId, UUID ownerId, StormTuning tuning) {
            this.expiryTick = expiryTick;
            this.nextDamageTick = nextDamageTick;
            this.nextJumpTick = nextJumpTick;
            this.spawnTick = spawnTick;
            this.center = center;
            this.currentTargetId = currentTargetId;
            this.ownerId = ownerId;
            this.tuning = tuning;
        }
    }

    private record StormTuning(
            float damage,
            float damageBonusScale,
            boolean painAreaMode,
            boolean graceSupportMode,
            boolean bountyTrapMode,
            double painAreaRadius,
            double graceAuraRadius,
            double bountyTriggerRadius,
            int maxActiveBountyTraps,
            double bountyTriggerKnockup,
            int durationTicks,
            int graceBuffTicks
    ) {
    }
}
