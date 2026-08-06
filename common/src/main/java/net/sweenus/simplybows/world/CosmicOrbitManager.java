package net.sweenus.simplybows.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.CosmicOrbitVisualEntity;
import net.sweenus.simplybows.entity.CosmicTetherVisualEntity;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CosmicOrbitManager {

    private static final String ORBIT_VISUAL_TAG = "simplybows_cosmic_orbit_visual";
    private static final int FADE_TICKS = 12;
    private static final int SCALE_IN_TICKS = 8;
    private static final double PAIN_TETHER_MIN_DISTANCE = 2.0;

    private static final Map<ServerWorld, List<ActiveOrbit>> ACTIVE_ORBITS = new HashMap<>();
    private static final Map<MinecraftServer, Map<UUID, Long>> PAIN_COOLDOWNS_BY_SERVER = CooldownStorage.newServerScopedStore();

    private CosmicOrbitManager() {
    }

    private static int orbitDurationTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitDurationTicks.get(); }
    private static int orbitDurationBonusPerString() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitDurationBonusPerString.get(); }
    private static double attackRadius() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitAttackRadius.get(); }
    private static float attackDamage() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitAttackDamage.get(); }
    private static int minAttackIntervalTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitAttackIntervalMinTicks.get(); }
    private static int maxAttackIntervalTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitAttackIntervalMaxTicks.get(); }
    private static int maxJumps() { return SimplyBowsConfig.INSTANCE.cosmicBow.orbitMaxJumps.get(); }
    private static int painCooldownTicks() { return SimplyBowsConfig.INSTANCE.cosmicBow.painOrbitCooldownTicks.get(); }
    private static int painMaxTetherTargets() { return SimplyBowsConfig.INSTANCE.cosmicBow.painMaxTetherTargets.get(); }
    private static double painTetherPullStrength() { return SimplyBowsConfig.INSTANCE.cosmicBow.painTetherPullStrength.get(); }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveOrbit> orbits = ACTIVE_ORBITS.get(world);
        return (orbits != null && !orbits.isEmpty()) || !getPainCooldowns(world).isEmpty() || (world.getTime() % 20L == 0L);
    }

    public static void createOrRefresh(ServerWorld world, LivingEntity target, Entity owner, BowUpgradeData upgrades) {
        if (world == null || target == null || !target.isAlive()) {
            return;
        }
        BowUpgradeData orbitUpgrades = upgrades == null ? BowUpgradeData.none() : upgrades;
        UUID ownerId = owner == null ? null : owner.getUuid();
        boolean painMode = orbitUpgrades.runeEtching() == RuneEtching.PAIN;

        if (painMode && ownerId != null && !isPainOrbitReady(world, ownerId)) {
            return;
        }

        List<ActiveOrbit> orbits = ACTIVE_ORBITS.computeIfAbsent(world, ignored -> new ArrayList<>());
        ActiveOrbit existing = null;
        for (ActiveOrbit orbit : orbits) {
            if (target.getUuid().equals(orbit.targetId)) {
                existing = orbit;
                break;
            }
        }

        long now = world.getTime();
        if (existing != null) {
            existing.ownerId = ownerId;
            existing.upgrades = orbitUpgrades;
            existing.painMode = painMode;
            existing.targetId = target.getUuid();
            existing.visitedTargets.clear();
            existing.visitedTargets.add(target.getUuid());
            existing.jumpsUsed = 0;
            existing.spawnTick = now;
            existing.expiryTick = now + getOrbitDurationTicks(orbitUpgrades);
            existing.nextLeapTick = now + randomLeapInterval(world);
            ensureVisual(world, target, existing);
            return;
        }

        Vec3d center = getOrbitCenter(target);
        CosmicOrbitVisualEntity visual = new CosmicOrbitVisualEntity(world, center.x, center.y, center.z);
        visual.setPainMode(painMode);
        visual.addCommandTag(ORBIT_VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            return;
        }

        ActiveOrbit orbit = new ActiveOrbit(
                target.getUuid(),
                ownerId,
                visual.getUuid(),
                orbitUpgrades,
                painMode,
                now,
                now + getOrbitDurationTicks(orbitUpgrades),
                now + randomLeapInterval(world)
        );
        orbit.visitedTargets.add(target.getUuid());
        orbits.add(orbit);
        if (painMode && ownerId != null) {
            startPainCooldown(world, ownerId);
            if (owner instanceof ServerPlayerEntity player) {
                int cooldownTicks = Math.max(20, painCooldownTicks());
                SimplyBowItem.simplybows$sendCooldownPacket(player, "cosmic", System.currentTimeMillis() + cooldownTicks * 50L, cooldownTicks);
            }
        }

        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.7F, 1.45F + world.random.nextFloat() * 0.12F);
        world.spawnParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 8, 0.24, 0.24, 0.24, 0.01);
    }

    public static void tick(ServerWorld world) {
        if (world.getTime() % 40L == 0L) {
            long now = CooldownStorage.currentTick(world);
            getPainCooldowns(world).entrySet().removeIf(entry -> entry.getValue() <= now);
        }

        List<ActiveOrbit> orbits = ACTIVE_ORBITS.get(world);
        if (orbits == null || orbits.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanVisuals(world);
            }
            return;
        }

        long now = world.getTime();
        orbits.removeIf(orbit -> {
            LivingEntity target = getLivingEntity(world, orbit.targetId);
            if (target == null || !target.isAlive() || now > orbit.expiryTick) {
                discardTetherVisuals(world, orbit);
                discardVisual(world, orbit.visualId);
                return true;
            }
            Entity visualEntity = world.getEntity(orbit.visualId);
            if (!(visualEntity instanceof CosmicOrbitVisualEntity)) {
                discardTetherVisuals(world, orbit);
                return true;
            }
            return false;
        });

        if (orbits.isEmpty()) {
            ACTIVE_ORBITS.remove(world);
            purgeOrphanVisuals(world);
            return;
        }

        for (ActiveOrbit orbit : orbits) {
            LivingEntity target = getLivingEntity(world, orbit.targetId);
            if (target == null) {
                continue;
            }
            updateVisual(world, orbit, target);
            updatePainTethers(world, orbit, target);
            tryLeap(world, orbit, target);
        }
    }

    private static void updateVisual(ServerWorld world, ActiveOrbit orbit, LivingEntity target) {
        Entity visualEntity = world.getEntity(orbit.visualId);
        if (!(visualEntity instanceof CosmicOrbitVisualEntity visual)) {
            return;
        }

        visual.setPainMode(orbit.painMode);
        Vec3d center = getOrbitCenter(target);
        visual.setPos(center.x, center.y, center.z);

        long now = world.getTime();
        float scaleIn = MathHelper.clamp((float) (now - orbit.spawnTick) / SCALE_IN_TICKS, 0.0F, 1.0F);
        float fadeOut = MathHelper.clamp((float) (orbit.expiryTick - now) / FADE_TICKS, 0.0F, 1.0F);
        visual.setVisualScale(Math.min(scaleIn, fadeOut));
    }

    private static void tryLeap(ServerWorld world, ActiveOrbit orbit, LivingEntity anchor) {
        if (orbit.painMode) {
            return;
        }
        long now = world.getTime();
        if (now < orbit.nextLeapTick || orbit.jumpsUsed >= maxJumps()) {
            return;
        }

        List<LivingEntity> targets = findNearbyTargets(world, orbit, anchor);
        if (targets.isEmpty()) {
            orbit.nextLeapTick = now + randomLeapInterval(world);
            return;
        }

        LivingEntity target = targets.get(world.random.nextInt(targets.size()));
        leapToTarget(world, orbit, anchor, target);
    }

    private static void updatePainTethers(ServerWorld world, ActiveOrbit orbit, LivingEntity anchor) {
        if (!orbit.painMode) {
            return;
        }

        List<LivingEntity> targets = findPainTetherTargets(world, orbit, anchor);
        List<UUID> activeTargetIds = new ArrayList<>();
        Vec3d start = getOrbitCenter(anchor);
        for (LivingEntity target : targets) {
            UUID targetId = target.getUuid();
            activeTargetIds.add(targetId);
            if (!orbit.tetherVisualIds.containsKey(targetId)) {
                UUID visualId = spawnTetherVisual(world, start, getEntityCenter(target));
                if (visualId != null) {
                    orbit.tetherVisualIds.put(targetId, visualId);
                }
            }
            updateTetherVisual(world, orbit.tetherVisualIds.get(targetId), start, getEntityCenter(target));
            applyTetherPull(anchor, target);
        }

        orbit.tetherVisualIds.entrySet().removeIf(entry -> {
            if (activeTargetIds.contains(entry.getKey())) {
                return false;
            }
            discardVisual(world, entry.getValue());
            return true;
        });
    }

    private static List<LivingEntity> findPainTetherTargets(ServerWorld world, ActiveOrbit orbit, LivingEntity anchor) {
        double radius = attackRadius();
        Vec3d center = getOrbitCenter(anchor);
        Entity owner = orbit.ownerId == null ? null : world.getEntity(orbit.ownerId);
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
        Box box = Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0);
        double maxDistanceSq = radius * radius;
        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, box, entity ->
                entity != anchor
                        && entity != owner
                        && entity.squaredDistanceTo(center) <= maxDistanceSq
                        && CombatTargeting.isOffensiveTargetCandidate(entity, ownerLiving)
                        && anchor.canSee(entity));
        candidates.sort((a, b) -> Double.compare(a.squaredDistanceTo(center), b.squaredDistanceTo(center)));
        int maxTargets = Math.max(0, painMaxTetherTargets());
        if (candidates.size() > maxTargets) {
            return new ArrayList<>(candidates.subList(0, maxTargets));
        }
        return candidates;
    }

    private static void applyTetherPull(LivingEntity anchor, LivingEntity target) {
        Vec3d anchorCenter = getOrbitCenter(anchor);
        Vec3d targetCenter = getEntityCenter(target);
        Vec3d delta = anchorCenter.subtract(targetCenter);
        double distance = delta.length();
        if (distance <= PAIN_TETHER_MIN_DISTANCE || distance < 1.0E-4) {
            return;
        }
        double strength = Math.max(0.0, painTetherPullStrength()) * Math.min(1.0, (distance - PAIN_TETHER_MIN_DISTANCE) / 4.0);
        target.addVelocity(delta.normalize().multiply(strength));
        target.velocityModified = true;
    }

    private static List<LivingEntity> findNearbyTargets(ServerWorld world, ActiveOrbit orbit, LivingEntity anchor) {
        double radius = attackRadius();
        Vec3d center = getOrbitCenter(anchor);
        Entity owner = orbit.ownerId == null ? null : world.getEntity(orbit.ownerId);
        LivingEntity ownerLiving = owner instanceof LivingEntity living ? living : null;
        Box box = Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0);
        double maxDistanceSq = radius * radius;

        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, box, entity ->
                entity != anchor
                        && entity != owner
                        && entity.squaredDistanceTo(center) <= maxDistanceSq
                        && !orbit.visitedTargets.contains(entity.getUuid())
                        && CombatTargeting.isOffensiveTargetCandidate(entity, ownerLiving)
                        && anchor.canSee(entity));
        candidates.sort((a, b) -> Double.compare(a.squaredDistanceTo(center), b.squaredDistanceTo(center)));
        return candidates;
    }

    private static void leapToTarget(ServerWorld world, ActiveOrbit orbit, LivingEntity anchor, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }

        Entity owner = orbit.ownerId == null ? null : world.getEntity(orbit.ownerId);
        Vec3d end = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.55, 0.0);

        CombatTargeting.applyDamage(world, owner, target, getLeapDamage(orbit.upgrades), true, false);
        orbit.targetId = target.getUuid();
        orbit.visitedTargets.add(target.getUuid());
        orbit.jumpsUsed++;
        orbit.nextLeapTick = world.getTime() + randomLeapInterval(world);

        if (orbit.jumpsUsed >= maxJumps()) {
            orbit.expiryTick = Math.min(orbit.expiryTick, world.getTime() + FADE_TICKS);
        }

        playImpactSound(world, end);
        world.spawnParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 4, 0.12, 0.12, 0.12, 0.0);
    }

    private static void playImpactSound(ServerWorld world, Vec3d pos) {
        float chimePitch = 0.95F + world.random.nextFloat() * 0.18F;
        float anchorPitch = 1.65F + world.random.nextFloat() * 0.16F;
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.PLAYERS, 0.75F, chimePitch);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.PLAYERS, 0.28F, anchorPitch);
    }

    private static void ensureVisual(ServerWorld world, LivingEntity target, ActiveOrbit orbit) {
        Entity entity = world.getEntity(orbit.visualId);
        if (entity instanceof CosmicOrbitVisualEntity) {
            ((CosmicOrbitVisualEntity) entity).setPainMode(orbit.painMode);
            return;
        }

        Vec3d center = getOrbitCenter(target);
        CosmicOrbitVisualEntity visual = new CosmicOrbitVisualEntity(world, center.x, center.y, center.z);
        visual.setPainMode(orbit.painMode);
        visual.addCommandTag(ORBIT_VISUAL_TAG);
        if (world.spawnEntity(visual)) {
            orbit.visualId = visual.getUuid();
        }
    }

    private static int randomLeapInterval(ServerWorld world) {
        int min = Math.max(1, minAttackIntervalTicks());
        int max = Math.max(min, maxAttackIntervalTicks());
        return min + world.random.nextInt(max - min + 1);
    }

    private static int getOrbitDurationTicks(BowUpgradeData upgrades) {
        int duration = orbitDurationTicks() + upgrades.stringLevel() * orbitDurationBonusPerString();
        return upgrades.runeEtching() == RuneEtching.PAIN ? duration * 2 : duration;
    }

    private static float getLeapDamage(BowUpgradeData upgrades) {
        return (float) (attackDamage() * upgrades.damageMultiplier());
    }

    private static Vec3d getOrbitCenter(LivingEntity target) {
        return target.getPos().add(0.0, target.getStandingEyeHeight() * 0.62, 0.0);
    }

    private static Vec3d getEntityCenter(LivingEntity target) {
        return target.getPos().add(0.0, target.getStandingEyeHeight() * 0.55, 0.0);
    }

    public static boolean isPainOrbitReady(ServerWorld world, UUID ownerId) {
        if (world == null || ownerId == null) {
            return false;
        }
        long now = CooldownStorage.currentTick(world);
        Long cooldownEnd = getPainCooldowns(world).get(ownerId);
        return cooldownEnd == null || cooldownEnd <= now;
    }

    private static void startPainCooldown(ServerWorld world, UUID ownerId) {
        getPainCooldowns(world).put(ownerId, CooldownStorage.currentTick(world) + Math.max(20, painCooldownTicks()));
    }

    private static Map<UUID, Long> getPainCooldowns(ServerWorld world) {
        return CooldownStorage.forWorld(PAIN_COOLDOWNS_BY_SERVER, world);
    }

    private static LivingEntity getLivingEntity(ServerWorld world, UUID id) {
        Entity entity = world.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void discardVisual(ServerWorld world, UUID visualId) {
        Entity visual = world.getEntity(visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    private static UUID spawnTetherVisual(ServerWorld world, Vec3d start, Vec3d end) {
        CosmicTetherVisualEntity visual = new CosmicTetherVisualEntity(world, start, end);
        if (world.spawnEntity(visual)) {
            return visual.getUuid();
        }
        return null;
    }

    private static void updateTetherVisual(ServerWorld world, UUID visualId, Vec3d start, Vec3d end) {
        if (visualId == null) {
            return;
        }
        Entity entity = world.getEntity(visualId);
        if (entity instanceof CosmicTetherVisualEntity visual) {
            visual.setPos(start.x, start.y, start.z);
            visual.setEndPos(end);
        }
    }

    private static void discardTetherVisuals(ServerWorld world, ActiveOrbit orbit) {
        for (UUID visualId : orbit.tetherVisualIds.values()) {
            discardVisual(world, visualId);
        }
        orbit.tetherVisualIds.clear();
    }

    public static void sharePainTetherDamage(ServerWorld world, LivingEntity damaged, DamageSource source, float amount) {
        if (world == null || damaged == null || amount <= 0.0F || SHARING_DAMAGE.get()) {
            return;
        }
        List<ActiveOrbit> orbits = ACTIVE_ORBITS.get(world);
        if (orbits == null || orbits.isEmpty()) {
            return;
        }

        UUID damagedId = damaged.getUuid();
        for (ActiveOrbit orbit : orbits) {
            if (!orbit.painMode || (!orbit.targetId.equals(damagedId) && !orbit.tetherVisualIds.containsKey(damagedId))) {
                continue;
            }
            SHARING_DAMAGE.set(true);
            try {
                if (!orbit.targetId.equals(damagedId)) {
                    LivingEntity anchor = getLivingEntity(world, orbit.targetId);
                    applySharedPainDamage(world, anchor, source, amount);
                }
                for (UUID targetId : orbit.tetherVisualIds.keySet()) {
                    if (targetId.equals(damagedId)) {
                        continue;
                    }
                    LivingEntity target = getLivingEntity(world, targetId);
                    applySharedPainDamage(world, target, source, amount);
                }
            } finally {
                SHARING_DAMAGE.set(false);
            }
            return;
        }
    }

    private static void applySharedPainDamage(ServerWorld world, LivingEntity target, DamageSource source, float amount) {
        if (target == null || !target.isAlive()) {
            return;
        }
        target.hurtTime = 0;
        target.timeUntilRegen = 0;
        target.damage(source == null ? world.getDamageSources().magic() : source, amount);
        target.hurtTime = 0;
        target.timeUntilRegen = 0;
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof CosmicOrbitVisualEntity && entity.getCommandTags().contains(ORBIT_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static class ActiveOrbit {
        private UUID targetId;
        private UUID ownerId;
        private UUID visualId;
        private BowUpgradeData upgrades;
        private boolean painMode;
        private long spawnTick;
        private long expiryTick;
        private long nextLeapTick;
        private int jumpsUsed;
        private final List<UUID> visitedTargets = new ArrayList<>();
        private final Map<UUID, UUID> tetherVisualIds = new HashMap<>();

        private ActiveOrbit(UUID targetId, UUID ownerId, UUID visualId, BowUpgradeData upgrades, boolean painMode, long spawnTick, long expiryTick, long nextLeapTick) {
            this.targetId = targetId;
            this.ownerId = ownerId;
            this.visualId = visualId;
            this.upgrades = upgrades == null ? BowUpgradeData.none() : upgrades;
            this.painMode = painMode;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
            this.nextLeapTick = nextLeapTick;
        }
    }

    private static final ThreadLocal<Boolean> SHARING_DAMAGE = ThreadLocal.withInitial(() -> false);
}
