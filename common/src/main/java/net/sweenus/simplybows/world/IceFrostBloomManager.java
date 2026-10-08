package net.sweenus.simplybows.world;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.util.GraceProjectile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Winterfang Pain frost zone, Bounty frost pulses, and Grace support sanctuary.
 */
public final class IceFrostBloomManager {

    private static final int PAIN_ZONE_DURATION_TICKS = 80; // 4s control zone
    private static final int PAIN_PULSE_GAP_TICKS = 10;
    private static final int PAIN_SLOWNESS_TICKS = 40;
    private static final int BOUNTY_PULSE_GAP_TICKS = 20; // 1 pulse per second
    private static final int BOUNTY_SLOWNESS_TICKS = 40;
    private static final int GRACE_ZONE_DURATION_TICKS = 140;
    private static final int GRACE_BUFF_TICKS = 100;
    private static final int BLOOM_CAST_LOCK_TICKS = 12;

    public static int graceZoneDurationTicks() {
        return GRACE_ZONE_DURATION_TICKS;
    }
    private static final Map<ServerWorld, List<ActiveBloom>> ACTIVE = new HashMap<>();
    private static final Map<ServerWorld, List<GraceZone>> GRACE_ZONES = new HashMap<>();
    private static final Map<ServerWorld, List<PainZone>> PAIN_ZONES = new HashMap<>();
    private static final Map<UUID, Long> RECENT_BLOOM_CAST = new HashMap<>();

