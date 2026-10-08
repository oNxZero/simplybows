package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.BeeArrowEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BeeChaosHoneyStormManager {

    private static final Map<ServerWorld, List<ActiveHoneyStorm>> ACTIVE_STORMS = new HashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> STORM_COOLDOWNS_BY_SERVER = CooldownStorage.newServerScopedStore();
    private static final double STORM_HEIGHT_OFFSET = 6.0;
    private static final long TARGET_HIT_COOLDOWN_TICKS = 10L;
    private static final int HONEY_FADE_TICKS = 12;

    private BeeChaosHoneyStormManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveHoneyStorm> storms = ACTIVE_STORMS.get(world);
        return (storms != null && !storms.isEmpty()) || !getCooldowns(world).isEmpty();
    }

    public static boolean isStormReady(ServerWorld world, UUID ownerId) {
        if (world == null || ownerId == null) {
            return false;
        }
        if (!RuneUseCooldown.isPlayerReady(world, ownerId)) {
            return false;
        }
        long now = CooldownStorage.currentTick(world);
        Long cooldownEnd = getCooldowns(world).get(ownerId);
        return cooldownEnd == null || cooldownEnd <= now;
    }

    public static void spawnAtImpact(ServerWorld world, Vec3d center, UUID ownerId, int stringLevel, int frameLevel) {
        if (world == null || center == null) {
            return;
        }

        Vec3d cloudCenter = center.add(0.0, STORM_HEIGHT_OFFSET, 0.0);

        List<ActiveHoneyStorm> storms = ACTIVE_STORMS.computeIfAbsent(world, ignored -> new ArrayList<>());
        if (ownerId != null) {
            storms.removeIf(storm -> ownerId.equals(storm.ownerId));
        }

        // Hard-cap near 13s so String/Frame cannot stretch the storm into a half-minute zone.
        int durationTicks = Math.max(20, Math.min(280,
                SimplyBowsConfig.INSTANCE.buzzkill.chaosBaseDurationTicks.get()
                        + Math.max(0, stringLevel) * Math.min(10, SimplyBowsConfig.INSTANCE.buzzkill.chaosDurationPerStringTicks.get())));
        double radius = Math.max(1.5, Math.min(7.0,
                SimplyBowsConfig.INSTANCE.buzzkill.chaosBaseRadius.get()
                        + Math.max(0, stringLevel) * Math.min(0.35, SimplyBowsConfig.INSTANCE.buzzkill.chaosRadiusPerString.get())));
        int diveInterval = Math.max(
                SimplyBowsConfig.INSTANCE.buzzkill.chaosMinDiveIntervalTicks.get(),
                SimplyBowsConfig.INSTANCE.buzzkill.chaosBaseDiveIntervalTicks.get()
                        - Math.max(0, frameLevel) * SimplyBowsConfig.INSTANCE.buzzkill.chaosDiveIntervalReductionPerFrameTicks.get()
        );

        long now = world.getTime();
        ActiveHoneyStorm storm = new ActiveHoneyStorm(
                cloudCenter,
                center.y,
                ownerId,
                now,
                now + durationTicks,
                radius,
                diveInterval
        );
        storms.add(storm);

        if (ownerId != null) {
            int cooldownTicks = RuneUseCooldown.fromEffectDuration(durationTicks);
            getCooldowns(world).put(ownerId, CooldownStorage.currentTick(world) + cooldownTicks);
            RuneUseCooldown.start(world, ownerId, "bee-chaos", "bee", cooldownTicks);
        }

        world.playSound(null, cloudCenter.x, cloudCenter.y, cloudCenter.z, SoundEvents.ITEM_HONEY_BOTTLE_DRINK, SoundCategory.PLAYERS, 0.8F, 0.8F + world.random.nextFloat() * 0.1F);
        world.playSound(null, cloudCenter.x, cloudCenter.y, cloudCenter.z, SoundEvents.ENTITY_BEE_LOOP, SoundCategory.PLAYERS, 0.9F, 0.85F + world.random.nextFloat() * 0.1F);
        world.spawnParticles(ParticleTypes.FALLING_HONEY, cloudCenter.x, cloudCenter.y, cloudCenter.z, 18, radius * 0.2, 0.2, radius * 0.2, 0.01);
        world.spawnParticles(ParticleTypes.POOF, cloudCenter.x, cloudCenter.y, cloudCenter.z, 24, radius * 0.4, 0.2, radius * 0.4, 0.01);
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, cloudCenter.x, cloudCenter.y + 0.2, cloudCenter.z, 16, radius * 0.35, 0.12, radius * 0.35, 0.01);
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 40L == 0L) {
            long now = CooldownStorage.currentTick(world);
            getCooldowns(world).entrySet().removeIf(entry -> entry.getValue() <= now);
        }

        List<ActiveHoneyStorm> storms = ACTIVE_STORMS.get(world);
        if (storms == null || storms.isEmpty()) {
            return;
        }

        Iterator<ActiveHoneyStorm> iterator = storms.iterator();
        while (iterator.hasNext()) {
            ActiveHoneyStorm storm = iterator.next();
            if (storm.fadeStartTick > 0L) {
                long age = world.getTime() - storm.fadeStartTick;
                if (age >= HONEY_FADE_TICKS) {
                    iterator.remove();
                    continue;
                }
                spawnAmbientParticles(world, storm, 1.0F - (float) age / (float) HONEY_FADE_TICKS);
                continue;
            }
            if (world.getTime() >= storm.expiryTick) {
                storm.fadeStartTick = world.getTime();
                world.playSound(null, storm.center.x, storm.center.y, storm.center.z, SoundEvents.BLOCK_BEEHIVE_DRIP, SoundCategory.PLAYERS, 0.65F, 0.9F);
                world.spawnParticles(ParticleTypes.WAX_OFF, storm.center.x, storm.center.y, storm.center.z, 10, storm.radius * 0.2, 0.12, storm.radius * 0.2, 0.01);
                continue;
            }
            tickStorm(world, storm);
        }

        if (storms.isEmpty()) {
            ACTIVE_STORMS.remove(world);
        }
    }

    private static void tickStorm(ServerWorld world, ActiveHoneyStorm storm) {
        spawnAmbientParticles(world, storm, 1.0F);

        long now = world.getTime();
        if (now >= storm.nextAuraTick) {
            applyAura(world, storm);
            storm.nextAuraTick = now + Math.max(1, SimplyBowsConfig.INSTANCE.buzzkill.chaosAuraIntervalTicks.get());
        }

        if (now >= storm.nextDiveTick) {
            triggerDiveBomb(world, storm);
            storm.nextDiveTick = now + storm.diveIntervalTicks;
        }
    }

    private static void applyAura(ServerWorld world, ActiveHoneyStorm storm) {
        LivingEntity owner = getOwnerEntity(world, storm.ownerId);
        if (owner == null || !owner.isAlive()) {
            return;
        }

        int slownessDuration = Math.max(1, SimplyBowsConfig.INSTANCE.buzzkill.chaosSlownessDurationTicks.get());
        int slownessAmplifier = Math.max(0, SimplyBowsConfig.INSTANCE.buzzkill.chaosSlownessAmplifier.get());

        Box box = new Box(
                storm.center.x - storm.radius,
                storm.groundY - 0.5,
                storm.center.z - storm.radius,
                storm.center.x + storm.radius,
                storm.center.y + 0.8,
                storm.center.z + storm.radius
        );
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (horizontalDistanceSquared(candidate.getPos(), storm.center) > storm.radius * storm.radius) {
                continue;
            }
            if (candidate.getY() < storm.groundY - 0.5 || candidate.getY() > storm.center.y + 0.8) {
                continue;
            }

            if (CombatTargeting.isOffensiveTargetCandidate(candidate)
                    && CombatTargeting.checkFriendlyFire(candidate, owner)) {
                candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slownessDuration, slownessAmplifier), owner);
            }
        }
    }

    private static void triggerDiveBomb(ServerWorld world, ActiveHoneyStorm storm) {
        LivingEntity owner = getOwnerEntity(world, storm.ownerId);
        if (owner == null || !owner.isAlive()) {
            return;
        }

        LivingEntity target = findRandomHostileInStorm(world, storm, owner);
        Vec3d impactPos;
        if (target != null) {
            impactPos = target.getPos().add(0.0, target.getHeight() * 0.4, 0.0);
        } else {
            double angle = world.random.nextDouble() * Math.PI * 2.0;
            double distance = world.random.nextDouble() * storm.radius;
            double x = storm.center.x + Math.cos(angle) * distance;
            double z = storm.center.z + Math.sin(angle) * distance;
            double y = findGroundTopY(world, x, z, storm.center.y) + 0.1;
            impactPos = new Vec3d(x, y, z);
        }

        Vec3d start = new Vec3d(
                storm.center.x + (world.random.nextDouble() - 0.5) * Math.min(storm.radius, 3.0),
                storm.center.y + (world.random.nextDouble() - 0.5) * 0.6,
                storm.center.z + (world.random.nextDouble() - 0.5) * Math.min(storm.radius, 3.0)
        );
        spawnDiveBombBee(world, owner, start, impactPos);
    }

    private static void spawnDiveBombBee(ServerWorld world, LivingEntity owner, Vec3d start, Vec3d impactPos) {
        if (owner == null || start == null || impactPos == null) {
            return;
        }

        BeeArrowEntity diveBee = new BeeArrowEntity(world, owner, new ItemStack(Items.ARROW), BowUpgradeData.none());
        diveBee.setPosition(start.x, start.y, start.z);
        Vec3d direction = impactPos.subtract(start);
        if (direction.lengthSquared() <= 1.0E-6) {
            direction = new Vec3d(0.0, -1.0, 0.0);
        }
        Vec3d velocity = direction.normalize().multiply(1.6);
        diveBee.setVelocity(velocity.x, velocity.y, velocity.z);
        diveBee.setCritical(false);
        diveBee.setChaosDiveBomb(
                SimplyBowsConfig.INSTANCE.buzzkill.chaosDiveDamage.get(),
                SimplyBowsConfig.INSTANCE.buzzkill.chaosDiveImpactRadius.get()
        );
        world.spawnEntity(diveBee);

        world.spawnParticles(ParticleTypes.CRIT, start.x, start.y, start.z, 4, 0.06, 0.06, 0.06, 0.0);
        world.playSound(null, start.x, start.y, start.z, SoundEvents.ENTITY_BEE_LOOP, SoundCategory.PLAYERS, 0.4F, 1.25F + world.random.nextFloat() * 0.15F);
    }

    private static void spawnAmbientParticles(ServerWorld world, ActiveHoneyStorm storm, float presence) {
        presence = Math.max(0.0F, Math.min(1.0F, presence));
        if (presence <= 0.02F) {
            return;
        }
        double radius = storm.radius * presence;
        int honeyCount = Math.max(1, Math.round(3.0F * presence));
        for (int i = 0; i < honeyCount; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2.0;
            double distance = MathHelper.lerp(world.random.nextDouble(), radius * 0.1, radius);
            double x = storm.center.x + Math.cos(angle) * distance;
            double z = storm.center.z + Math.sin(angle) * distance;
            double y = storm.groundY + 0.12 + world.random.nextDouble() * 0.35 * presence;
            world.spawnParticles(ParticleTypes.WAX_ON, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        if (world.getTime() % 3L == 0L) {
            world.spawnParticles(ParticleTypes.POOF, storm.center.x, storm.groundY + 0.2, storm.center.z, Math.max(1, Math.round(3.0F * presence)), radius * 0.35, 0.05, radius * 0.35, 0.0);
        }
    }

    private static LivingEntity findRandomHostileInStorm(ServerWorld world, ActiveHoneyStorm storm, LivingEntity owner) {
        Box box = Box.of(storm.center, storm.radius * 2.0, 6.0, storm.radius * 2.0);
        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, box, entity ->
                CombatTargeting.isOffensiveTargetCandidate(entity)
                        && CombatTargeting.checkFriendlyFire(entity, owner)
                        && horizontalDistanceSquared(entity.getPos(), storm.center) <= storm.radius * storm.radius);
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(world.random.nextInt(candidates.size()));
    }

    private static double findGroundTopY(ServerWorld world, double x, double z, double centerY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = (int) Math.floor(centerY) + 6;
        int minY = Math.max(world.getBottomY(), (int) Math.floor(centerY) - 14);
        for (int y = startY; y >= minY; y--) {
            net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, net.minecraft.util.math.Direction.UP)) {
                return y + 1.0;
            }
        }
        return centerY;
    }

    private static LivingEntity getOwnerEntity(ServerWorld world, UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        Entity owner = world.getEntity(ownerId);
        return owner instanceof LivingEntity living ? living : null;
    }

    private static Map<UUID, Long> getCooldowns(ServerWorld world) {
        return CooldownStorage.forWorld(STORM_COOLDOWNS_BY_SERVER, world);
    }

    private static double horizontalDistanceSquared(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static final class ActiveHoneyStorm {
        private final Vec3d center;
        private final double groundY;
        private final UUID ownerId;
        private final long spawnTick;
        private final long expiryTick;
        private final double radius;
        private final int diveIntervalTicks;
        private final Map<UUID, Long> recentTargetHitTicks = new HashMap<>();
        private long nextAuraTick;
        private long nextDiveTick;
        private long fadeStartTick;

        private ActiveHoneyStorm(Vec3d center, double groundY, UUID ownerId, long spawnTick, long expiryTick, double radius, int diveIntervalTicks) {
            this.center = center;
            this.groundY = groundY;
            this.ownerId = ownerId;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
            this.radius = radius;
            this.diveIntervalTicks = Math.max(1, diveIntervalTicks);
            this.nextAuraTick = spawnTick;
            this.nextDiveTick = spawnTick + Math.max(8, this.diveIntervalTicks / 2);
        }
    }
}
