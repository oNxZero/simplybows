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
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.util.NetworkCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BlossomStormManager {

    private static final int JUMP_INTERVAL_TICKS = 25;
    private static final int GRACE_SWIFTNESS_AMPLIFIER = 0;
    private static final int GRACE_HASTE_AMPLIFIER = 0;
    private static final double BOUNTY_TRIGGER_BASE_KNOCKUP = 0.22;
    private static final double BOUNTY_TRIGGER_KNOCKUP_PER_FRAME = 0.06;
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
    private static int bountyBaseMaxTraps() { return SimplyBowsConfig.INSTANCE.petalwind.bountyBaseMaxTraps.get(); }
    private static int bountyMaxTrapsPerString() { return SimplyBowsConfig.INSTANCE.petalwind.bountyMaxTrapsPerString.get(); }
    private static float bountyTriggerDamageMultiplier() { return SimplyBowsConfig.INSTANCE.petalwind.bountyTriggerDamageMultiplier.get(); }
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
        if (ownerId != null && !tuning.bountyTrapMode()) {
            for (ActiveStorm storm : existing) {
                if (ownerId.equals(storm.ownerId)) {
                    return;
                }
            }
        } else if (ownerId != null) {
            int maxTraps = tuning.maxActiveBountyTraps();
            while (countOwnerBountyStorms(existing, ownerId) >= maxTraps) {
                int oldestIndex = findOldestOwnerBountyStormIndex(existing, ownerId);
                if (oldestIndex < 0) {
                    break;
                }
                existing.remove(oldestIndex);
            }
        }

        long now = world.getTime();
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
        UUID initialTargetId = null;
        Vec3d initialCenter = startPos;
        if (tuning.bountyTrapMode()) {
            initialTargetId = null;
        } else if (tuning.graceSupportMode()) {
            LivingEntity anchor = resolveGraceInitialAnchor(ownerLiving, directTarget);
            if (anchor != null) {
                initialTargetId = anchor.getUuid();
                initialCenter = anchor.getPos().add(0.0, anchor.getHeight() * 0.5, 0.0);
            }
        } else if (directTarget != null && ownerLiving != null && CombatTargeting.checkFriendlyFire(directTarget, ownerLiving)) {
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
        if (owner instanceof ServerPlayerEntity player) {
            int durationTicks = Math.max(20, tuning.durationTicks());
            SimplyBowItem.simplybows$sendCooldownPacket(player, "blossom",
                    System.currentTimeMillis() + (long) durationTicks * 50L, durationTicks);
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
        if (currentTarget != null && currentTarget.isAlive()) {
            storm.center = currentTarget.getPos().add(0.0, currentTarget.getHeight() * 0.5, 0.0);
        }

        if (storm.tuning.bountyTrapMode()) {
            boolean triggered = processBountyTrapTriggers(world, storm, getLivingEntityNullable(world, storm.ownerId));
            if (triggered) {
                return false;
            }
        }

        spawnVortexParticles(world, storm, now, 1.0F);

        if (!storm.tuning.bountyTrapMode() && now >= storm.nextDamageTick) {
            LivingEntity owner = getLivingEntityNullable(world, storm.ownerId);
            if (storm.tuning.painAreaMode()) {
                applyPainAreaDamage(world, storm, owner);
            } else if (storm.tuning.graceSupportMode()) {
                applyGraceSupportPulse(world, storm, owner, currentTarget);
            } else if (currentTarget != null && currentTarget.isAlive()) {
                CombatTargeting.applyDamage(world, owner, currentTarget, storm.tuning.damage(), true, false);
            }
            storm.nextDamageTick = now + damageIntervalTicks();
        }

        if (!storm.tuning.painAreaMode() && !storm.tuning.bountyTrapMode() && now >= storm.nextJumpTick) {
            LivingEntity next = findNextTarget(world, storm, currentTarget);
            if (next != null && (currentTarget == null || !next.getUuid().equals(currentTarget.getUuid()))) {
                Vec3d from = storm.center;
                Vec3d to = next.getPos().add(0.0, next.getHeight() * 0.5, 0.0);
                spawnJumpTrail(world, from, to);
                world.playSound(null, to.x, to.y, to.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.7F, 1.05F + world.random.nextFloat() * 0.2F);
                storm.currentTargetId = next.getUuid();
                storm.center = to;
            }
            storm.nextJumpTick = now + JUMP_INTERVAL_TICKS;
        }

        return false;
    }

    private static boolean processBountyTrapTriggers(ServerWorld world, ActiveStorm storm, LivingEntity owner) {
        double radius = storm.tuning.bountyTriggerRadius();
        float damage = storm.tuning.damage() * bountyTriggerDamageMultiplier();
        Box triggerBox = Box.of(storm.center, radius * 2.0, 3.5, radius * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                triggerBox,
                CombatTargeting::isOffensiveTargetCandidate
        )) {
            if (candidate.squaredDistanceTo(storm.center) > radius * radius) {
                continue;
            }
            if (!storm.triggeredBountyVictims.add(candidate.getUuid())) {
                continue;
            }
            if (owner != null && !CombatTargeting.checkFriendlyFire(candidate, owner)) {
                continue;
            }

            CombatTargeting.applyDamage(world, owner, candidate, damage, true, false);
            applyBountyTriggerKnockup(candidate, storm.tuning.bountyTriggerKnockup());
            spawnBountyTriggerEffects(world, candidate.getPos().add(0.0, candidate.getHeight() * 0.5, 0.0), radius);
            storm.fadeStartTick = world.getTime();
            return true;
        }
        return false;
    }

    private static void applyBountyTriggerKnockup(LivingEntity target, double knockup) {
        if (target == null || knockup <= 0.0) {
            return;
        }
        target.addVelocity(0.0, knockup, 0.0);
        target.setOnGround(false);
        target.velocityDirty = true;
        if (target instanceof ServerPlayerEntity player) {
            NetworkCompat.sendVelocityUpdate(player);
        }
    }

    private static void spawnBountyTriggerEffects(ServerWorld world, Vec3d center, double radius) {
        world.spawnParticles(ParticleTypes.CHERRY_LEAVES, center.x, center.y, center.z, 22, radius * 0.35, 0.22, radius * 0.35, 0.01);
        world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, center.x, center.y + 0.1, center.z, 16, radius * 0.28, 0.18, radius * 0.28, 0.0);
        world.spawnParticles(ParticleTypes.CRIT, center.x, center.y + 0.12, center.z, 10, radius * 0.2, 0.15, radius * 0.2, 0.02);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.8F, 0.9F + world.random.nextFloat() * 0.15F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_CHERRY_LEAVES_BREAK, SoundCategory.PLAYERS, 0.7F, 0.95F + world.random.nextFloat() * 0.2F);
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
            CombatTargeting.applyDamage(world, owner, candidate, storm.tuning.damage(), true, false);
        }
    }

    private static void applyGraceSupportPulse(ServerWorld world, ActiveStorm storm, LivingEntity owner, LivingEntity currentTarget) {
        LivingEntity anchor = currentTarget;
        if (anchor == null || !anchor.isAlive()) {
            anchor = getLivingEntityNullable(world, storm.ownerId);
        }
        if (anchor == null || !anchor.isAlive()) {
            return;
        }

        Vec3d anchorCenter = anchor.getPos().add(0.0, anchor.getHeight() * 0.5, 0.0);
        storm.center = anchorCenter;
        anchor.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, graceBuffDurationTicks(), GRACE_SWIFTNESS_AMPLIFIER), owner);
        anchor.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, graceBuffDurationTicks(), GRACE_HASTE_AMPLIFIER), owner);

        double radius = storm.tuning.graceAuraRadius();
        Box hitBox = Box.of(anchorCenter, radius * 2.0, 5.0, radius * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                hitBox,
                CombatTargeting::isOffensiveTargetCandidate
        )) {
            if (candidate.squaredDistanceTo(anchorCenter) > radius * radius) {
                continue;
            }
            if (owner != null && !CombatTargeting.checkFriendlyFire(candidate, owner)) {
                continue;
            }
            CombatTargeting.applyDamage(world, owner, candidate, storm.tuning.damage(), true, false);
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

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            if (storm.ownerId != null && candidate.getUuid().equals(storm.ownerId)) {
                continue;
            }
            if (currentTarget != null && candidate.getUuid().equals(currentTarget.getUuid())) {
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

    private static LivingEntity resolveGraceInitialAnchor(LivingEntity owner, LivingEntity directTarget) {
        if (owner == null || !owner.isAlive()) {
            return null;
        }
        if (directTarget != null && directTarget.isAlive() && CombatTargeting.isFriendlyTo(directTarget, owner)) {
            return directTarget;
        }
        return owner;
    }

    private static void spawnVortexParticles(ServerWorld world, ActiveStorm storm, long now, float presence) {
        presence = Math.max(0.0F, Math.min(1.0F, presence));
        if (presence <= 0.02F) {
            return;
        }
        Vec3d center = storm.center;
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
        double painAreaRadius = (painAreaDamageRadius() * upgrades.sizeMultiplier()) + upgrades.stringLevel() * painAreaRadiusPerString();
        double graceAuraRadius = (graceAuraDamageRadius() * upgrades.sizeMultiplier()) + upgrades.stringLevel() * graceAuraRadiusPerString();
        double bountyTriggerRadius = (bountyTriggerBaseRadius() * upgrades.sizeMultiplier()) + upgrades.stringLevel() * bountyTriggerRadiusPerString();
        int maxActiveBountyTraps = bountyBaseMaxTraps() + upgrades.stringLevel() * bountyMaxTrapsPerString();
        double bountyTriggerKnockup = BOUNTY_TRIGGER_BASE_KNOCKUP + upgrades.frameLevel() * BOUNTY_TRIGGER_KNOCKUP_PER_FRAME;
        int durationTicks = stormDurationTicks() + upgrades.stringLevel() * stormDurationBonusPerString();
        return new StormTuning(damage, painAreaMode, graceSupportMode, bountyTrapMode, painAreaRadius, graceAuraRadius, bountyTriggerRadius, maxActiveBountyTraps, bountyTriggerKnockup, durationTicks);
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
            boolean painAreaMode,
            boolean graceSupportMode,
            boolean bountyTrapMode,
            double painAreaRadius,
            double graceAuraRadius,
            double bountyTriggerRadius,
            int maxActiveBountyTraps,
            double bountyTriggerKnockup,
            int durationTicks
    ) {
    }
}
