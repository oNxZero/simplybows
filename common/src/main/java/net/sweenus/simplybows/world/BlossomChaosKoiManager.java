package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.KoiFishVisualEntity;
import net.sweenus.simplybows.registry.ParticleRegistry;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BlossomChaosKoiManager {

    private static final Map<ServerWorld, List<ActiveKoiEffect>> ACTIVE_EFFECTS = new HashMap<>();
    // Vanilla tropical fish base colors (15 dye-based colors)
    private static final int[] KOI_BASE_PALETTE = new int[]{
            0xF9FFFE, // white
            0xF9801D, // orange
            0xC74EBD, // magenta
            0x3AB3DA, // light blue
            0xFED83D, // yellow
            0x80C71F, // lime
            0xF38BAA, // pink
            0x474F52, // gray
            0x9D9D97, // light gray
            0x169C9C, // cyan
            0x8932B8, // purple
            0x3C44AA, // blue
            0x835432, // brown
            0x5E7C16, // green
            0xB02E26  // red
    };

    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private static final int FADE_IN_TICKS = 20;
    private static final int FADE_OUT_TICKS = 25;
    private static final String KOI_VISUAL_TAG = "simplybows_blossom_koi_visual";
    private static final double KOI_CONTACT_VERTICAL_RANGE = 1.4;

    private BlossomChaosKoiManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveKoiEffect> effects = ACTIVE_EFFECTS.get(world);
        return (effects != null && !effects.isEmpty()) || (world.getTime() % 20L == 0L);
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    public static void createEffect(ServerWorld world, Vec3d impactPos, LivingEntity directTarget,
                                    Entity owner, BowUpgradeData upgrades) {
        if (world == null || impactPos == null) {
            return;
        }

        UUID ownerId = owner != null ? owner.getUuid() : null;
        if (ownerId != null && !RuneUseCooldown.isReady(world, ownerId, "blossom-chaos")) {
            return;
        }
        // Replace any previous school for this owner so ground casts aren't blocked.
        if (ownerId != null) {
            removeOwnerEffects(world, ownerId);
        }

        int stringLevel = upgrades != null ? Math.max(0, upgrades.stringLevel()) : 0;
        int frameLevel  = upgrades != null ? Math.max(0, upgrades.frameLevel())  : 0;

        var cfg = SimplyBowsConfig.INSTANCE.petalwind;

        int durationTicks = Math.max(40, cfg.chaosDurationTicks.get() + frameLevel * cfg.chaosDurationPerFrameTicks.get());
        double radius = Math.max(1.5, cfg.chaosRadius.get() + stringLevel * cfg.chaosRadiusPerString.get());
        float swimRadius = cfg.chaosKoiSwimRadius.get() + frameLevel * cfg.chaosKoiSwimRadiusPerFrame.get();
        int fishCount = Math.max(1, cfg.chaosBaseFishCount.get() + frameLevel * cfg.chaosFishPerFrame.get());
        float orbitPeriodTicks = Math.max(
                cfg.chaosMinOrbitPeriodTicks.get(),
                cfg.chaosBaseOrbitPeriodTicks.get() - stringLevel * cfg.chaosOrbitPeriodReductionPerStringTicks.get()
        );

        UUID anchorId = directTarget != null && directTarget.isAlive() ? directTarget.getUuid() : null;
        // Visual koi render ~1 block below entity Y (trail/model offset). Ground casts need extra height
        // or the school clips into the floor and looks like "nothing spawned".
        Vec3d effectCenter = anchorId != null
                ? directTarget.getPos().add(0.0, Math.max(1.0, directTarget.getHeight() * 0.55), 0.0)
                : new Vec3d(impactPos.x, impactPos.y + 1.55, impactPos.z);

        List<FishRef> fishRefs = new ArrayList<>();
        for (int i = 0; i < fishCount; i++) {
            float phase = TWO_PI * i / fishCount;
            double spawnX = effectCenter.x + swimRadius * Math.cos(phase);
            double spawnY = effectCenter.y + 0.2;
            double spawnZ = effectCenter.z + swimRadius * Math.sin(phase);

            KoiFishVisualEntity fish = new KoiFishVisualEntity(world, spawnX, spawnY, spawnZ);
            fish.addCommandTag(KOI_VISUAL_TAG);
            fish.setLargeVariant(false);
            fish.setBaseColorRgb(KOI_BASE_PALETTE[i % KOI_BASE_PALETTE.length]);
            fish.setPatternIndex(i % 6);
            world.spawnEntity(fish);

            fishRefs.add(new FishRef(fish.getUuid(), phase));
        }

        long now = world.getTime();
        ActiveKoiEffect effect = new ActiveKoiEffect(
                effectCenter,
                ownerId,
                anchorId,
                now,
                now + durationTicks,
                radius,
                swimRadius,
                orbitPeriodTicks,
                fishRefs,
                Math.max(0.0F, cfg.chaosContactDamage.get()),
                Math.max(0.0, cfg.chaosContactKnockbackHorizontal.get()),
                Math.max(0.0, cfg.chaosContactKnockbackVertical.get()),
                Math.max(1, cfg.chaosContactCooldownTicks.get()),
                Math.max(0.25, cfg.chaosTouchRadius.get()),
                Math.max(0.1, cfg.chaosProjectileReflectSpeedMultiplier.get()),
                Math.max(1, cfg.chaosProjectileReflectCooldownTicks.get())
        );

        ACTIVE_EFFECTS.computeIfAbsent(world, ignored -> new ArrayList<>()).add(effect);
        RuneUseCooldown.startForEffect(world, ownerId, "blossom-chaos", "blossom", durationTicks);

        world.spawnParticles(ParticleTypes.CHERRY_LEAVES,
                impactPos.x, impactPos.y + 0.5, impactPos.z, 30, radius * 0.3, 0.5, radius * 0.3, 0.02);
        world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                effectCenter.x, effectCenter.y, effectCenter.z, 20, radius * 0.2, 0.08, radius * 0.2, 0.01);
        world.spawnParticles(ParticleTypes.POOF,
                impactPos.x, impactPos.y + 0.5, impactPos.z, 12, 0.3, 0.3, 0.3, 0.02);
        for (int i = 0; i < 16; i++) {
            double wa = world.random.nextDouble() * TWO_PI;
            double wd = world.random.nextDouble() * radius * 0.7;
            world.spawnParticles(ParticleRegistry.JAPANESE_WAVE.get(),
                    effectCenter.x + Math.cos(wa) * wd,
                    effectCenter.y + (world.random.nextDouble() - 0.5) * 0.12,
                    effectCenter.z + Math.sin(wa) * wd,
                    1, 0.02, 0.01, 0.02, 0.0);
        }

        world.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.ENTITY_SALMON_FLOP, SoundCategory.PLAYERS, 0.9F, 0.85F + world.random.nextFloat() * 0.15F);
        world.playSound(null, impactPos.x, impactPos.y, impactPos.z,
                SoundEvents.BLOCK_CHERRY_LEAVES_PLACE, SoundCategory.PLAYERS, 0.85F, 0.9F + world.random.nextFloat() * 0.15F);
    }

    public static void tick(ServerWorld world) {
        List<ActiveKoiEffect> effects = ACTIVE_EFFECTS.get(world);
        if (effects == null || effects.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanKoiVisuals(world);
            }
            return;
        }

        Iterator<ActiveKoiEffect> it = effects.iterator();
        while (it.hasNext()) {
            ActiveKoiEffect effect = it.next();
            long now = world.getTime();

            if (now >= effect.expiryTick) {
                expireEffect(world, effect);
                it.remove();
                continue;
            }

            if (effect.fishes.isEmpty()) {
                it.remove();
                continue;
            }

            tickEffect(world, effect, now);
        }

        if (effects.isEmpty()) {
            ACTIVE_EFFECTS.remove(world);
            purgeOrphanKoiVisuals(world);
        }
    }

    private static void tickEffect(ServerWorld world, ActiveKoiEffect effect, long now) {
        if (effect.anchorId != null) {
            Entity anchor = world.getEntity(effect.anchorId);
            if (anchor instanceof LivingEntity living && living.isAlive()) {
                effect.center = living.getPos().add(0.0, living.getHeight() * 0.5, 0.0);
            }
        }

        long age = now - effect.spawnTick;
        long remaining = effect.expiryTick - now;

        float visualScale;
        if (age < FADE_IN_TICKS) {
            visualScale = (float) age / FADE_IN_TICKS;
        } else if (remaining < FADE_OUT_TICKS) {
            visualScale = (float) remaining / FADE_OUT_TICKS;
        } else {
            visualScale = 1.0F;
        }
        visualScale = MathHelper.clamp(visualScale, 0.0F, 1.0F);

        effect.orbitAngle += TWO_PI / effect.orbitPeriodTicks;
        if (effect.orbitAngle > TWO_PI) {
            effect.orbitAngle -= TWO_PI;
        }

        Iterator<FishRef> fishIt = effect.fishes.iterator();
        while (fishIt.hasNext()) {
            FishRef fishRef = fishIt.next();
            KoiFishVisualEntity koi = getKoiEntity(world, fishRef.entityId);
            if (koi == null || !koi.isAlive()) {
                fishIt.remove();
                continue;
            }

            float fishAngle = effect.orbitAngle + fishRef.phaseOffset;
            double koiX = effect.center.x + effect.swimRadius * Math.cos(fishAngle);
            double koiY = effect.center.y + 0.2;
            double koiZ = effect.center.z + effect.swimRadius * Math.sin(fishAngle);

            koi.setVisualScale(visualScale);
            koi.setPosition(koiX, koiY, koiZ);
            double tanX = -Math.sin(fishAngle);
            double tanZ = Math.cos(fishAngle);
            float swimYaw = (float) (Math.atan2(tanX, tanZ) * (180.0 / Math.PI));
            koi.setSwimAngle(swimYaw);

            spawnKoiTrail(world, fishAngle, koiX, koiY, koiZ);
            if (now % 3L == 0L) {
                spawnWaveParticles(world, fishAngle, koiX, koiY, koiZ);
                spawnBlossomBehindKoi(world, fishAngle, koiX, koiY, koiZ);
            }

            handleEntityContact(world, effect, koi, now);
            handleProjectileReflection(world, effect, koi, now);
        }
    }

    private static void spawnWaveParticles(ServerWorld world,
                                           float fishAngle, double koiX, double koiY, double koiZ) {
        double trailAngle = fishAngle + Math.PI;
        double trailX = koiX + Math.cos(trailAngle) * 0.55 + Math.cos(fishAngle) * 0.5;
        double trailZ = koiZ + Math.sin(trailAngle) * 0.55 + Math.sin(fishAngle) * 0.5;
        world.spawnParticles(ParticleRegistry.JAPANESE_WAVE.get(),
                trailX + (world.random.nextDouble() - 0.5) * 0.10,
                (koiY - 1.0) - 0.28 + world.random.nextDouble() * 0.20,
                trailZ + (world.random.nextDouble() - 0.5) * 0.10,
                1, 0.004, 0.002, 0.004, 0.0);
    }

    private static void spawnKoiTrail(ServerWorld world, float fishAngle,
                                      double koiX, double koiY, double koiZ) {
        double trailAngle = fishAngle + Math.PI;
        double trailX = koiX + Math.cos(trailAngle) * 0.6 + Math.cos(fishAngle) * 0.5;
        double trailZ = koiZ + Math.sin(trailAngle) * 0.6 + Math.sin(fishAngle) * 0.5;
        for (int i = 0; i < 2; i++) {
            world.spawnParticles(ParticleRegistry.JAPANESE_WAVE.get(),
                    trailX + (world.random.nextDouble() - 0.5) * 0.25,
                    (koiY - 1.45) + (world.random.nextDouble() - 0.5) * 0.16,
                    trailZ + (world.random.nextDouble() - 0.5) * 0.25,
                    1, 0.02, 0.01, 0.02, 0.0);
        }
        world.spawnParticles(ParticleTypes.CHERRY_LEAVES,
                trailX, koiY - 1.3, trailZ, 1, 0.08, 0.04, 0.08, 0.004);
    }

    private static void spawnBlossomBehindKoi(ServerWorld world,
                                               float fishAngle, double koiX, double koiY, double koiZ) {
        double trailAngle = fishAngle + Math.PI;
        // Different anchoring approach: lock blossom spawn to the same trailing frame as wave trails.
        // This removes dependence on fish model/entity origin mismatch.
        double blossomX = koiX + Math.cos(trailAngle) * 0.60 + Math.cos(fishAngle) * 0.5;
        double blossomZ = koiZ + Math.sin(trailAngle) * 0.60 + Math.sin(fishAngle) * 0.5;
        double blossomY = (koiY - 1.0) - 0.20;

        world.spawnParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                blossomX, blossomY, blossomZ, 1, 0.05, 0.01, 0.05, 0.0);
    }

    private static void handleEntityContact(ServerWorld world, ActiveKoiEffect effect,
                                            KoiFishVisualEntity koi, long now) {
        LivingEntity owner = getOwnerEntity(world, effect.ownerId);
        if (owner != null && !owner.isAlive()) {
            return;
        }

        double hitRadiusSq = effect.touchRadius * effect.touchRadius;
        Box box = koi.getBoundingBox().expand(effect.touchRadius, KOI_CONTACT_VERTICAL_RANGE, effect.touchRadius);
        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (candidate == owner) {
                continue;
            }
            double dx = candidate.getX() - koi.getX();
            double dz = candidate.getZ() - koi.getZ();
            if ((dx * dx + dz * dz) > hitRadiusSq) {
                continue;
            }
            if (Math.abs(candidate.getBodyY(0.5) - koi.getY()) > KOI_CONTACT_VERTICAL_RANGE) {
                continue;
            }
            if (!CombatTargeting.isOffensiveTargetCandidate(candidate)) {
                continue;
            }
            if (owner != null && !CombatTargeting.checkFriendlyFire(candidate, owner)) {
                continue;
            }

            long readyAt = effect.nextDamageTickByTarget.getOrDefault(candidate.getUuid(), 0L);
            if (now < readyAt) {
                continue;
            }

            boolean damaged = effect.contactDamage > 0.0F
                    && CombatTargeting.applyDamage(world, owner, candidate, effect.contactDamage, true, true);
            if (damaged || effect.contactDamage <= 0.0F) {
                effect.nextDamageTickByTarget.put(candidate.getUuid(), now + effect.contactCooldownTicks);
                Vec3d push = candidate.getPos().subtract(koi.getPos());
                if (push.lengthSquared() < 1.0E-6) {
                    push = new Vec3d(world.random.nextDouble() - 0.5, 0.0, world.random.nextDouble() - 0.5);
                }
                push = push.normalize().multiply(effect.contactKnockbackHorizontal);
                candidate.addVelocity(push.x, effect.contactKnockbackVertical, push.z);
                candidate.velocityDirty = true;
                world.spawnParticles(ParticleTypes.CRIT,
                        candidate.getX(), candidate.getBodyY(0.5), candidate.getZ(),
                        6, 0.2, 0.2, 0.2, 0.05);
                world.playSound(null, candidate.getX(), candidate.getY(), candidate.getZ(),
                        SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.35F,
                        1.3F + world.random.nextFloat() * 0.2F);
            }
        }
    }

    private static void handleProjectileReflection(ServerWorld world, ActiveKoiEffect effect,
                                                   KoiFishVisualEntity koi, long now) {
        LivingEntity owner = getOwnerEntity(world, effect.ownerId);
        Box box = koi.getBoundingBox().expand(effect.touchRadius, KOI_CONTACT_VERTICAL_RANGE, effect.touchRadius);

        for (ProjectileEntity projectile : world.getEntitiesByClass(ProjectileEntity.class, box, p -> p.isAlive() && !p.isRemoved())) {
            if (!isHostileProjectile(owner, projectile)) {
                continue;
            }

            long readyAt = effect.nextReflectTickByProjectile.getOrDefault(projectile.getUuid(), 0L);
            if (now < readyAt) {
                continue;
            }

            Vec3d away = projectile.getPos().subtract(koi.getPos());
            if (away.lengthSquared() < 1.0E-6) {
                away = new Vec3d(world.random.nextDouble() - 0.5, 0.2, world.random.nextDouble() - 0.5);
            }
            away = away.normalize();

            Vec3d velocity = projectile.getVelocity();
            double speed = Math.max(0.35, velocity.length()) * effect.reflectSpeedMultiplier;
            Vec3d reflectedVelocity = away.multiply(speed).add(0.0, 0.04, 0.0);

            projectile.setVelocity(reflectedVelocity.x, reflectedVelocity.y, reflectedVelocity.z);
            projectile.velocityDirty = true;
            projectile.velocityModified = true;
            if (owner != null) {
                projectile.setOwner(owner);
            }

            effect.nextReflectTickByProjectile.put(projectile.getUuid(), now + effect.reflectCooldownTicks);
            world.spawnParticles(ParticleTypes.SPLASH,
                    projectile.getX(), projectile.getY(), projectile.getZ(), 5,
                    0.12, 0.12, 0.12, 0.03);
            world.spawnParticles(ParticleRegistry.JAPANESE_WAVE.get(),
                    projectile.getX(), projectile.getY(), projectile.getZ(), 2,
                    0.02, 0.02, 0.02, 0.0);
            world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.55F,
                    1.6F + world.random.nextFloat() * 0.15F);
        }
    }

    private static boolean isHostileProjectile(LivingEntity owner, ProjectileEntity projectile) {
        Entity projectileOwner = projectile.getOwner();
        if (projectileOwner == null) {
            return true;
        }
        if (owner != null && projectileOwner == owner) {
            return false;
        }
        if (owner == null || !(projectileOwner instanceof LivingEntity livingOwner)) {
            return true;
        }
        return CombatTargeting.checkFriendlyFire(owner, livingOwner);
    }

    private static void removeOwnerEffects(ServerWorld world, UUID ownerId) {
        List<ActiveKoiEffect> effects = ACTIVE_EFFECTS.get(world);
        if (effects == null || effects.isEmpty()) {
            return;
        }
        effects.removeIf(effect -> {
            if (!ownerId.equals(effect.ownerId)) {
                return false;
            }
            expireEffect(world, effect);
            return true;
        });
        if (effects.isEmpty()) {
            ACTIVE_EFFECTS.remove(world);
        }
    }

    private static void expireEffect(ServerWorld world, ActiveKoiEffect effect) {
        world.spawnParticles(ParticleTypes.CHERRY_LEAVES,
                effect.center.x, effect.center.y + 1.5, effect.center.z, 24,
                effect.radius * 0.4, 0.6, effect.radius * 0.4, 0.02);
        world.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                effect.center.x, effect.center.y + 1.0, effect.center.z, 16,
                effect.radius * 0.3, 0.3, effect.radius * 0.3, 0.01);
        world.playSound(null, effect.center.x, effect.center.y, effect.center.z,
                SoundEvents.BLOCK_CHERRY_LEAVES_BREAK, SoundCategory.PLAYERS, 0.7F, 0.9F);

        for (FishRef fishRef : effect.fishes) {
            KoiFishVisualEntity koi = getKoiEntity(world, fishRef.entityId);
            if (koi != null && koi.isAlive()) {
                koi.setVisualScale(0.0F);
                koi.discard();
            }
        }
        effect.fishes.clear();
    }

    private static void purgeOrphanKoiVisuals(ServerWorld world) {
        List<ActiveKoiEffect> effects = ACTIVE_EFFECTS.get(world);
        var activeIds = new java.util.HashSet<UUID>();
        if (effects != null) {
            for (ActiveKoiEffect effect : effects) {
                for (FishRef fishRef : effect.fishes) {
                    activeIds.add(fishRef.entityId);
                }
            }
        }

        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof KoiFishVisualEntity koi)) {
                continue;
            }
            if (!koi.getCommandTags().contains(KOI_VISUAL_TAG)) {
                continue;
            }
            if (!activeIds.contains(koi.getUuid())) {
                koi.discard();
            }
        }
    }

    private static KoiFishVisualEntity getKoiEntity(ServerWorld world, UUID uuid) {
        if (uuid == null) return null;
        Entity e = world.getEntity(uuid);
        return e instanceof KoiFishVisualEntity koi ? koi : null;
    }

    private static LivingEntity getOwnerEntity(ServerWorld world, UUID ownerId) {
        if (ownerId == null) return null;
        Entity e = world.getEntity(ownerId);
        return e instanceof LivingEntity living ? living : null;
    }

    private static Vec3d resolveCenter(Entity owner, Vec3d fallback) {
        if (owner == null || !owner.isAlive()) {
            return new Vec3d(fallback.x, fallback.y + 1.2, fallback.z);
        }
        return new Vec3d(owner.getX(), owner.getEyeY(), owner.getZ());
    }

    private static final class ActiveKoiEffect {
        private Vec3d center;
        private final UUID ownerId;
        private final UUID anchorId;
        private final long spawnTick;
        private final long expiryTick;
        private final double radius;
        private final float swimRadius;
        private final float orbitPeriodTicks;
        private final List<FishRef> fishes;
        private final float contactDamage;
        private final double contactKnockbackHorizontal;
        private final double contactKnockbackVertical;
        private final int contactCooldownTicks;
        private final double touchRadius;
        private final double reflectSpeedMultiplier;
        private final int reflectCooldownTicks;
        private final Map<UUID, Long> nextDamageTickByTarget;
        private final Map<UUID, Long> nextReflectTickByProjectile;
        private float orbitAngle;

        private ActiveKoiEffect(Vec3d center, UUID ownerId, UUID anchorId, long spawnTick, long expiryTick,
                                double radius, float swimRadius, float orbitPeriodTicks, List<FishRef> fishes,
                                float contactDamage, double contactKnockbackHorizontal, double contactKnockbackVertical,
                                int contactCooldownTicks, double touchRadius,
                                double reflectSpeedMultiplier, int reflectCooldownTicks) {
            this.center = center;
            this.ownerId = ownerId;
            this.anchorId = anchorId;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
            this.radius = radius;
            this.swimRadius = swimRadius;
            this.orbitPeriodTicks = orbitPeriodTicks;
            this.fishes = fishes;
            this.contactDamage = contactDamage;
            this.contactKnockbackHorizontal = contactKnockbackHorizontal;
            this.contactKnockbackVertical = contactKnockbackVertical;
            this.contactCooldownTicks = contactCooldownTicks;
            this.touchRadius = touchRadius;
            this.reflectSpeedMultiplier = reflectSpeedMultiplier;
            this.reflectCooldownTicks = reflectCooldownTicks;
            this.nextDamageTickByTarget = new HashMap<>();
            this.nextReflectTickByProjectile = new HashMap<>();
            this.orbitAngle = 0.0F;
        }
    }

    private static final class FishRef {
        private final UUID entityId;
        private final float phaseOffset;

        private FishRef(UUID entityId, float phaseOffset) {
            this.entityId = entityId;
            this.phaseOffset = phaseOffset;
        }
    }
}
