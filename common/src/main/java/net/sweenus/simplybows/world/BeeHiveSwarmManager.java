package net.sweenus.simplybows.world;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.entity.BeeArrowEntity;
import net.sweenus.simplybows.entity.BeeHiveVisualEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BeeHiveSwarmManager {

    private static int baseHiveDurationTicks() { return SimplyBowsConfig.INSTANCE.buzzkill.bountyHiveDuration.get(); }
    private static int stringHiveDurationBonusTicks() { return SimplyBowsConfig.INSTANCE.buzzkill.bountyHiveDurationBonusPerString.get(); }
    private static int fireIntervalTicks() { return SimplyBowsConfig.INSTANCE.buzzkill.bountyFireInterval.get(); }
    private static int baseShots() { return SimplyBowsConfig.INSTANCE.buzzkill.bountyBaseShots.get(); }
    private static int frameBonusShots() { return SimplyBowsConfig.INSTANCE.buzzkill.bountyFrameBonusShots.get(); }
    private static double targetRadius() { return SimplyBowsConfig.INSTANCE.buzzkill.bountyTargetRadius.get(); }

    private static final int SPRING_ANIM_TICKS = 8;
    private static final int HIVE_RETRACT_TICKS = 10;
    private static final int FIRE_INTERVAL_RANDOM_EXTRA_TICKS = 6;
    private static final double START_OFFSET_Y = 0.45;
    private static final double TARGET_AIM_EXTRA_Y = 3.65;
    private static final double SHOT_SPEED = 0.55;
    private static final double SHOT_DIVERGENCE = 0.08;
    private static final int RANDOM_TARGET_POOL_SIZE = 4;
    private static final String HIVE_VISUAL_TAG = "simplybows_bee_hive_visual";

    private static final Map<ServerWorld, List<ActiveBeeHive>> ACTIVE_HIVES = new HashMap<>();

    private BeeHiveSwarmManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveBeeHive> hives = ACTIVE_HIVES.get(world);
        return (hives != null && !hives.isEmpty()) || (world.getTime() % 20L == 0L);
    }

    public static void createHive(ServerWorld world, Vec3d center, LivingEntity owner, BowUpgradeData upgrades) {
        if (world == null || center == null || owner == null) {
            return;
        }

        BowUpgradeData swarmUpgrades = new BowUpgradeData(
                upgrades.stringLevel(),
                upgrades.frameLevel(),
                RuneEtching.NONE
        );

        double groundY = findGroundTopY(world, center.x, center.z, center.y);
        Vec3d hiveCenter = new Vec3d(center.x, groundY, center.z);

        BeeHiveVisualEntity visual = new BeeHiveVisualEntity(world, hiveCenter.x, hiveCenter.y, hiveCenter.z);
        visual.setHeightScale(0.0F);
        visual.addCommandTag(HIVE_VISUAL_TAG);
        if (!world.spawnEntity(visual)) {
            return;
        }

        long now = world.getTime();
        int shots = baseShots() + swarmUpgrades.frameLevel() * frameBonusShots();
        int hiveDurationTicks = baseHiveDurationTicks() + swarmUpgrades.stringLevel() * stringHiveDurationBonusTicks();
        ActiveBeeHive hive = new ActiveBeeHive(
                hiveCenter,
                owner.getUuid(),
                visual.getUuid(),
                now,
                now + hiveDurationTicks,
                now + 3,
                Math.max(2, shots),
                swarmUpgrades
        );
        ACTIVE_HIVES.computeIfAbsent(world, w -> new ArrayList<>()).add(hive);

        world.playSound(null, hiveCenter.x, hiveCenter.y, hiveCenter.z, SoundEvents.BLOCK_BEEHIVE_ENTER, SoundCategory.PLAYERS, 0.8F, 0.95F + world.random.nextFloat() * 0.12F);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.HONEYCOMB_BLOCK.getDefaultState()), hiveCenter.x, hiveCenter.y + 0.2, hiveCenter.z, 10, 0.35, 0.18, 0.35, 0.01);
        world.spawnParticles(ParticleTypes.WAX_ON, hiveCenter.x, hiveCenter.y + 0.2, hiveCenter.z, 8, 0.3, 0.14, 0.3, 0.01);
    }

    public static void tick(ServerWorld world) {
        List<ActiveBeeHive> hives = ACTIVE_HIVES.get(world);
        if (hives == null || hives.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanHiveVisuals(world);
            }
            return;
        }

        hives.removeIf(hive -> {
            if (hive.retractStartTick > 0L) {
                return tickHiveRetract(world, hive);
            }
            if (world.getTime() > hive.expiryTick || hive.shotsRemaining <= 0) {
                hive.retractStartTick = world.getTime();
                return false;
            }
            return false;
        });

        if (hives.isEmpty()) {
            ACTIVE_HIVES.remove(world);
            purgeOrphanHiveVisuals(world);
            return;
        }

        for (ActiveBeeHive hive : hives) {
            if (hive.retractStartTick > 0L) {
                continue;
            }
            animateHive(world, hive);
            spawnHiveAmbient(world, hive.center);
            tryFireBee(world, hive);
        }
    }

    private static boolean tickHiveRetract(ServerWorld world, ActiveBeeHive hive) {
        Entity entity = world.getEntity(hive.visualId);
        long age = world.getTime() - hive.retractStartTick;
        if (!(entity instanceof BeeHiveVisualEntity visual) || age >= HIVE_RETRACT_TICKS) {
            discardVisual(world, hive.visualId);
            return true;
        }
        float ease = (float) age / (float) HIVE_RETRACT_TICKS;
        ease = ease * ease;
        visual.setHeightScale(MathHelper.clamp(1.0F - ease, 0.0F, 1.0F));
        visual.setPos(hive.center.x, hive.center.y - 0.55 * ease, hive.center.z);
        return false;
    }

    private static void animateHive(ServerWorld world, ActiveBeeHive hive) {
        Entity entity = world.getEntity(hive.visualId);
        if (!(entity instanceof BeeHiveVisualEntity visual)) {
            return;
        }

        long age = world.getTime() - hive.spawnTick;
        float scaleIn = MathHelper.clamp((float) age / SPRING_ANIM_TICKS, 0.0F, 1.0F);
        visual.setHeightScale(scaleIn);
        visual.setPos(hive.center.x, hive.center.y, hive.center.z);
    }

    private static void spawnHiveAmbient(ServerWorld world, Vec3d center) {
        if (world.getTime() % 4L == 0L) {
            world.spawnParticles(ParticleTypes.FALLING_HONEY, center.x, center.y + 0.3, center.z, 1, 0.2, 0.1, 0.2, 0.0);
        }
        if (world.getTime() % 8L == 0L) {
            world.spawnParticles(ParticleTypes.WAX_ON, center.x, center.y + 0.45, center.z, 1, 0.16, 0.1, 0.16, 0.0);
        }
    }

    private static void tryFireBee(ServerWorld world, ActiveBeeHive hive) {
        if (world.getTime() < hive.nextShotTick || hive.shotsRemaining <= 0) {
            return;
        }

        LivingEntity owner = getOwnerEntity(world, hive.ownerId);
        if (owner == null || !owner.isAlive()) {
            return;
        }

        LivingEntity target = findRandomNearbyHostile(world, hive.center, owner);
        hive.nextShotTick = world.getTime() + fireIntervalTicks() + world.random.nextInt(FIRE_INTERVAL_RANDOM_EXTRA_TICKS + 1);
        if (target == null) {
            return;
        }

        Vec3d start = hive.center.add(0.0, START_OFFSET_Y, 0.0);
        Vec3d targetPos = target.getPos().add(0.0, target.getStandingEyeHeight() + TARGET_AIM_EXTRA_Y, 0.0);
        Vec3d direction = targetPos.subtract(start);
        if (direction.lengthSquared() < 1.0E-6) {
            return;
        }

        BeeArrowEntity beeArrow = new BeeArrowEntity(world, owner, new ItemStack(Items.ARROW), hive.upgrades);
        beeArrow.setPosition(start.x, start.y, start.z);
        Vec3d velocity = direction.normalize().multiply(SHOT_SPEED).add(
                (world.random.nextDouble() - 0.5) * SHOT_DIVERGENCE,
                (world.random.nextDouble() - 0.5) * SHOT_DIVERGENCE,
                (world.random.nextDouble() - 0.5) * SHOT_DIVERGENCE
        );
        beeArrow.setVelocity(velocity);
        beeArrow.setCritical(false);
        beeArrow.setDamage(2.0 * hive.upgrades.damageMultiplier() + CombatTargeting.getRangedWeaponDamageBonus(owner));

        world.spawnEntity(beeArrow);
        hive.shotsRemaining--;

        world.playSound(null, start.x, start.y, start.z, SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.PLAYERS, 0.7F, 1.0F + world.random.nextFloat() * 0.18F);
        world.spawnParticles(ParticleTypes.POOF, start.x, start.y, start.z, 2, 0.08, 0.05, 0.08, 0.01);
    }

    private static LivingEntity findRandomNearbyHostile(ServerWorld world, Vec3d center, LivingEntity owner) {
        Box box = Box.of(center, targetRadius() * 2.0, 8.0, targetRadius() * 2.0);
        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, box, entity ->
                CombatTargeting.isOffensiveTargetCandidate(entity)
                        && CombatTargeting.checkFriendlyFire(entity, owner));

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort((a, b) -> Double.compare(a.squaredDistanceTo(center), b.squaredDistanceTo(center)));
        int poolSize = Math.min(RANDOM_TARGET_POOL_SIZE, candidates.size());
        return candidates.get(world.random.nextInt(poolSize));
    }

    private static LivingEntity getOwnerEntity(ServerWorld world, UUID ownerId) {
        Entity ownerEntity = world.getEntity(ownerId);
        return ownerEntity instanceof LivingEntity living ? living : null;
    }

    private static void discardVisual(ServerWorld world, UUID visualId) {
        Entity visual = world.getEntity(visualId);
        if (visual != null) {
            visual.discard();
        }
    }

    private static void purgeOrphanHiveVisuals(ServerWorld world) {
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof BeeHiveVisualEntity && entity.getCommandTags().contains(HIVE_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static double findGroundTopY(ServerWorld world, double x, double z, double centerY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = (int) Math.floor(centerY) + 4;
        int minY = Math.max(world.getBottomY(), (int) Math.floor(centerY) - 12);

        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return y + 1.0;
            }
        }
        return centerY;
    }

    private static final class ActiveBeeHive {
        private final Vec3d center;
        private final UUID ownerId;
        private final UUID visualId;
        private final long spawnTick;
        private final long expiryTick;
        private final BowUpgradeData upgrades;
        private long nextShotTick;
        private int shotsRemaining;
        private long retractStartTick;

        private ActiveBeeHive(Vec3d center, UUID ownerId, UUID visualId, long spawnTick, long expiryTick, long nextShotTick, int shotsRemaining, BowUpgradeData upgrades) {
            this.center = center;
            this.ownerId = ownerId;
            this.visualId = visualId;
            this.spawnTick = spawnTick;
            this.expiryTick = expiryTick;
            this.nextShotTick = nextShotTick;
            this.shotsRemaining = shotsRemaining;
            this.upgrades = upgrades == null ? BowUpgradeData.none() : upgrades;
        }
    }
}