    private IceFrostBloomManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveBloom> blooms = ACTIVE.get(world);
        List<GraceZone> zones = GRACE_ZONES.get(world);
        List<PainZone> pain = PAIN_ZONES.get(world);
        return (blooms != null && !blooms.isEmpty())
                || (zones != null && !zones.isEmpty())
                || (pain != null && !pain.isEmpty());
    }

    /** Lasting frost zone: Slowness II + light chip damage. Useful control, not a one-tick puff. */
    public static void spawnPainBloom(ServerWorld world, Vec3d center, LivingEntity owner, float baseDamage, int stringLevel) {
        if (world == null || center == null || !claimBloomCast(world, owner)) {
            return;
        }
        double radius = SimplyBowsConfig.INSTANCE.winterfang.painFrostRadius.get()
                + Math.max(0, stringLevel) * SimplyBowsConfig.INSTANCE.winterfang.painFrostRadiusPerString.get();
        float damage = (float) (baseDamage * Math.max(0.12, SimplyBowsConfig.INSTANCE.winterfang.painFrostDamageMultiplier.get()));
        long now = world.getTime();
        PAIN_ZONES.computeIfAbsent(world, w -> new ArrayList<>()).add(new PainZone(
                center,
                owner != null ? owner.getUuid() : null,
                radius,
                damage,
                now + PAIN_ZONE_DURATION_TICKS,
                now
        ));
        applyPainPulse(world, center, owner, radius, damage);
        spawnFrostFloor(world, center, radius, 1.0F);
        spawnFrostRing(world, center, radius, 1.0F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_POWDER_SNOW_PLACE, SoundCategory.PLAYERS, 0.95F, 0.7F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.55F, 1.1F);
    }

    /** String = radius. Frame = pulse count. One pulse per second with clear frost animation. */
    public static void spawnBountyBloom(ServerWorld world, Vec3d center, LivingEntity owner, float baseDamage, int stringLevel, int frameLevel) {
        if (world == null || center == null || !claimBloomCast(world, owner)) {
            return;
        }
        double radius = SimplyBowsConfig.INSTANCE.winterfang.bountyFrostRadius.get()
                + Math.max(0, stringLevel) * SimplyBowsConfig.INSTANCE.winterfang.bountyFrostRadiusPerString.get();
        float damage = (float) (baseDamage * SimplyBowsConfig.INSTANCE.winterfang.bountyFrostDamageMultiplier.get());
        int pulses = Math.max(2, SimplyBowsConfig.INSTANCE.winterfang.bountyFrostPulseCount.get() + Math.max(0, frameLevel));
        long now = world.getTime();
        pulseDamage(world, center, owner, radius, damage, BOUNTY_SLOWNESS_TICKS, 0);
        spawnBountyPulseVisual(world, center, radius, 1.0F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_POWDER_SNOW_BREAK, SoundCategory.PLAYERS, 0.9F, 0.8F);
        if (pulses > 1) {
            ACTIVE.computeIfAbsent(world, w -> new ArrayList<>()).add(new ActiveBloom(
                    center, owner != null ? owner.getUuid() : null, radius, damage, pulses - 1, now + BOUNTY_PULSE_GAP_TICKS
            ));
        }
    }

    private static boolean claimBloomCast(ServerWorld world, LivingEntity owner) {
        if (owner == null) {
            return true;
        }
        Long last = RECENT_BLOOM_CAST.get(owner.getUuid());
        long now = world.getTime();
        if (last != null && now - last < BLOOM_CAST_LOCK_TICKS) {
            return false;
        }
        RECENT_BLOOM_CAST.put(owner.getUuid(), now);
        return true;
    }

    /** Support zone: allies get Resistance + Speed + Regen, hostiles get Slowness. */
    public static void spawnGraceSanctuary(ServerWorld world, Vec3d center, LivingEntity owner, int stringLevel) {
        if (world == null || center == null || owner == null) {
            return;
        }
        double radius = 3.25 + Math.max(0, stringLevel) * 0.35;
        long now = world.getTime();
        GRACE_ZONES.computeIfAbsent(world, w -> new ArrayList<>()).add(new GraceZone(
                center, owner.getUuid(), radius, now + GRACE_ZONE_DURATION_TICKS, now
        ));
        applyGracePulse(world, center, owner, radius);
        spawnFrostFloor(world, center, radius, 1.0F);
        spawnFrostRing(world, center, radius, 1.0F);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.7F, 1.4F);
    }

    public static void tick(ServerWorld world) {
        tickPainZones(world);
        tickBlooms(world);
        tickGraceZones(world);
    }

    private static void tickPainZones(ServerWorld world) {
        List<PainZone> zones = PAIN_ZONES.get(world);
        if (zones == null || zones.isEmpty()) {
            return;
        }
        Iterator<PainZone> it = zones.iterator();
        while (it.hasNext()) {
            PainZone zone = it.next();
            if (world.getTime() >= zone.expiryTick) {
                it.remove();
                continue;
            }
            spawnFrostFloor(world, zone.center, zone.radius, 0.55F);
            spawnAmbient(world, zone.center, zone.radius);
            if (world.getTime() >= zone.nextPulseTick) {
                LivingEntity owner = zone.ownerId == null ? null : getLiving(world, zone.ownerId);
                applyPainPulse(world, zone.center, owner, zone.radius, zone.damage);
                spawnFrostRing(world, zone.center, zone.radius, 0.7F);
                zone.nextPulseTick = world.getTime() + PAIN_PULSE_GAP_TICKS;
            }
        }
        if (zones.isEmpty()) {
            PAIN_ZONES.remove(world);
        }
    }

    private static void tickBlooms(ServerWorld world) {
        List<ActiveBloom> blooms = ACTIVE.get(world);
        if (blooms == null || blooms.isEmpty()) {
            return;
        }
        Iterator<ActiveBloom> it = blooms.iterator();
        while (it.hasNext()) {
            ActiveBloom bloom = it.next();
            if (world.getTime() < bloom.nextPulseTick) {
                spawnAmbient(world, bloom.center, bloom.radius);
                spawnFrostFloor(world, bloom.center, bloom.radius, 0.35F);
                continue;
            }
            LivingEntity owner = bloom.ownerId == null ? null : getLiving(world, bloom.ownerId);
            pulseDamage(world, bloom.center, owner, bloom.radius, bloom.damage, BOUNTY_SLOWNESS_TICKS, 0);
            spawnBountyPulseVisual(world, bloom.center, bloom.radius, 0.85F);
            world.playSound(null, bloom.center.x, bloom.center.y, bloom.center.z,
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.35F, 1.4F);
            bloom.pulsesLeft--;
            if (bloom.pulsesLeft <= 0) {
                it.remove();
            } else {
                bloom.nextPulseTick = world.getTime() + BOUNTY_PULSE_GAP_TICKS;
            }
        }
        if (blooms.isEmpty()) {
            ACTIVE.remove(world);
        }
    }

    private static void tickGraceZones(ServerWorld world) {
        List<GraceZone> zones = GRACE_ZONES.get(world);
        if (zones == null || zones.isEmpty()) {
            return;
        }
        Iterator<GraceZone> it = zones.iterator();
        while (it.hasNext()) {
            GraceZone zone = it.next();
            if (world.getTime() >= zone.expiryTick) {
                it.remove();
                continue;
            }
            // Dense floor every tick so the sanctuary reads clearly.
            spawnFrostFloor(world, zone.center, zone.radius, 0.85F);
            spawnAmbient(world, zone.center, zone.radius);
            if (world.getTime() % 8L == 0L) {
                spawnFrostRing(world, zone.center, zone.radius, 0.75F);
            }
            if (world.getTime() >= zone.nextPulseTick) {
                LivingEntity owner = getLiving(world, zone.ownerId);
                if (owner != null && owner.isAlive()) {
                    applyGracePulse(world, zone.center, owner, zone.radius);
                }
                zone.nextPulseTick = world.getTime() + 20L;
            }
        }
        if (zones.isEmpty()) {
            GRACE_ZONES.remove(world);
        }
    }

    private static void applyPainPulse(ServerWorld world, Vec3d center, LivingEntity owner, double radius, float damage) {
        Box box = Box.of(center, radius * 2.0, 3.5, radius * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, box, CombatTargeting::isOffensiveTargetCandidate)) {
            if (owner != null && !CombatTargeting.checkFriendlyFire(candidate, owner)) {
                continue;
            }
            if (candidate.squaredDistanceTo(center) > radius * radius) {
                continue;
            }
            CombatTargeting.applyDamage(world, owner, candidate, damage, true, false);
            candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, PAIN_SLOWNESS_TICKS, 1), owner);
            candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, PAIN_SLOWNESS_TICKS, 0), owner);
        }
    }

    private static void pulseDamage(ServerWorld world, Vec3d center, LivingEntity owner, double radius, float damage, int slowTicks, int slowAmp) {
        Box box = Box.of(center, radius * 2.0, 3.5, radius * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, box, CombatTargeting::isOffensiveTargetCandidate)) {
            if (owner != null && !CombatTargeting.checkFriendlyFire(candidate, owner)) {
                continue;
            }
            if (candidate.squaredDistanceTo(center) > radius * radius) {
                continue;
            }
            CombatTargeting.applyDamage(world, owner, candidate, damage, true, false);
            candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowTicks, slowAmp), owner);
        }
    }

    private static void applyGracePulse(ServerWorld world, Vec3d center, LivingEntity owner, double radius) {
        Box box = Box.of(center, radius * 2.0, 3.5, radius * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (candidate.squaredDistanceTo(center) > radius * radius) {
                continue;
            }
            if (GraceProjectile.isSupportTarget(candidate)) {
                candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, GRACE_BUFF_TICKS, 0), owner);
                candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, GRACE_BUFF_TICKS, 0), owner);
                candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, GRACE_BUFF_TICKS, 0), owner);
            } else if (CombatTargeting.isOffensiveTargetCandidate(candidate)
                    && CombatTargeting.checkFriendlyFire(candidate, owner)) {
                candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0), owner);
            }
        }
    }

    private static void spawnFrostFloor(ServerWorld world, Vec3d center, double radius, float presence) {
        presence = Math.max(0.15F, Math.min(1.0F, presence));
        int count = Math.max(8, Math.round(18 * presence));
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.PACKED_ICE.getDefaultState()),
                center.x, center.y + 0.05, center.z, count, radius * 0.55, 0.02, radius * 0.55, 0.0);
        world.spawnParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.12, center.z,
                Math.max(6, Math.round(14 * presence)), radius * 0.5, 0.08, radius * 0.5, 0.0);
        world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, center.x, center.y + 0.04, center.z,
                Math.max(4, Math.round(10 * presence)), radius * 0.45, 0.02, radius * 0.45, 0.0);
        world.spawnParticles(ParticleTypes.WHITE_ASH, center.x, center.y + 0.18, center.z,
                Math.max(4, Math.round(8 * presence)), radius * 0.4, 0.1, radius * 0.4, 0.0);
    }

    private static void spawnBountyPulseVisual(ServerWorld world, Vec3d center, double radius, float presence) {
        spawnFrostFloor(world, center, radius, presence);
        spawnFrostRing(world, center, radius, presence);
        // Rising frost columns so each 1s pulse reads clearly.
        int pillars = Math.max(8, (int) Math.round(radius * 4.0));
        for (int i = 0; i < pillars; i++) {
            double angle = (Math.PI * 2.0 / pillars) * i + world.getTime() * 0.05;
            double dist = radius * (0.35 + (i % 3) * 0.2);
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            world.spawnParticles(ParticleTypes.SNOWFLAKE, x, center.y + 0.1, z, 4, 0.05, 0.45, 0.05, 0.0);
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.ICE.getDefaultState()),
                    x, center.y + 0.05, z, 2, 0.04, 0.25, 0.04, 0.0);
        }
    }

    private static void spawnFrostRing(ServerWorld world, Vec3d center, double radius, float presence) {
        int points = Math.max(16, (int) Math.round(radius * 10.0));
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 / points) * i;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.SNOWFLAKE, x, center.y + 0.15, z, 1, 0.02, 0.05, 0.02, 0.0);
            if ((i & 1) == 0) {
                world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, x, center.y + 0.05, z, 1, 0.02, 0.02, 0.02, 0.0);
            }
            if ((i % 3) == 0) {
                world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.ICE.getDefaultState()),
                        x, center.y + 0.08, z, 1, 0.02, 0.04, 0.02, 0.0);
            }
        }
        world.spawnParticles(ParticleTypes.WHITE_ASH, center.x, center.y + 0.2, center.z,
                Math.max(6, Math.round(10 * presence)), radius * 0.35, 0.12, radius * 0.35, 0.01);
    }

    private static void spawnAmbient(ServerWorld world, Vec3d center, double radius) {
        world.spawnParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 0.2, center.z,
                6, radius * 0.45, 0.15, radius * 0.45, 0.0);
    }

    private static LivingEntity getLiving(ServerWorld world, UUID id) {
        return world.getEntity(id) instanceof LivingEntity living ? living : null;
    }

    private static final class ActiveBloom {
        private final Vec3d center;
        private final UUID ownerId;
        private final double radius;
        private final float damage;
        private int pulsesLeft;
        private long nextPulseTick;

        private ActiveBloom(Vec3d center, UUID ownerId, double radius, float damage, int pulses, long nextPulseTick) {
            this.center = center;
            this.ownerId = ownerId;
            this.radius = radius;
            this.damage = damage;
            this.pulsesLeft = pulses;
            this.nextPulseTick = nextPulseTick;
        }
    }

    private static final class GraceZone {
        private final Vec3d center;
        private final UUID ownerId;
        private final double radius;
        private final long expiryTick;
        private long nextPulseTick;

        private GraceZone(Vec3d center, UUID ownerId, double radius, long expiryTick, long nextPulseTick) {
            this.center = center;
            this.ownerId = ownerId;
            this.radius = radius;
            this.expiryTick = expiryTick;
            this.nextPulseTick = nextPulseTick;
        }
    }

    private static final class PainZone {
        private final Vec3d center;
        private final UUID ownerId;
        private final double radius;
        private final float damage;
        private final long expiryTick;
        private long nextPulseTick;

        private PainZone(Vec3d center, UUID ownerId, double radius, float damage, long expiryTick, long nextPulseTick) {
            this.center = center;
            this.ownerId = ownerId;
            this.radius = radius;
            this.damage = damage;
            this.expiryTick = expiryTick;
            this.nextPulseTick = nextPulseTick;
        }
    }
}
