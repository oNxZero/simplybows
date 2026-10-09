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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pain impact splashes and a harmless Grace sanctuary. */
public final class IceFrostBloomManager {
    private static final Map<ServerWorld, List<GraceZone>> GRACE_ZONES = new java.util.WeakHashMap<>();
    private static final Map<ServerWorld, Map<UUID, SplashClaims>> PAIN_CLAIMS = new java.util.WeakHashMap<>();
    public static int graceZoneDurationTicks() { return 140; }
    private IceFrostBloomManager() {}
    public static boolean hasActive(ServerWorld world) { return GRACE_ZONES.containsKey(world) || PAIN_CLAIMS.containsKey(world); }

    public static void spawnPainImpact(ServerWorld world, LivingEntity victim, LivingEntity owner, float baseDamage, UUID volleyId) {
        if (owner == null || !CombatTargeting.checkFriendlyFire(victim, owner)) return;
        IceFrostSlowManager.apply(world, owner, victim);
        Vec3d center = victim.getPos();
        double radius = Math.max(0.5, Math.min(3.0, SimplyBowsConfig.INSTANCE.winterfang.painFrostRadius.get()));
        float damage = (float) (baseDamage * SimplyBowsConfig.INSTANCE.winterfang.painFrostDamageMultiplier.get());
        UUID cast = volleyId != null ? volleyId : UUID.randomUUID();
        SplashClaims claims = PAIN_CLAIMS.computeIfAbsent(world, w -> new HashMap<>())
                .computeIfAbsent(cast, id -> new SplashClaims(world.getTime() + 600, new java.util.HashSet<>()));
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, victim.getBoundingBox().expand(radius),
                target -> target != victim && target.isAlive() && (CombatTargeting.isOffensiveTargetCandidate(target)
                        || target instanceof net.minecraft.entity.mob.Monster))) {
            if (target.squaredDistanceTo(center) > radius * radius || !CombatTargeting.checkFriendlyFire(target, owner)
                    || !claims.targets.add(target.getUuid())) continue;
            CombatTargeting.applyDamage(world, owner, target, damage, true, false);
            IceFrostSlowManager.apply(world, owner, target);
        }
        spawnFrostRing(world, center, radius, 1);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_POWDER_SNOW_BREAK, SoundCategory.PLAYERS, 0.7F, 1.2F);
    }

    public static void spawnGraceSanctuary(ServerWorld world, Vec3d center, LivingEntity owner, int stringLevel, int frameLevel) {
        if (owner == null) return;
        double radius = 3.25 + Math.max(0, stringLevel) * 0.35;
        GRACE_ZONES.computeIfAbsent(world, w -> new ArrayList<>()).add(new GraceZone(center, owner.getUuid(), radius,
                world.getTime() + graceZoneDurationTicks(), 100 + Math.max(0, frameLevel) * 20));
        applyGracePulse(world, center, owner, radius, 100 + Math.max(0, frameLevel) * 20);
        spawnFrostRing(world, center, radius, 1);
    }

    public static void tick(ServerWorld world) {
        Map<UUID, SplashClaims> claims = PAIN_CLAIMS.get(world);
        if (claims != null) {
            claims.values().removeIf(c -> c.expires <= world.getTime());
            if (claims.isEmpty()) PAIN_CLAIMS.remove(world);
        }
        List<GraceZone> zones = GRACE_ZONES.get(world);
        if (zones == null) return;
        zones.removeIf(zone -> {
            if (zone.expires <= world.getTime()) return true;
            if (world.getTime() % 4 == 0) spawnFrostFloor(world, zone.center, zone.radius, 0.65F);
            if (world.getTime() % 20 == 0 && world.getEntity(zone.owner) instanceof LivingEntity owner)
                applyGracePulse(world, zone.center, owner, zone.radius, zone.buffTicks);
            return false;
        });
        if (zones.isEmpty()) GRACE_ZONES.remove(world);
    }

    private static void applyGracePulse(ServerWorld world, Vec3d center, LivingEntity owner, double radius, int buffTicks) {
        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, Box.of(center, radius * 2, 3.5, radius * 2),
                LivingEntity::isAlive)) {
            if (target.squaredDistanceTo(center) > radius * radius || !GraceProjectile.isSupportTarget(target)) continue;
            if (target != owner && target instanceof net.minecraft.entity.player.PlayerEntity
                    && !CombatTargeting.isFriendlyTo(target, owner)) continue;
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, buffTicks, 0), owner);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, buffTicks, 0), owner);
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, buffTicks, 0), owner);
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

    private record GraceZone(Vec3d center, UUID owner, double radius, long expires, int buffTicks) {}
    private record SplashClaims(long expires, java.util.Set<UUID> targets) {}
}
