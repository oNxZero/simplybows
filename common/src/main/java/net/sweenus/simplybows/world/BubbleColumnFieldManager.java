package net.sweenus.simplybows.world;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.entity.BubbleBountyVisualEntity;
import net.sweenus.simplybows.entity.BubbleGraceVisualEntity;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BubbleColumnFieldManager {

    private static int columnDurationTicks() { return SimplyBowsConfig.INSTANCE.bubbleveil.columnDurationTicks.get(); }
    private static int columnDurationBonusPerString() { return SimplyBowsConfig.INSTANCE.bubbleveil.columnDurationBonusPerString.get(); }
    private static double columnBaseRadius() { return SimplyBowsConfig.INSTANCE.bubbleveil.columnBaseRadius.get(); }
    private static double columnBaseHeight() { return SimplyBowsConfig.INSTANCE.bubbleveil.columnBaseHeight.get(); }
    private static double columnRadiusPerFrame() { return SimplyBowsConfig.INSTANCE.bubbleveil.columnRadiusPerFrame.get(); }
    private static double columnHeightPerFrame() { return SimplyBowsConfig.INSTANCE.bubbleveil.columnHeightPerFrame.get(); }
    private static int bountyDamageIntervalTicks() { return SimplyBowsConfig.INSTANCE.bubbleveil.bountyDamageIntervalTicks.get(); }
    private static float bountyBaseDamage() { return SimplyBowsConfig.INSTANCE.bubbleveil.bountyBaseDamage.get(); }
    private static int gracePulseIntervalTicks() { return SimplyBowsConfig.INSTANCE.bubbleveil.gracePulseIntervalTicks.get(); }
    private static int graceResistanceDurationTicks() { return SimplyBowsConfig.INSTANCE.bubbleveil.graceResistanceDuration.get(); }
    private static int graceSlownessDurationTicks() { return SimplyBowsConfig.INSTANCE.bubbleveil.graceSlownessDuration.get(); }
    private static final int GRACE_RESISTANCE_AMPLIFIER = 0;
    private static final int GRACE_SLOWNESS_AMPLIFIER = 0;
    private static final String BUBBLE_BOUNTY_VISUAL_TAG = "simplybows_bubble_bounty_visual";
    private static final String BUBBLE_GRACE_VISUAL_TAG = "simplybows_bubble_grace_visual";
    private static final int GROUND_SCAN_UP = 4;
    private static final int GROUND_SCAN_DOWN = 12;
    private static final Map<ServerWorld, ActiveBubbleColumn> ACTIVE_COLUMNS = new HashMap<>();

    private BubbleColumnFieldManager() {
    }

    public static boolean hasActive(ServerWorld world) {
        return ACTIVE_COLUMNS.containsKey(world) || (world.getTime() % 20L == 0L);
    }

    public static boolean createOrReplaceColumn(ServerWorld world, Vec3d center) {
        return createOrReplaceColumn(world, center, null, BowUpgradeData.none());
    }

    public static boolean createOrReplaceColumn(ServerWorld world, Vec3d center, UUID ownerId, BowUpgradeData upgrades) {
        if (!isUnderwater(world, center)) {
            return false;
        }

        if (upgrades == null) {
            upgrades = BowUpgradeData.none();
        }
        ColumnTuning tuning = buildTuning(upgrades);

        double y = findGroundTopY(world, center.x, center.z, center.y) + 0.05;
        Vec3d anchoredCenter = new Vec3d(center.x, y, center.z);
        if (!isUnderwater(world, anchoredCenter)) {
            return false;
        }

        ActiveBubbleColumn previous = ACTIVE_COLUMNS.remove(world);
        if (previous != null) {
            discardVisual(world, previous.visualId);
        }

        long expiryTick = world.getTime() + tuning.durationTicks();
        UUID visualId = null;
        if (tuning.bountyMode()) {
            BubbleBountyVisualEntity visual = new BubbleBountyVisualEntity(world, anchoredCenter.x, anchoredCenter.y, anchoredCenter.z);
            visual.setHeightScale(1.0F);
            visual.setRadius((float) tuning.radius());
            visual.setColumnHeight((float) tuning.height());
            visual.addCommandTag(BUBBLE_BOUNTY_VISUAL_TAG);
            if (world.spawnEntity(visual)) {
                visualId = visual.getUuid();
            }
        } else if (tuning.graceMode()) {
            BubbleGraceVisualEntity visual = new BubbleGraceVisualEntity(world, anchoredCenter.x, anchoredCenter.y, anchoredCenter.z);
            visual.setHeightScale(1.0F);
            visual.setRadius((float) tuning.radius());
            visual.setColumnHeight((float) tuning.height());
            visual.addCommandTag(BUBBLE_GRACE_VISUAL_TAG);
            if (world.spawnEntity(visual)) {
                visualId = visual.getUuid();
            }
        }

        long firstEffectTick = world.getTime() + (tuning.bountyMode() ? bountyDamageIntervalTicks() : gracePulseIntervalTicks());
        ACTIVE_COLUMNS.put(world, new ActiveBubbleColumn(anchoredCenter, expiryTick, ownerId, tuning, firstEffectTick, visualId));
        spawnBurstParticles(world, anchoredCenter, tuning);
        playSpawnSound(world, anchoredCenter);
        return true;
    }

    public static void tick(ServerWorld world) {
        ActiveBubbleColumn column = ACTIVE_COLUMNS.get(world);
        if (column == null) {
            if (world.getTime() % 20L == 0L) {
                purgeOrphanVisuals(world);
            }
            return;
        }

        if (world.getTime() >= column.expiryTick) {
            discardVisual(world, column.visualId);
            ACTIVE_COLUMNS.remove(world);
            return;
        }

        spawnAmbientParticles(world, column.center, column.tuning);
        updateVisual(world, column);

        if (world.getTime() % 5L == 0L) {
            refillPlayerAir(world, column.center, column.tuning);
        }
        if (column.tuning.bountyMode() && world.getTime() >= column.nextDamageTick) {
            applyBountySwarmDamage(world, column);
            column.nextDamageTick = world.getTime() + bountyDamageIntervalTicks();
        } else if (column.tuning.graceMode()) {
            blockHostileProjectiles(world, column);
            if (world.getTime() >= column.nextDamageTick) {
                applyGraceAuras(world, column);
                column.nextDamageTick = world.getTime() + gracePulseIntervalTicks();
            }
        }
    }

    private static void refillPlayerAir(ServerWorld world, Vec3d center, ColumnTuning tuning) {
        double radius = tuning.radius();
        double height = tuning.height();
        Box box = Box.of(center.add(0.0, height * 0.5, 0.0), radius * 2.0, height, radius * 2.0);
        for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, box, PlayerEntity::isAlive)) {
            if (player.squaredDistanceTo(center.x, player.getY(), center.z) > radius * radius) {
                continue;
            }
            if (player.getAir() < player.getMaxAir()) {
                player.setAir(player.getMaxAir());
            }
        }
    }

    private static void spawnAmbientParticles(ServerWorld world, Vec3d center, ColumnTuning tuning) {
        double radius = tuning.radius();
        double height = tuning.height();
        for (int i = 0; i < 3; i++) {
            double y = center.y + 0.2 + world.getRandom().nextDouble() * (height - 0.25);
            world.spawnParticles(ParticleTypes.BUBBLE_COLUMN_UP, center.x, y, center.z, 1, 0.15 + radius * 0.08, 0.05, 0.15 + radius * 0.08, 0.0);
        }
        world.spawnParticles(ParticleTypes.BUBBLE, center.x, center.y + 0.15, center.z, 2, 0.15 + radius * 0.1, 0.04, 0.15 + radius * 0.1, 0.0);
        if (tuning.bountyMode() && world.getTime() % 4L == 0L) {
            world.spawnParticles(ParticleTypes.BUBBLE_POP, center.x, center.y + 0.25 + world.random.nextDouble() * (height * 0.6), center.z, 1, radius * 0.25, 0.12, radius * 0.25, 0.0);
        } else if (tuning.graceMode() && world.getTime() % 3L == 0L) {
            world.spawnParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.3 + world.random.nextDouble() * (height * 0.65), center.z, 1, radius * 0.28, 0.16, radius * 0.28, 0.0);
            world.spawnParticles(ParticleTypes.BUBBLE_POP, center.x, center.y + 0.2 + world.random.nextDouble() * (height * 0.6), center.z, 1, radius * 0.22, 0.12, radius * 0.22, 0.0);
        }
    }

    private static void spawnBurstParticles(ServerWorld world, Vec3d center, ColumnTuning tuning) {
        double radius = tuning.radius();
        world.spawnParticles(ParticleTypes.BUBBLE_COLUMN_UP, center.x, center.y + 0.3, center.z, 16, radius * 0.35, 0.25, radius * 0.35, 0.0);
        world.spawnParticles(ParticleTypes.SPLASH, center.x, center.y + 0.15, center.z, 10, radius * 0.3, 0.1, radius * 0.3, 0.0);
    }

    private static void playSpawnSound(ServerWorld world, Vec3d center) {
        world.playSound(
                null,
                center.x,
                center.y,
                center.z,
                SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT,
                SoundCategory.BLOCKS,
                0.7F,
                0.95F + world.getRandom().nextFloat() * 0.15F
        );
    }

    private static boolean isUnderwater(ServerWorld world, Vec3d center) {
        BlockPos base = BlockPos.ofFloored(center);
        if (!world.getFluidState(base).isOf(Fluids.WATER)) {
            return false;
        }
        BlockPos up = base.up();
        return world.getFluidState(up).isOf(Fluids.WATER);
    }

    private static double findGroundTopY(ServerWorld world, double x, double z, double centerY) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startY = (int) Math.floor(centerY) + GROUND_SCAN_UP;
        int minY = Math.max(world.getBottomY(), (int) Math.floor(centerY) - GROUND_SCAN_DOWN);

        for (int y = startY; y >= minY; y--) {
            BlockPos pos = new BlockPos(blockX, y, blockZ);
            if (world.getBlockState(pos).isSideSolidFullSquare(world, pos, Direction.UP)) {
                return y + 1.0;
            }
        }

        return centerY;
    }

    private static void applyBountySwarmDamage(ServerWorld world, ActiveBubbleColumn column) {
        LivingEntity owner = getLivingEntity(world, column.ownerId);
        double radius = column.tuning.radius();
        double height = column.tuning.height();
        Box box = Box.of(column.center.add(0.0, height * 0.5, 0.0), radius * 2.0, height, radius * 2.0);
        for (LivingEntity candidate : world.getEntitiesByClass(
                LivingEntity.class,
                box,
                CombatTargeting::isOffensiveTargetCandidate
        )) {
            if (owner != null && !CombatTargeting.checkFriendlyFire(candidate, owner)) {
                continue;
            }
            if (candidate.squaredDistanceTo(column.center.x, candidate.getY(), column.center.z) > radius * radius) {
                continue;
            }
            CombatTargeting.applyDamage(world, owner, candidate, column.tuning.bountyDotDamage(), true, false);
        }
    }

    private static void applyGraceAuras(ServerWorld world, ActiveBubbleColumn column) {
        LivingEntity owner = getLivingEntity(world, column.ownerId);
        if (owner == null || !owner.isAlive()) {
            return;
        }

        double radius = column.tuning.radius();
        double height = column.tuning.height();
        Box box = Box.of(column.center.add(0.0, height * 0.5, 0.0), radius * 2.0, height, radius * 2.0);

        for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (candidate.squaredDistanceTo(column.center.x, candidate.getY(), column.center.z) > radius * radius) {
                continue;
            }
            if (CombatTargeting.isFriendlyTo(candidate, owner)) {
                candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, graceResistanceDurationTicks(), GRACE_RESISTANCE_AMPLIFIER), owner);
            } else if (CombatTargeting.isOffensiveTargetCandidate(candidate)) {
                candidate.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, graceSlownessDurationTicks(), GRACE_SLOWNESS_AMPLIFIER), owner);
            }
        }
    }

    private static void blockHostileProjectiles(ServerWorld world, ActiveBubbleColumn column) {
        LivingEntity owner = getLivingEntity(world, column.ownerId);
        if (owner == null || !owner.isAlive()) {
            return;
        }

        double radius = column.tuning.radius();
        double height = column.tuning.height();
        Box box = Box.of(column.center.add(0.0, height * 0.5, 0.0), radius * 2.0, height, radius * 2.0);
        for (ProjectileEntity projectile : world.getEntitiesByClass(ProjectileEntity.class, box, p -> p.isAlive() && !p.isRemoved())) {
            if (projectile.squaredDistanceTo(column.center.x, projectile.getY(), column.center.z) > radius * radius) {
                continue;
            }
            if (!isHostileProjectile(owner, projectile)) {
                continue;
            }
            Vec3d pos = projectile.getPos();
            world.spawnParticles(ParticleTypes.BUBBLE_POP, pos.x, pos.y, pos.z, 7, 0.12, 0.08, 0.12, 0.01);
            world.spawnParticles(ParticleTypes.ENCHANT, pos.x, pos.y, pos.z, 5, 0.12, 0.08, 0.12, 0.0);
            world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.6F, 1.35F + world.random.nextFloat() * 0.15F);
            projectile.discard();
        }
    }

    private static boolean isHostileProjectile(LivingEntity owner, ProjectileEntity projectile) {
        Entity projectileOwner = projectile.getOwner();
        if (projectileOwner == owner) {
            return false;
        }
        if (projectileOwner instanceof LivingEntity livingOwner) {
            return CombatTargeting.checkFriendlyFire(owner, livingOwner);
        }
        return projectileOwner == null || projectileOwner instanceof HostileEntity;
    }

    private static void updateVisual(ServerWorld world, ActiveBubbleColumn column) {
        if (column.visualId == null) {
            return;
        }
        net.minecraft.entity.Entity entity = world.getEntity(column.visualId);
        if (entity instanceof BubbleBountyVisualEntity visual) {
            visual.setPos(column.center.x, column.center.y, column.center.z);
            visual.setRadius((float) column.tuning.radius());
            visual.setColumnHeight((float) column.tuning.height());
            long ticksRemaining = column.expiryTick - world.getTime();
            float fade = Math.max(0.0F, Math.min(1.0F, ticksRemaining / 15.0F));
            visual.setHeightScale(fade);
            return;
        }
        if (entity instanceof BubbleGraceVisualEntity visual) {
            visual.setPos(column.center.x, column.center.y, column.center.z);
            visual.setRadius((float) column.tuning.radius());
            visual.setColumnHeight((float) column.tuning.height());
            long ticksRemaining = column.expiryTick - world.getTime();
            float fade = Math.max(0.0F, Math.min(1.0F, ticksRemaining / 15.0F));
            visual.setHeightScale(fade);
        }
    }

    private static LivingEntity getLivingEntity(ServerWorld world, UUID id) {
        if (id == null) {
            return null;
        }
        net.minecraft.entity.Entity entity = world.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void discardVisual(ServerWorld world, UUID visualId) {
        if (visualId == null) {
            return;
        }
        net.minecraft.entity.Entity entity = world.getEntity(visualId);
        if (entity != null) {
            entity.discard();
        }
    }

    private static void purgeOrphanVisuals(ServerWorld world) {
        for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
            if (entity instanceof BubbleBountyVisualEntity && entity.getCommandTags().contains(BUBBLE_BOUNTY_VISUAL_TAG)) {
                entity.discard();
            }
            if (entity instanceof BubbleGraceVisualEntity && entity.getCommandTags().contains(BUBBLE_GRACE_VISUAL_TAG)) {
                entity.discard();
            }
        }
    }

    private static ColumnTuning buildTuning(BowUpgradeData upgrades) {
        double radius = (columnBaseRadius() * upgrades.sizeMultiplier()) + upgrades.frameLevel() * columnRadiusPerFrame();
        double height = (columnBaseHeight() * upgrades.sizeMultiplier()) + upgrades.frameLevel() * columnHeightPerFrame();
        int durationTicks = columnDurationTicks() + upgrades.stringLevel() * columnDurationBonusPerString();
        boolean bountyMode = upgrades.runeEtching() == RuneEtching.BOUNTY;
        boolean graceMode = upgrades.runeEtching() == RuneEtching.GRACE;
        float bountyDotDamage = (float) (bountyBaseDamage() * upgrades.damageMultiplier());
        return new ColumnTuning(radius, height, durationTicks, bountyMode, graceMode, bountyDotDamage);
    }

    private record ColumnTuning(double radius, double height, int durationTicks, boolean bountyMode, boolean graceMode, float bountyDotDamage) {
    }

    private static final class ActiveBubbleColumn {
        private final Vec3d center;
        private final long expiryTick;
        private final UUID ownerId;
        private final ColumnTuning tuning;
        private long nextDamageTick;
        private final UUID visualId;

        private ActiveBubbleColumn(Vec3d center, long expiryTick, UUID ownerId, ColumnTuning tuning, long nextDamageTick, UUID visualId) {
            this.center = center;
            this.expiryTick = expiryTick;
            this.ownerId = ownerId;
            this.tuning = tuning;
            this.nextDamageTick = nextDamageTick;
            this.visualId = visualId;
        }
    }
}
