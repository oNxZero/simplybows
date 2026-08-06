package net.sweenus.simplybows.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sweenus.simplybows.item.unique.BeeBowItem;
import net.sweenus.simplybows.item.unique.BlossomBowItem;
import net.sweenus.simplybows.item.unique.BubbleBowItem;
import net.sweenus.simplybows.item.unique.CosmicBowItem;
import net.sweenus.simplybows.item.unique.EarthBowItem;
import net.sweenus.simplybows.item.unique.EchoBowItem;
import net.sweenus.simplybows.item.unique.IceBowItem;
import net.sweenus.simplybows.item.unique.SimplyBowItem;
import net.sweenus.simplybows.item.unique.VineBowItem;
import net.sweenus.simplybows.registry.EntityRegistry;
import net.sweenus.simplybows.config.SimplyBowsConfig;
import net.sweenus.simplybows.upgrade.BowUpgradeData;
import net.sweenus.simplybows.upgrade.RuneEtching;
import net.sweenus.simplybows.util.CombatTargeting;
import net.sweenus.simplybows.world.EchoShoulderBowManager;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ShoulderBowEntity extends Entity {

    private static final TrackedData<Integer> SIDE = DataTracker.registerData(ShoulderBowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PULL_STAGE = DataTracker.registerData(ShoulderBowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> OWNER_ENTITY_ID = DataTracker.registerData(ShoulderBowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> MIRROR_OFFHAND = DataTracker.registerData(ShoulderBowEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> MIRROR_ITEM_RAW_ID = DataTracker.registerData(ShoulderBowEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final int DRAW_TICKS = 9;
    private static final int AUTO_FIRE_COOLDOWN_TICKS = 40;
    private static final int TARGET_SCAN_TICKS = 10;
    private static final double TARGET_RADIUS = 18.0;
    private static final double STRING_TARGET_RADIUS_DELTA = 2.0;
    private static final double FRAME_TARGET_RADIUS_DELTA = 2.0;
    private static final int STRING_COOLDOWN_DELTA = 8;
    private static final int FRAME_COOLDOWN_DELTA = 8;
    private static final int MIN_TARGET_RADIUS = 6;
    private static final int MAX_TARGET_RADIUS = 40;
    private static final int MIN_COOLDOWN_TICKS = 10;
    private static final int MAX_COOLDOWN_TICKS = 80;
    public static final double SHOULDER_SIDE_OFFSET = 0.68;
    public static final double SHOULDER_BACK_OFFSET = -0.16;
    public static final double SHOULDER_HEIGHT_OFFSET = -0.15;
    private static final double FOLLOW_LERP = 0.24;
    private static final double FOLLOW_TELEPORT_DISTANCE_SQ = 9.0;
    private static final float IDLE_AIM_SMOOTHING = 0.25F;
    private static final double BOB_AMPLITUDE = 0.06;
    private static final double BOB_SPEED = 0.22;
    private static final float ARROW_SPEED = 2.7F;
    private static final float ARROW_DIVERGENCE = 0.85F;
    private static final float COMPANION_ARROW_GRAVITY_PER_TICK = 0.05F;
    private UUID ownerUuid;
    private UUID trackedTargetUuid;
    private UUID forcedTargetUuid;
    private UUID queuedTargetUuid;
    private Vec3d queuedShotDirection;
    private Vec3d preparedShotDirection;
    private int drawTicksRemaining;
    private int cooldownTicks;
    private long lastFiredTick = -AUTO_FIRE_COOLDOWN_TICKS;

    public ShoulderBowEntity(EntityType<? extends ShoulderBowEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public ShoulderBowEntity(World world, LivingEntity owner, int side) {
        this(EntityRegistry.SHOULDER_BOW.get(), world);
        this.ownerUuid = owner.getUuid();
        this.setSide(side);
        this.setPullStage(0);
        this.setOwnerEntityId(owner.getId());
        this.refreshShoulderTransform(owner);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(SIDE, 1);
        this.dataTracker.startTracking(PULL_STAGE, 0);
        this.dataTracker.startTracking(OWNER_ENTITY_ID, -1);
        this.dataTracker.startTracking(MIRROR_OFFHAND, false);
        this.dataTracker.startTracking(MIRROR_ITEM_RAW_ID, Item.getRawId(Items.AIR));
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public int getSide() {
        return this.dataTracker.get(SIDE);
    }

    public void setSide(int side) {
        this.dataTracker.set(SIDE, side < 0 ? -1 : 1);
    }

    public int getPullStage() {
        return this.dataTracker.get(PULL_STAGE);
    }

    public void setPullStage(int stage) {
        this.dataTracker.set(PULL_STAGE, MathHelper.clamp(stage, 0, 3));
    }

    public int getOwnerEntityId() {
        return this.dataTracker.get(OWNER_ENTITY_ID);
    }

    public void setOwnerEntityId(int entityId) {
        this.dataTracker.set(OWNER_ENTITY_ID, entityId);
    }

    public boolean isMirroringOffhand() {
        return this.dataTracker.get(MIRROR_OFFHAND);
    }

    public int getMirroredItemRawId() {
        return this.dataTracker.get(MIRROR_ITEM_RAW_ID);
    }

    public void configureOffhandMirror(boolean enabled, ItemStack mirroredStack) {
        this.dataTracker.set(MIRROR_OFFHAND, enabled);
        Item item = mirroredStack == null || mirroredStack.isEmpty() ? Items.AIR : mirroredStack.getItem();
        this.dataTracker.set(MIRROR_ITEM_RAW_ID, Item.getRawId(item));
    }

    public void queueLookShot(Vec3d direction) {
        queueLookShot(direction, null);
    }

    public void queueLookShot(Vec3d direction, @Nullable UUID targetUuid) {
        if (direction.lengthSquared() > 1.0E-6) {
            this.queuedShotDirection = direction.normalize();
            this.queuedTargetUuid = targetUuid;
        }
    }

    public void setForcedTargetUuid(@Nullable UUID forcedTargetUuid) {
        this.forcedTargetUuid = forcedTargetUuid;
    }

    @Override
    public void tick() {
        this.noClip = true;
        this.setNoGravity(true);
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }

        LivingEntity owner = getOwnerLiving();
        if (owner == null || !owner.isAlive() || !(owner.getMainHandStack().getItem() instanceof EchoBowItem)) {
            this.discard();
            return;
        }

        this.setOwnerEntityId(owner.getId());
        refreshShoulderTransform(owner);
        spawnIdleParticles();
        BowUpgradeData mainHandUpgrades = BowUpgradeData.from(owner.getMainHandStack());
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }

        if (this.drawTicksRemaining > 0) {
            this.drawTicksRemaining--;
            this.setPullStage(stageForRemainingDrawTicks(this.drawTicksRemaining));
            updateAimDuringDraw(owner);
            if (this.drawTicksRemaining == 0) {
                firePreparedArrow(owner);
            }
            return;
        }

        this.setPullStage(0);

        if (this.queuedShotDirection != null && this.cooldownTicks <= 0) {
            UUID queuedTarget = this.queuedTargetUuid != null ? this.queuedTargetUuid : this.forcedTargetUuid;
            beginDrawing(this.queuedShotDirection, queuedTarget);
            this.queuedShotDirection = null;
            this.queuedTargetUuid = null;
            return;
        }

        double dynamicTargetRadius = getDynamicTargetRadius(mainHandUpgrades);
        LivingEntity forcedTarget = resolveForcedTarget(owner, dynamicTargetRadius);
        if (forcedTarget != null && this.cooldownTicks <= 0) {
            Vec3d targetPos = forcedTarget.getPos().add(0.0, forcedTarget.getStandingEyeHeight() * 0.6, 0.0);
            Vec3d aim = getCompensatedAimDirection(targetPos, ARROW_SPEED, COMPANION_ARROW_GRAVITY_PER_TICK);
            beginDrawing(aim, forcedTarget.getUuid());
            return;
        }

        int scanTicks = getDynamicScanTicks(mainHandUpgrades);
        if (this.cooldownTicks > 0 || this.age % scanTicks != 0) {
            return;
        }

        LivingEntity target = findNearestTarget(owner, dynamicTargetRadius);
        if (target != null) {
            Vec3d targetPos = target.getPos().add(0.0, target.getStandingEyeHeight() * 0.6, 0.0);
            Vec3d aim = getCompensatedAimDirection(targetPos, ARROW_SPEED, COMPANION_ARROW_GRAVITY_PER_TICK);
            beginDrawing(aim, target.getUuid());
        }
    }

    private LivingEntity getOwnerLiving() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld) || this.ownerUuid == null) {
            return null;
        }
        return serverWorld.getEntity(this.ownerUuid) instanceof LivingEntity living ? living : null;
    }

    private void refreshShoulderTransform(LivingEntity owner) {
        Vec3d forward = Vec3d.fromPolar(0.0F, owner.getYaw()).normalize();
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);
        double sideOffset = SHOULDER_SIDE_OFFSET * (double) this.getSide();
        double bob = Math.sin((this.age + (this.getSide() > 0 ? 6.0 : 0.0)) * BOB_SPEED) * BOB_AMPLITUDE;
        Vec3d shoulder = owner.getPos()
                .add(0.0, owner.getStandingEyeHeight() + SHOULDER_HEIGHT_OFFSET + bob, 0.0)
                .add(right.multiply(sideOffset))
                .add(forward.multiply(SHOULDER_BACK_OFFSET));
        if (this.age < 2 || this.squaredDistanceTo(shoulder.x, shoulder.y, shoulder.z) > FOLLOW_TELEPORT_DISTANCE_SQ) {
            this.setPos(shoulder.x, shoulder.y, shoulder.z);
        } else {
            Vec3d smoothed = this.getPos().lerp(shoulder, FOLLOW_LERP);
            this.setPos(smoothed.x, smoothed.y, smoothed.z);
        }

        if (this.preparedShotDirection == null || this.preparedShotDirection.lengthSquared() <= 1.0E-6 || this.trackedTargetUuid == null) {
            setLookToDirectionSmooth(owner.getRotationVec(1.0F), IDLE_AIM_SMOOTHING);
        }
    }

    private LivingEntity findNearestTarget(LivingEntity owner, double targetRadius) {
        boolean supportMode = owner instanceof ServerPlayerEntity serverOwner
                && EchoShoulderBowManager.isGraceSupportActive(serverOwner);
        LivingEntity nearest = null;
        double nearestDist = targetRadius * targetRadius;
        for (LivingEntity candidate : this.getWorld().getEntitiesByClass(
                LivingEntity.class,
                owner.getBoundingBox().expand(targetRadius, 8.0, targetRadius),
                entity -> entity.isAlive() && (supportMode
                        ? entity != owner
                        : CombatTargeting.isOffensiveTargetCandidate(entity, owner))
        )) {
            if (supportMode) {
                if (candidate == owner || !CombatTargeting.isFriendlyTo(candidate, owner)) {
                    continue;
                }
            } else {
                if (!CombatTargeting.checkFriendlyFire(candidate, owner)) {
                    continue;
                }
            }
            double dist = candidate.squaredDistanceTo(this.getX(), this.getY(), this.getZ());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private LivingEntity resolveForcedTarget(LivingEntity owner, double targetRadius) {
        if (this.forcedTargetUuid == null || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return null;
        }
        Entity entity = serverWorld.getEntity(this.forcedTargetUuid);
        if (!(entity instanceof LivingEntity forced) || !forced.isAlive()) {
            this.forcedTargetUuid = null;
            return null;
        }
        if (!CombatTargeting.checkFriendlyFire(forced, owner)) {
            this.forcedTargetUuid = null;
            return null;
        }
        if (forced.squaredDistanceTo(this.getX(), this.getY(), this.getZ()) > targetRadius * targetRadius) {
            return null;
        }
        return forced;
    }

    private void beginDrawing(Vec3d direction, UUID targetUuid) {
        if (direction.lengthSquared() <= 1.0E-6) {
            return;
        }
        this.preparedShotDirection = direction.normalize();
        this.trackedTargetUuid = targetUuid;
        this.drawTicksRemaining = DRAW_TICKS;
        this.setPullStage(1);
    }

    private void updateAimDuringDraw(LivingEntity owner) {
        if (this.trackedTargetUuid != null && this.getWorld() instanceof ServerWorld serverWorld) {
            Entity tracked = serverWorld.getEntity(this.trackedTargetUuid);
            if (tracked instanceof LivingEntity living && living.isAlive()) {
                Vec3d trackedPos = living.getPos().add(0.0, living.getStandingEyeHeight() * 0.6, 0.0);
                this.preparedShotDirection = getCompensatedAimDirection(trackedPos, ARROW_SPEED, COMPANION_ARROW_GRAVITY_PER_TICK);
            } else {
                this.trackedTargetUuid = null;
            }
        }

        if (this.preparedShotDirection == null || this.preparedShotDirection.lengthSquared() <= 1.0E-6) {
            this.preparedShotDirection = owner.getRotationVec(1.0F).normalize();
        }

        setLookToDirection(this.preparedShotDirection);
    }

    private Vec3d getCompensatedAimDirection(Vec3d targetPos, float speed, float gravityPerTick) {
        Vec3d delta = targetPos.subtract(this.getPos());
        double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (horizontalDistance <= 1.0E-6) {
            return delta.lengthSquared() > 1.0E-6 ? delta.normalize() : Vec3d.ZERO;
        }

        double safeSpeed = Math.max(0.1, speed);
        double travelTicks = horizontalDistance / safeSpeed;
        double dropCompensation = 0.5 * gravityPerTick * travelTicks * travelTicks;
        Vec3d compensated = new Vec3d(delta.x, delta.y + dropCompensation, delta.z);
        return compensated.lengthSquared() > 1.0E-6 ? compensated.normalize() : delta.normalize();
    }

    private void firePreparedArrow(LivingEntity owner) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        BowUpgradeData upgrades = BowUpgradeData.from(owner.getMainHandStack());
        int dynamicCooldown = getDynamicCooldown(upgrades);
        long now = serverWorld.getTime();
        if (this.lastFiredTick >= 0L && now - this.lastFiredTick < dynamicCooldown) {
            this.cooldownTicks = Math.max(this.cooldownTicks, (int) (dynamicCooldown - (now - this.lastFiredTick)));
            this.preparedShotDirection = null;
            this.trackedTargetUuid = null;
            this.setPullStage(0);
            return;
        }

        boolean graceSupport = owner instanceof ServerPlayerEntity serverOwner
                && EchoShoulderBowManager.isGraceSupportActive(serverOwner);
        if (!graceSupport && isTrackedTargetFriendly(owner)) {
            this.preparedShotDirection = null;
            this.trackedTargetUuid = null;
            this.cooldownTicks = Math.max(this.cooldownTicks, 2);
            this.setPullStage(0);
            return;
        }

        Vec3d direction = this.preparedShotDirection;
        if (direction == null || direction.lengthSquared() <= 1.0E-6) {
            direction = owner.getRotationVec(1.0F);
        }

        ProjectileEntity arrow = createCompanionArrow(serverWorld, owner, direction);
        if (arrow == null) {
            this.preparedShotDirection = null;
            this.trackedTargetUuid = null;
            this.setPullStage(0);
            return;
        }
        serverWorld.spawnEntity(arrow);

        serverWorld.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundCategory.PLAYERS, 0.45F, 1.35F + serverWorld.random.nextFloat() * 0.1F);
        this.lastFiredTick = now;
        this.cooldownTicks = dynamicCooldown + (this.getSide() > 0 ? 4 : 0);
        this.preparedShotDirection = null;
        this.trackedTargetUuid = null;
        this.setPullStage(0);
    }

    private boolean isTrackedTargetFriendly(LivingEntity owner) {
        if (owner == null || this.trackedTargetUuid == null || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return false;
        }
        Entity tracked = serverWorld.getEntity(this.trackedTargetUuid);
        return tracked instanceof LivingEntity living && living.isAlive() && CombatTargeting.isFriendlyTo(living, owner);
    }

    private double getDynamicTargetRadius(BowUpgradeData upgrades) {
        double radius = TARGET_RADIUS
                - upgrades.stringLevel() * STRING_TARGET_RADIUS_DELTA
                + upgrades.frameLevel() * FRAME_TARGET_RADIUS_DELTA;
        return MathHelper.clamp(radius, MIN_TARGET_RADIUS, MAX_TARGET_RADIUS);
    }

    private int getDynamicCooldown(BowUpgradeData upgrades) {
        int cooldown = AUTO_FIRE_COOLDOWN_TICKS
                - upgrades.stringLevel() * STRING_COOLDOWN_DELTA
                + upgrades.frameLevel() * FRAME_COOLDOWN_DELTA;
        return MathHelper.clamp(cooldown, MIN_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS);
    }

    private int getDynamicScanTicks(BowUpgradeData upgrades) {
        int cooldown = getDynamicCooldown(upgrades);
        return MathHelper.clamp(cooldown / 4, 2, TARGET_SCAN_TICKS);
    }

    private ProjectileEntity createCompanionArrow(ServerWorld world, LivingEntity owner, Vec3d direction) {
        ItemStack mainHand = owner.getMainHandStack();
        ItemStack offHand = owner.getOffHandStack();
        ItemStack arrowStack = owner.getProjectileType(this.isMirroringOffhand() ? offHand : mainHand);
        if (arrowStack == null || arrowStack.isEmpty()) {
            arrowStack = new ItemStack(Items.ARROW);
        }

        float speed = ARROW_SPEED;
        float divergence = ARROW_DIVERGENCE;
        ProjectileEntity projectile;

        if (this.isMirroringOffhand() && offHand.getItem() instanceof SimplyBowItem offhandBow) {
            BowUpgradeData upgrades = BowUpgradeData.from(offHand);
            if (offhandBow instanceof VineBowItem) {
                speed = 1.65F * (float) (1.0 + upgrades.stringLevel() * 0.05);
                divergence = 1.15F;
                VineArrowEntity arrow = new VineArrowEntity(world, owner, arrowStack, offHand);
                arrow.setDamage(1.5 * upgrades.damageMultiplier());
                projectile = arrow;
            } else if (offhandBow instanceof EarthBowItem) {
                speed = 2.16F * (float) (1.0 + upgrades.stringLevel() * 0.05);
                divergence = 0.9F;
                EarthArrowEntity arrow = new EarthArrowEntity(world, owner, arrowStack, offHand);
                arrow.setDamage(2.0 * upgrades.damageMultiplier());
                projectile = arrow;
            } else if (offhandBow instanceof BubbleBowItem) {
                speed = 2.85F;
                divergence = 0.8F;
                ProjectileEntity arrow;
                if (upgrades.runeEtching() == RuneEtching.PAIN) {
                    arrow = new BubblePainArrowEntity(world, owner, arrowStack, offHand);
                } else {
                    arrow = new BubbleArrowEntity(world, owner, arrowStack, offHand);
                }
                if (arrow instanceof net.minecraft.entity.projectile.PersistentProjectileEntity persistent) {
                    persistent.setDamage(1.75);
                }
                projectile = arrow;
            } else if (offhandBow instanceof BeeBowItem) {
                speed = 1.44F;
                divergence = 0.7F;
                BeeArrowEntity arrow = new BeeArrowEntity(world, owner, arrowStack, offHand);
                arrow.setDamage(2.0);
                projectile = arrow;
            } else if (offhandBow instanceof BlossomBowItem) {
                speed = 2.34F;
                divergence = 0.85F;
                BlossomArrowEntity arrow = new BlossomArrowEntity(world, owner, arrowStack, offHand);
                arrow.setDamage(1.5);
                projectile = arrow;
            } else if (offhandBow instanceof IceBowItem) {
                speed = 3.6F;
                divergence = 1.0F;
                double damageMultiplier = upgrades.damageMultiplier();
                RuneEtching rune = upgrades.runeEtching();
                if (rune == RuneEtching.PAIN) {
                    damageMultiplier *= 1.5;
                } else if (rune == RuneEtching.BOUNTY) {
                    damageMultiplier *= 0.75;
                }

                int quantity = getOffhandIceArrowQuantity(upgrades);
                projectile = spawnOffhandIceVolley(world, owner, offHand, arrowStack, direction, damageMultiplier, rune, speed, divergence, quantity);
            } else if (offhandBow instanceof CosmicBowItem) {
                float runeSpeedMultiplier = upgrades.runeEtching() == RuneEtching.BOUNTY
                        ? SimplyBowsConfig.INSTANCE.cosmicBow.bountyArrowSpeedMultiplier.get()
                        : 1.0F;
                speed = ARROW_SPEED * SimplyBowsConfig.INSTANCE.cosmicBow.arrowSpeedMultiplier.get() * runeSpeedMultiplier;
                divergence = SimplyBowsConfig.INSTANCE.cosmicBow.arrowDivergence.get();
                CosmicArrowEntity arrow = new CosmicArrowEntity(world, owner, arrowStack, offHand);
                arrow.setDamage(SimplyBowsConfig.INSTANCE.cosmicBow.baseDamage.get());
                projectile = arrow;
            } else {
                EchoArrowEntity arrow = new EchoArrowEntity(world, owner, arrowStack, mainHand);
                arrow.setDamage(2.0);
                applyGracePotionPayload(owner, arrow);
                projectile = arrow;
                speed = ARROW_SPEED;
                divergence = ARROW_DIVERGENCE;
            }
        } else {
            EchoArrowEntity arrow = new EchoArrowEntity(world, owner, arrowStack, mainHand);
            arrow.setDamage(2.0);
            applyGracePotionPayload(owner, arrow);
            projectile = arrow;
            speed = ARROW_SPEED;
            divergence = ARROW_DIVERGENCE;
        }

        if (projectile instanceof net.minecraft.entity.projectile.PersistentProjectileEntity persistent) {
            persistent.setCritical(this.getPullStage() >= 3);
            persistent.setDamage(CombatTargeting.applyNonPlayerProjectileDamageModifier(owner, (float) persistent.getDamage()));
        }
        projectile.setPosition(this.getX(), this.getY() + 0.02, this.getZ());
        projectile.setVelocity(direction.x, direction.y, direction.z, speed, divergence);
        return projectile;
    }

    private static void applyGracePotionPayload(LivingEntity owner, EchoArrowEntity arrow) {
        if (!(owner instanceof ServerPlayerEntity player)) {
            return;
        }
        EchoShoulderBowManager.GracePotionPayload payload = EchoShoulderBowManager.consumeGracePotionShot(player);
        if (payload != null && arrow != null) {
            arrow.setGracePotionPayload(payload.effects(), payload.supportMode(), payload.splashRadius());
        }
    }

    private ProjectileEntity spawnOffhandIceVolley(
            ServerWorld world,
            LivingEntity owner,
            ItemStack offHand,
            ItemStack arrowStack,
            Vec3d direction,
            double damageMultiplier,
            RuneEtching rune,
            float speed,
            float divergence,
            int quantity
    ) {
        ProjectileEntity first = null;
        int clampedQuantity = Math.max(1, quantity);
        double yawStep = 5.0;
        double centerOffset = (clampedQuantity - 1) * 0.5;
        for (int i = 0; i < clampedQuantity; i++) {
            ProjectileEntity projectile = createOffhandIceProjectile(world, owner, offHand, arrowStack, damageMultiplier, rune);
            if (projectile == null) {
                continue;
            }
            if (first == null) {
                first = projectile;
            }

            double yawOffsetRad = Math.toRadians((i - centerOffset) * yawStep);
            Vec3d spreadDirection = direction.rotateY((float) yawOffsetRad).normalize();
            projectile.setPosition(this.getX(), this.getY() + 0.02, this.getZ());
            projectile.setVelocity(spreadDirection.x, spreadDirection.y, spreadDirection.z, speed, divergence);

            if (i == 0) {
                continue;
            }
            world.spawnEntity(projectile);
        }
        return first;
    }

    private ProjectileEntity createOffhandIceProjectile(
            ServerWorld world,
            LivingEntity owner,
            ItemStack offHand,
            ItemStack arrowStack,
            double damageMultiplier,
            RuneEtching rune
    ) {
        if (arrowStack.isOf(Items.SPECTRAL_ARROW)) {
            HomingSpectralArrowEntity arrow = new HomingSpectralArrowEntity(world, owner, arrowStack, offHand);
            arrow.setDamage(2.0 * damageMultiplier);
            arrow.setLockSingleTarget(rune == RuneEtching.PAIN);
            arrow.setStackingSlowness(rune == RuneEtching.GRACE);
            if (rune == RuneEtching.PAIN && this.trackedTargetUuid != null) {
                arrow.setLockedTargetUuid(this.trackedTargetUuid);
            }
            return arrow;
        }

        HomingArrowEntity arrow = new HomingArrowEntity(world, owner, arrowStack, offHand);
        arrow.setDamage(2.0 * damageMultiplier);
        arrow.setLockSingleTarget(rune == RuneEtching.PAIN);
        arrow.setStackingSlowness(rune == RuneEtching.GRACE);
        if (rune == RuneEtching.PAIN && this.trackedTargetUuid != null) {
            arrow.setLockedTargetUuid(this.trackedTargetUuid);
        }
        return arrow;
    }

    private static int getOffhandIceArrowQuantity(BowUpgradeData upgrades) {
        int quantity = 3 + upgrades.stringLevel();
        if (upgrades.runeEtching() == RuneEtching.BOUNTY) {
            quantity *= 2;
        }
        return quantity;
    }

    private static int stageForRemainingDrawTicks(int remainingTicks) {
        if (remainingTicks >= 6) {
            return 1;
        }
        if (remainingTicks >= 3) {
            return 2;
        }
        return 3;
    }

    private void setLookToDirection(Vec3d direction) {
        if (direction.lengthSquared() <= 1.0E-6) {
            return;
        }
        float yaw = (float) (Math.atan2(direction.x, direction.z) * (180.0F / Math.PI));
        float pitch = (float) (-(Math.atan2(direction.y, direction.horizontalLength()) * (180.0F / Math.PI)));
        this.setYaw(yaw);
        this.setPitch(pitch);
    }

    private void setLookToDirectionSmooth(Vec3d direction, float smoothing) {
        if (direction.lengthSquared() <= 1.0E-6) {
            return;
        }
        float targetYaw = (float) (Math.atan2(direction.x, direction.z) * (180.0F / Math.PI));
        float targetPitch = (float) (-(Math.atan2(direction.y, direction.horizontalLength()) * (180.0F / Math.PI)));
        float yawDelta = MathHelper.wrapDegrees(targetYaw - this.getYaw());
        float pitchDelta = targetPitch - this.getPitch();
        this.setYaw(this.getYaw() + yawDelta * smoothing);
        this.setPitch(this.getPitch() + pitchDelta * smoothing);
    }

    private void spawnIdleParticles() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld) || this.age % 3 != 0) {
            return;
        }
        serverWorld.spawnParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 0.08, this.getZ(), 2, 0.08, 0.04, 0.08, 0.0);
        if (getOwnerLiving() instanceof ServerPlayerEntity owner && EchoShoulderBowManager.hasGracePotionCharge(owner)) {
            serverWorld.spawnParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 0.1, this.getZ(), 1, 0.05, 0.03, 0.05, 0.0);
            if (this.age % 6 == 0) {
                serverWorld.spawnParticles(ParticleTypes.INSTANT_EFFECT, this.getX(), this.getY() + 0.12, this.getZ(), 2, 0.06, 0.04, 0.06, 0.0);
            }
        }
    }


    @Override
    public boolean isAttackable() {
        return false;
    }


    @Override
    public boolean isCollidable() {
        return false;
    }


    @Override
    public boolean canHit() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("owner_uuid")) {
            this.ownerUuid = nbt.getUuid("owner_uuid");
        }
        if (nbt.contains("side")) {
            this.setSide(nbt.getInt("side"));
        }
        if (nbt.contains("pull_stage")) {
            this.setPullStage(nbt.getInt("pull_stage"));
        }
        if (nbt.contains("mirror_offhand")) {
            this.dataTracker.set(MIRROR_OFFHAND, nbt.getBoolean("mirror_offhand"));
        }
        if (nbt.contains("mirror_item_id")) {
            this.dataTracker.set(MIRROR_ITEM_RAW_ID, nbt.getInt("mirror_item_id"));
        }
        this.drawTicksRemaining = nbt.getInt("draw_ticks");
        this.cooldownTicks = nbt.getInt("cooldown_ticks");
        if (nbt.contains("last_fired_tick")) {
            this.lastFiredTick = nbt.getLong("last_fired_tick");
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.putUuid("owner_uuid", this.ownerUuid);
        }
        nbt.putInt("side", this.getSide());
        nbt.putInt("pull_stage", this.getPullStage());
        nbt.putBoolean("mirror_offhand", this.dataTracker.get(MIRROR_OFFHAND));
        nbt.putInt("mirror_item_id", this.dataTracker.get(MIRROR_ITEM_RAW_ID));
        nbt.putInt("draw_ticks", this.drawTicksRemaining);
        nbt.putInt("cooldown_ticks", this.cooldownTicks);
        nbt.putLong("last_fired_tick", this.lastFiredTick);
    }
}
