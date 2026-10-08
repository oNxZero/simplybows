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
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.BeeArrowEntity;
import net.sweenus.simplybows.entity.BeeHiveVisualEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bounty: hive releases exploding bees. String = bee count, Frame = damage + poison level (up to III).
 */
public final class BeeHiveSwarmManager {

    private static double targetRadius() { return SimplyBowsConfig.INSTANCE.buzzkill.bountyTargetRadius.get(); }
    private static double baseBeeDamage() { return SimplyBowsConfig.INSTANCE.buzzkill.baseDamage.get(); }

    private static final int BEE_INTERVAL_TICKS = 16;
    private static final double START_OFFSET_Y = 0.85;
    private static final double SHOT_SPEED = 0.72;
    private static final String HIVE_VISUAL_TAG = "simplybows_bee_hive_visual";

    private static final Map<ServerWorld, List<ActiveHive>> ACTIVE_HIVES = new HashMap<>();

    private BeeHiveSwarmManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        List<ActiveHive> hives = ACTIVE_HIVES.get(world);
        return (hives != null && !hives.isEmpty()) || (world.getTime() % 20L == 0L);
    }

    public static int hiveDurationTicks(BowUpgradeData upgrades) {
        return beeCountFor(upgrades) * BEE_INTERVAL_TICKS;
    }

    public static int beeCountFor(BowUpgradeData upgrades) {
        // String 0 = 2 bees, String 5 = 7 bees (more bees, not poison scaling).
        return Math.max(2, 2 + Math.max(0, upgrades.stringLevel()));
    }

    public static void createHive(ServerWorld world, Vec3d center, LivingEntity owner, BowUpgradeData upgrades) {
        if (world == null || center == null || owner == null) {
            return;
        }

        // Keep string/frame so hive bees can scale poison from Frame.
        BowUpgradeData swarmUpgrades = new BowUpgradeData(
                upgrades.stringLevel(),
                upgrades.frameLevel(),
                RuneEtching.BOUNTY
        );

        double groundY = findGroundTopY(world, center.x, center.z, center.y);
        Vec3d hiveCenter = new Vec3d(center.x, groundY, center.z);

        int beeCount = beeCountFor(swarmUpgrades);
        // Frame = damage. Soft curve so Frame 5 isn't nuclear.
        float beeDamage = (float) (baseBeeDamage() * (1.35 + swarmUpgrades.frameLevel() * 0.35));
        long now = world.getTime();
        long expiry = now + (long) beeCount * BEE_INTERVAL_TICKS;

        BeeHiveVisualEntity visual = new BeeHiveVisualEntity(world, hiveCenter.x, hiveCenter.y, hiveCenter.z);
        visual.setHeightScale(1.0F);
        visual.addCommandTag(HIVE_VISUAL_TAG);
        UUID visualId = null;
        if (world.spawnEntity(visual)) {
            visualId = visual.getUuid();
        }

        ACTIVE_HIVES.computeIfAbsent(world, w -> new ArrayList<>()).add(new ActiveHive(
                hiveCenter,
                owner.getUuid(),
                swarmUpgrades,
                beeCount,
                beeDamage,
                visualId,
                now,
                expiry,
                now // first bee immediately
        ));

        world.playSound(null, hiveCenter.x, hiveCenter.y, hiveCenter.z, SoundEvents.BLOCK_BEEHIVE_ENTER, SoundCategory.PLAYERS, 0.9F, 0.95F);
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, net.minecraft.block.Blocks.HONEYCOMB_BLOCK.getDefaultState()),
                hiveCenter.x, hiveCenter.y + 0.2, hiveCenter.z, 10, 0.35, 0.18, 0.35, 0.01);
    }

    public static void tick(ServerWorld world) {
        List<ActiveHive> hives = ACTIVE_HIVES.get(world);
        if (hives == null || hives.isEmpty()) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanHiveVisuals(world);
            }
            return;
        }

        Iterator<ActiveHive> it = hives.iterator();
        while (it.hasNext()) {
            ActiveHive hive = it.next();
            Entity visual = hive.visualId == null ? null : world.getEntity(hive.visualId);
            if (visual instanceof BeeHiveVisualEntity beeVisual) {
                beeVisual.setHeightScale(1.0F);
                beeVisual.setPos(hive.center.x, hive.center.y, hive.center.z);
            }

            if (hive.beesRemaining > 0 && world.getTime() >= hive.nextBeeTick) {
                LivingEntity owner = world.getEntity(hive.ownerId) instanceof LivingEntity living ? living : null;
                if (owner != null && owner.isAlive()) {
                    List<LivingEntity> hostiles = findHostiles(world, hive.center, owner);
                    LivingEntity target = hostiles.isEmpty() ? null : hostiles.get(world.random.nextInt(hostiles.size()));
                    int index = hive.beeCount - hive.beesRemaining;
                    spawnBee(world, owner, hive.upgrades, hive.center, target, index, hive.beeCount, hive.beeDamage);
                    world.playSound(null, hive.center.x, hive.center.y, hive.center.z, SoundEvents.BLOCK_BEEHIVE_EXIT, SoundCategory.PLAYERS, 0.75F, 1.0F + world.random.nextFloat() * 0.1F);
                }
                hive.beesRemaining--;
                hive.nextBeeTick = world.getTime() + BEE_INTERVAL_TICKS;
            }

            if (world.getTime() % 4L == 0L) {
                world.spawnParticles(ParticleTypes.FALLING_HONEY, hive.center.x, hive.center.y + 0.35, hive.center.z, 2, 0.2, 0.1, 0.2, 0.0);
                world.spawnParticles(ParticleTypes.POOF, hive.center.x, hive.center.y + 0.2, hive.center.z, 1, 0.15, 0.05, 0.15, 0.0);
            }

            if (hive.beesRemaining <= 0 && world.getTime() >= hive.expiryTick) {
                if (visual != null) {
                    visual.discard();
                }
                it.remove();
            }
        }

        if (hives.isEmpty()) {
            ACTIVE_HIVES.remove(world);
            purgeOrphanHiveVisuals(world);
        }
    }

    private static void spawnBee(ServerWorld world, LivingEntity owner, BowUpgradeData upgrades,
                                 Vec3d hiveCenter, LivingEntity target, int index, int total, float damage) {
        Vec3d start = hiveCenter.add(0.0, START_OFFSET_Y, 0.0);
        // Always launch outward; bees acquire/search while flying (don't pre-aim across the map).
        double angle = (Math.PI * 2.0 / Math.max(1, total)) * index + world.random.nextDouble() * 0.35;
        Vec3d direction = new Vec3d(Math.cos(angle), 0.2 + world.random.nextDouble() * 0.15, Math.sin(angle));

        BeeArrowEntity.setPainHoming(true, 0.55F);
        try {
            BeeArrowEntity bee = new BeeArrowEntity(world, owner, new ItemStack(Items.ARROW), upgrades);
            bee.setPosition(start.x, start.y, start.z);
            bee.setNoGravity(true);
            bee.setVelocity(direction.normalize().multiply(SHOT_SPEED));
            bee.setCritical(false);
            bee.setDamage(damage);
            bee.setHiveBee(true);
            if (target != null) {
                bee.lockHiveTarget(target);
            }
            world.spawnEntity(bee);
        } finally {
            BeeArrowEntity.setPainHoming(false);
        }
        world.spawnParticles(ParticleTypes.POOF, start.x, start.y, start.z, 2, 0.05, 0.04, 0.05, 0.01);
    }

    private static List<LivingEntity> findHostiles(ServerWorld world, Vec3d center, LivingEntity owner) {
        double radius = targetRadius(); // already halfed in config (7)
        Box box = Box.of(center, radius * 2.0, 8.0, radius * 2.0);
        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, box, entity ->
                CombatTargeting.isOffensiveTargetCandidate(entity)
                        && CombatTargeting.checkFriendlyFire(entity, owner));
        candidates.sort((a, b) -> Double.compare(a.squaredDistanceTo(center), b.squaredDistanceTo(center)));
        return candidates;
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

    private static final class ActiveHive {
        private final Vec3d center;
        private final UUID ownerId;
        private final BowUpgradeData upgrades;
        private final int beeCount;
        private final float beeDamage;
        private final UUID visualId;
        private final long expiryTick;
        private int beesRemaining;
        private long nextBeeTick;

        private ActiveHive(Vec3d center, UUID ownerId, BowUpgradeData upgrades, int beeCount, float beeDamage,
                           UUID visualId, long spawnTick, long expiryTick, long nextBeeTick) {
            this.center = center;
            this.ownerId = ownerId;
            this.upgrades = upgrades;
            this.beeCount = beeCount;
            this.beeDamage = beeDamage;
            this.visualId = visualId;
            this.expiryTick = expiryTick;
            this.beesRemaining = beeCount;
            this.nextBeeTick = nextBeeTick;
        }
    }
}
